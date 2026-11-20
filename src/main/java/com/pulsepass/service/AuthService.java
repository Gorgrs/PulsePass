package com.pulsepass.service;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.SecurityPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro con contrasena y login (FR-SECAUTH-001, 002 y 003).
 *
 * <p>No reemplaza a {@link UserService}: las reglas de negocio del usuario
 * (unicidad, edad minima, etc.) siguen alli. Este servicio solo agrega la
 * credencial, el rol y la emision del token.</p>
 */
@Service
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";
    private static final String INVALID_CREDENTIALS = "Credenciales invalidas";

    private final UserService userService;
    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserService userService,
                       UserRepository userRepository,
                       UserCredentialRepository userCredentialRepository,
                       UserRoleRepository userRoleRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Crea el usuario, su credencial (hash BCrypt) y el rol USER en una sola
     * transaccion: si algo falla, no queda una cuenta a medias (NFR-SEC004).
     * El rol inicial es siempre USER; nunca viene del cliente (NFR-SEC009).
     */
    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        UserResponse created = userService.register(request);

        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new IllegalStateException(
                        "No se pudo vincular la credencial al usuario creado"));

        userCredentialRepository.save(
                new UserCredential(user, passwordEncoder.encode(request.password())));
        userRoleRepository.save(new UserRole(user, Role.USER));

        return created;
    }

    /**
     * Verifica usuario o correo y contrasena, y emite el JWT.
     * Cualquier fallo (usuario inexistente, clave incorrecta, usuario inactivo)
     * produce el mismo error, para no revelar si la cuenta existe.
     */
    public AuthResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.identifier().trim(), request.password()));
        } catch (AuthenticationException e) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        SecurityPrincipal principal = (SecurityPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal.getUserId());
        return new AuthResponse(token, TOKEN_TYPE, jwtService.getExpiresInSeconds());
    }
}