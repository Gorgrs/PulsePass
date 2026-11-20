package com.pulsepass.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Responde 401 cuando se llega a una ruta protegida sin identidad valida:
 * sin token, token vencido o token manipulado.
 *
 * <p>El mensaje es siempre el mismo, sin detalles internos (SEC009). Las
 * letras con tilde van como escapes unicode para no depender de la
 * codificacion del archivo.</p>
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String MESSAGE = "Autenticaci\u00f3n requerida o token inv\u00e1lido";

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        SecurityErrorWriter.write(response, HttpStatus.UNAUTHORIZED, MESSAGE);
    }
}