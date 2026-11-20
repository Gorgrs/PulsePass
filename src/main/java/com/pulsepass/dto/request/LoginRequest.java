package com.pulsepass.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Datos de inicio de sesion. El identificador puede ser el username o el correo.
 */
public record LoginRequest(
        @NotBlank(message = "Identifier is required")
        String identifier,

        @NotBlank(message = "Password is required")
        String password
) {

    @Override
    public String toString() {
        // Nunca incluir la contrasena en el texto del objeto (NFR-SEC001).
        return "LoginRequest{identifier='" + identifier + "'}";
    }
}