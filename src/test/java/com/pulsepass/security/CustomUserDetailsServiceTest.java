package com.pulsepass.security;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link CustomUserDetailsService} (sin Spring ni base de
 * datos). Los hashes de estas pruebas son ficticios.
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    private static final long USER_ID = 42L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserCredentialRepository userCredentialRepository;
    @Mock
    private UserRoleRepository userRoleRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    // ---------- ayudas ----------

    /** El id lo genera la base de datos; en la prueba se asigna a mano. */
    private User userWithId() {
        User user = new User("andrea", "andrea@email.com");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    private static List<String> authorityNames(UserDetails details) {
        return details.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }

    // ---------- loadUserByUsername ----------

    @Test
    @DisplayName("Usuario existente: devuelve id, hash, estado y roles con prefijo ROLE_")
    void existingUser_returnsPrincipalWithRoles() {
        User user = userWithId();
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(user));
        when(userCredentialRepository.findById(USER_ID))
                .thenReturn(Optional.of(new UserCredential(user, "HASH-FICTICIO")));
        when(userRoleRepository.findByIdUserId(USER_ID))
                .thenReturn(List.of(new UserRole(user, Role.USER), new UserRole(user, Role.STAFF)));

        UserDetails details = service.loadUserByUsername("andrea");

        assertThat(details).isInstanceOf(SecurityPrincipal.class);
        assertThat(((SecurityPrincipal) details).getUserId()).isEqualTo(USER_ID);
        assertThat(details.getUsername()).isEqualTo("andrea");
        assertThat(details.getPassword()).isEqualTo("HASH-FICTICIO");
        assertThat(details.isEnabled()).isTrue();
        assertThat(authorityNames(details)).containsExactlyInAnyOrder("ROLE_USER", "ROLE_STAFF");
    }

    @Test
    @DisplayName("El identificador puede ser el correo")
    void identifierCanBeTheEmail() {
        User user = userWithId();
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(user));
        when(userCredentialRepository.findById(USER_ID))
                .thenReturn(Optional.of(new UserCredential(user, "HASH-FICTICIO")));
        when(userRoleRepository.findByIdUserId(USER_ID)).thenReturn(List.of(new UserRole(user, Role.USER)));

        UserDetails details = service.loadUserByUsername("andrea@email.com");

        assertThat(details.getUsername()).isEqualTo("andrea");
    }

    @Test
    @DisplayName("Usuario inexistente: error generico")
    void unknownUser_throwsGenericError() {
        assertThatThrownBy(() -> service.loadUserByUsername("nadie"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Credenciales invalidas");
    }

    @Test
    @DisplayName("Usuario sin credencial (legado): no puede entrar y el error es el mismo")
    void userWithoutCredential_throwsGenericError() {
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(userWithId()));
        when(userCredentialRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("andrea"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Credenciales invalidas");
        verify(userRoleRepository, never()).findByIdUserId(USER_ID);
    }

    @Test
    @DisplayName("Usuario inactivo: el principal queda deshabilitado")
    void inactiveUser_principalIsDisabled() {
        User user = userWithId();
        user.setActive(false);
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(user));
        when(userCredentialRepository.findById(USER_ID))
                .thenReturn(Optional.of(new UserCredential(user, "HASH-FICTICIO")));
        when(userRoleRepository.findByIdUserId(USER_ID)).thenReturn(List.of(new UserRole(user, Role.USER)));

        UserDetails details = service.loadUserByUsername("andrea");

        assertThat(details.isEnabled()).isFalse();
    }

    // ---------- loadUserById (lo usa el filtro JWT) ----------

    @Test
    @DisplayName("Por id: devuelve el principal del usuario")
    void loadUserById_returnsPrincipal() {
        User user = userWithId();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userCredentialRepository.findById(USER_ID))
                .thenReturn(Optional.of(new UserCredential(user, "HASH-FICTICIO")));
        when(userRoleRepository.findByIdUserId(USER_ID)).thenReturn(List.of(new UserRole(user, Role.ADMIN)));

        UserDetails details = service.loadUserById(USER_ID);

        assertThat(((SecurityPrincipal) details).getUserId()).isEqualTo(USER_ID);
        assertThat(authorityNames(details)).containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Por id: un id que no existe da el error generico")
    void loadUserById_unknownId_throwsGenericError() {
        assertThatThrownBy(() -> service.loadUserById(999L))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Credenciales invalidas");
    }

    @Test
    @DisplayName("SEC011: los roles se leen de nuevo en cada carga, no quedan guardados")
    void rolesAreReadAgainOnEveryLoad() {
        User user = userWithId();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userCredentialRepository.findById(USER_ID))
                .thenReturn(Optional.of(new UserCredential(user, "HASH-FICTICIO")));
        when(userRoleRepository.findByIdUserId(USER_ID)).thenReturn(
                List.of(new UserRole(user, Role.USER), new UserRole(user, Role.ADMIN)),
                List.of(new UserRole(user, Role.USER)));

        UserDetails first = service.loadUserById(USER_ID);
        UserDetails second = service.loadUserById(USER_ID);

        assertThat(authorityNames(first)).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        assertThat(authorityNames(second)).containsExactly("ROLE_USER");
    }
}