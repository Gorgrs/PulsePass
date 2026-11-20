package com.pulsepass.security;

import com.pulsepass.domain.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Identidad comprobada de quien hace la solicitud (el "principal").
 *
 * <p>Se construye siempre a partir de lo que hay en la base de datos: id
 * persistente, hash de la contrasena, estado activo y roles actuales. Nunca
 * con datos que mande el cliente (SEC006).</p>
 *
 * <p>Los roles se convierten en authorities con el prefijo {@code ROLE_},
 * que es lo que espera {@code hasRole("ADMIN")}.</p>
 */
public class SecurityPrincipal implements UserDetails {

    private final Long userId;
    private final String username;
    private final String passwordHash;
    private final boolean active;
    private final List<SimpleGrantedAuthority> authorities;

    public SecurityPrincipal(Long userId, String username, String passwordHash,
                             boolean active, Collection<Role> roles) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.active = active;
        this.authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    /** SEC011: un usuario desactivado pierde el acceso. */
    @Override
    public boolean isEnabled() {
        return active;
    }

    @Override
    public String toString() {
        // Nunca incluir el hash en el texto del principal (NFR-SEC001).
        return "SecurityPrincipal{userId=" + userId + ", username='" + username + "'}";
    }
}