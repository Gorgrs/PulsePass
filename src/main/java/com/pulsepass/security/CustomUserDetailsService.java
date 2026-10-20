package com.pulsepass.security;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Carga la identidad de un usuario para Spring Security.
 *
 * <p>Solo lee identidad, hash, estado y roles. No valida reglas comerciales.
 * El mensaje de error es el mismo si el usuario no existe o no tiene
 * credencial, para no revelar si una cuenta existe (FR-SECAUTH-003).</p>
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final String INVALID_CREDENTIALS = "Credenciales invalidas";

    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final UserRoleRepository userRoleRepository;

    public CustomUserDetailsService(UserRepository userRepository,
                                    UserCredentialRepository userCredentialRepository,
                                    UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.userRoleRepository = userRoleRepository;
    }

    /** Para el login: el identificador puede ser el username o el correo. */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmailIgnoreCase(identifier))
                .orElseThrow(() -> new UsernameNotFoundException(INVALID_CREDENTIALS));
        return toPrincipal(user);
    }

    /** Para el filtro JWT: el token trae el id del usuario en el claim sub. */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException(INVALID_CREDENTIALS));
        return toPrincipal(user);
    }

    private SecurityPrincipal toPrincipal(User user) {
        UserCredential credential = userCredentialRepository.findById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException(INVALID_CREDENTIALS));
        List<Role> roles = userRoleRepository.findByIdUserId(user.getId()).stream()
                .map(UserRole::getRole)
                .toList();
        return new SecurityPrincipal(
                user.getId(),
                user.getUsername(),
                credential.getPasswordHash(),
                user.isActive(),
                roles
        );
    }
}