package com.pulsepass.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Responde 403 cuando el usuario esta autenticado pero no tiene permiso para
 * la operacion (por ejemplo, un USER que intenta publicar un evento).
 *
 * <p>Las letras con tilde van como escapes unicode para no depender de la
 * codificacion del archivo.</p>
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final String MESSAGE = "No tiene permisos para realizar esta operaci\u00f3n";

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        SecurityErrorWriter.write(response, HttpStatus.FORBIDDEN, MESSAGE);
    }
}