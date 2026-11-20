package com.pulsepass.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Filtro que corre una vez por solicitud y lee el token del encabezado
 * {@code Authorization: Bearer <token>} (FR-SECAUTH-007).
 *
 * <p>Solo autentica si el token es valido y el usuario sigue activo en la
 * base de datos. En cualquier otro caso deja pasar la solicitud SIN
 * autenticar: la cadena de seguridad decide despues si la ruta es publica
 * (sigue) o protegida (responde 401). Este filtro nunca responde por si
 * mismo ni lanza error por un token invalido.</p>
 *
 * <p>No es un bean de Spring a proposito: {@code SecurityConfig} lo crea con
 * {@code new}, para que corra una sola vez, dentro de la cadena de seguridad.</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // No se sobrescribe una autenticacion que ya esta establecida.
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            extractToken(request)
                    .flatMap(jwtService::extractUserId)
                    .ifPresent(userId -> authenticate(userId, request));
        }
        filterChain.doFilter(request, response);
    }

    /** Devuelve el token solo si hay exactamente un encabezado Authorization con Bearer. */
    private Optional<String> extractToken(HttpServletRequest request) {
        List<String> headers = Collections.list(request.getHeaders(HttpHeaders.AUTHORIZATION));
        if (headers.size() != 1) {
            // Sin encabezado, o varios (sospechoso): no se autentica.
            return Optional.empty();
        }
        String header = headers.get(0);
        if (header == null
                || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return Optional.empty();
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }

    private void authenticate(Long userId, HttpServletRequest request) {
        try {
            // SEC011: se vuelve a consultar el usuario, su estado y sus roles actuales.
            UserDetails principal = userDetailsService.loadUserById(userId);
            if (!principal.isEnabled()) {
                return;
            }
            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(
                            principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (UsernameNotFoundException e) {
            // Usuario borrado o sin credencial: no se autentica.
        }
    }
}