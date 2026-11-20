package com.pulsepass.dto.response;

/**
 * Respuesta del login: el token, su tipo (Bearer) y los segundos de vigencia.
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {

    @Override
    public String toString() {
        // Nunca incluir el token en el texto del objeto (NFR-SEC001).
        return "AuthResponse{tokenType='" + tokenType + "', expiresIn=" + expiresIn + "}";
    }
}