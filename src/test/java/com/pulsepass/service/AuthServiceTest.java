package com.pulsepass.service;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.SecurityPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link AuthService} (sin Spring ni base de datos).
 * Las contrasenas y hashes de estas pruebas son ficticios.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String PASSWORD = "ClaveDePrueba2026";

    @Mock
    private UserService userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserCredentialRepository userCredentialRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    // ---------- ayudas ----------

    private RegisterUserRequest registerRequest() {
        return new RegisterUserRequest("andrea", "andrea@email.com",
                "Andrea", "Perez", "3001234567", "Santa Marta", LocalDate.of(2000, 1, 1), PASSWORD);
    }

    private UserResponse userResponse() {
        return new UserResponse(1L, "andrea", "andrea@email.com", true,
                "Andrea", "Perez", "3001234567", "Santa Marta", LocalDate.of(2000, 1, 1));
    }

    private Authentication authenticatedAs(long userId) {
        SecurityPrincipal principal = new SecurityPrincipal(
                userId, "andrea", "hash-ficticio", true, List.of(Role.USER));
        return UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());
    }

    // ---------- login ----------

    @Test
    @DisplayName("Login correcto: devuelve un token Bearer con el id del usuario")
    void login_validCredentials_returnsBearerToken() {
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticatedAs(42L));
        when(jwtService.generateToken(42L)).thenReturn("token-ficticio");
        when(jwtService.getExpiresInSeconds()).thenReturn(900L);

        AuthResponse response = authService.login(new LoginRequest("  andrea  ", PASSWORD));

        assertThat(response.accessToken()).isEqualTo("token-ficticio");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900L);

        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("andrea");
        assertThat(captor.getValue().getCredentials()).isEqualTo(PASSWORD);
    }

    @Test
    @DisplayName("Contrasena incorrecta: error generico y no se emite token")
    void login_wrongPassword_throwsAndNeverIssuesToken() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("detalle interno"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("andrea", "otra-clave-larga")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales invalidas");
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("Usuario inactivo: error generico y no se emite token")
    void login_inactiveUser_throwsAndNeverIssuesToken() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new DisabledException("User is disabled"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("andrea", PASSWORD)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales invalidas");
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("Usuario inexistente: el mismo error generico, sin revelar que no existe")
    void login_unknownUser_throwsTheSameGenericError() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new UsernameNotFoundException("no existe"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("nadie", PASSWORD)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales invalidas");
        verify(jwtService, never()).generateToken(any());
    }

    // ---------- registro ----------

    @Test
    @DisplayName("Registro: guarda el hash (no la contrasena) y solo el rol USER")
    void register_validRequest_savesHashedCredentialAndUserRole() {
        User user = new User("andrea", "andrea@email.com");
        when(userService.register(any(RegisterUserRequest.class))).thenReturn(userResponse());
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(PASSWORD)).thenReturn("HASH-FICTICIO");

        UserResponse result = authService.register(registerRequest());

        assertThat(result).isEqualTo(userResponse());

        ArgumentCaptor<UserCredential> credentialCaptor = ArgumentCaptor.forClass(UserCredential.class);
        verify(userCredentialRepository).save(credentialCaptor.capture());
        assertThat(credentialCaptor.getValue().getPasswordHash())
                .isEqualTo("HASH-FICTICIO")
                .isNotEqualTo(PASSWORD);

        ArgumentCaptor<UserRole> roleCaptor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(roleCaptor.capture());
        assertThat(roleCaptor.getValue().getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("Registro: si el usuario no se puede crear, no se guarda credencial ni rol")
    void register_userCreationFails_savesNoCredentialOrRole() {
        when(userService.register(any(RegisterUserRequest.class)))
                .thenThrow(new DuplicateResourceException("Username already exists: andrea"));

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(DuplicateResourceException.class);

        verify(passwordEncoder, never()).encode(any());
        verify(userCredentialRepository, never()).save(any());
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registro: si el usuario creado no se encuentra, falla y no guarda credencial")
    void register_createdUserNotFound_failsWithoutSavingCredential() {
        when(userService.register(any(RegisterUserRequest.class))).thenReturn(userResponse());
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(IllegalStateException.class);

        verify(userCredentialRepository, never()).save(any());
        verify(userRoleRepository, never()).save(any());
    }
}