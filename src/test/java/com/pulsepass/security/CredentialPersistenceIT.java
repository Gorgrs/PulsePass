package com.pulsepass.security;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.service.AuthService;
import com.pulsepass.support.AbstractPersistenceIT;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integracion de credenciales y roles contra PostgreSQL real (seccion 17.3 del
 * PRD): migracion V4, hash BCrypt, unicidad de roles, registro y login con un
 * usuario real. Cada prueba se revierte al terminar. Las contrasenas son
 * ficticias.
 */
class CredentialPersistenceIT extends AbstractPersistenceIT {

    private static final String PASSWORD = "ClaveDePrueba2026";

    @Autowired
    private AuthService authService;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserCredentialRepository userCredentialRepository;
    @Autowired
    private UserRoleRepository userRoleRepository;
    @Autowired
    private EntityManager entityManager;

    // ---------- ayudas ----------

    private RegisterUserRequest request(String username, String email) {
        return new RegisterUserRequest(username, email, "Andrea", "Perez",
                "3001234567", "Santa Marta", LocalDate.of(2000, 1, 1), PASSWORD);
    }

    private User registerAndrea() {
        authService.register(request("andrea", "andrea@pulsepass.test"));
        return userRepository.findByUsername("andrea").orElseThrow();
    }

    // ---------- migracion ----------

    @Test
    @DisplayName("La migracion V4 creo las tablas user_credentials y user_roles")
    void migrationCreatedTheSecurityTables() {
        Number tables = (Number) entityManager.createNativeQuery("""
                select count(*) from information_schema.tables
                where table_name in ('user_credentials', 'user_roles')
                """).getSingleResult();

        assertThat(tables.intValue()).isEqualTo(2);
    }

    // ---------- hash y roles ----------

    @Test
    @DisplayName("El registro guarda un hash BCrypt y nunca la contrasena")
    void registrationStoresABcryptHash() {
        User user = registerAndrea();

        UserCredential credential = userCredentialRepository.findById(user.getId()).orElseThrow();

        assertThat(credential.getPasswordHash()).isNotEqualTo(PASSWORD).startsWith("$2");
        assertThat(passwordEncoder.matches(PASSWORD, credential.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("otra-clave-larga-99", credential.getPasswordHash())).isFalse();
    }

    @Test
    @DisplayName("El registro publico crea solo el rol USER")
    void registrationCreatesOnlyTheUserRole() {
        User user = registerAndrea();

        List<UserRole> roles = userRoleRepository.findByIdUserId(user.getId());

        assertThat(roles).hasSize(1);
        assertThat(roles.get(0).getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("La base de datos rechaza el mismo rol repetido para un usuario")
    void databaseRejectsADuplicateRole() {
        User user = registerAndrea();
        entityManager.flush();

        assertThatThrownBy(() -> entityManager
                .createNativeQuery("insert into user_roles (user_id, role) values (:id, 'USER')")
                .setParameter("id", user.getId())
                .executeUpdate())
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("La base de datos rechaza un rol que no existe")
    void databaseRejectsAnUnknownRole() {
        User user = registerAndrea();
        entityManager.flush();

        assertThatThrownBy(() -> entityManager
                .createNativeQuery("insert into user_roles (user_id, role) values (:id, 'SUPERADMIN')")
                .setParameter("id", user.getId())
                .executeUpdate())
                .isInstanceOf(PersistenceException.class);
    }

    // ---------- registro fallido ----------

    @Test
    @DisplayName("Un registro rechazado no deja credenciales ni roles de mas")
    void rejectedRegistrationLeavesNoExtraCredentialOrRole() {
        registerAndrea();
        long credentials = userCredentialRepository.count();
        long roles = userRoleRepository.count();

        assertThatThrownBy(() -> authService.register(request("andrea", "otro@pulsepass.test")))
                .isInstanceOf(DuplicateResourceException.class);

        assertThat(userCredentialRepository.count()).isEqualTo(credentials);
        assertThat(userRoleRepository.count()).isEqualTo(roles);
    }

    // ---------- login con un usuario real ----------

    @Test
    @DisplayName("Login con un usuario real: el token trae el id persistente del usuario")
    void loginWithARealUserReturnsATokenForThatUser() {
        User user = registerAndrea();

        AuthResponse response = authService.login(new LoginRequest("andrea", PASSWORD));

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(jwtService.extractUserId(response.accessToken())).contains(user.getId());
    }

    @Test
    @DisplayName("Login con contrasena incorrecta falla con el error generico")
    void loginWithAWrongPasswordFails() {
        registerAndrea();

        assertThatThrownBy(() -> authService.login(new LoginRequest("andrea", "otra-clave-larga-99")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales invalidas");
    }

    @Test
    @DisplayName("Un usuario legado, sin credencial, no puede iniciar sesion")
    void legacyUserWithoutCredentialCannotLogIn() {
        userRepository.save(new User("legado", "legado@pulsepass.test"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("legado", PASSWORD)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales invalidas");
    }
}