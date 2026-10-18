package com.pulsepass.dto.response;

import java.util.List;

/**
 * Informacion minima del usuario autenticado (GET /api/auth/me).
 * No incluye contrasena, hash ni correo.
 */
public record AuthenticatedUserResponse(
        Long userId,
        String username,
        List<String> roles
) {
}