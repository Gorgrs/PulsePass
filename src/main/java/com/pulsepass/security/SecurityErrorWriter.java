package com.pulsepass.security;

import com.pulsepass.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Escribe en la respuesta HTTP un error con el mismo formato JSON de
 * {@link ErrorResponse} (timestamp, status, error, message, details).
 *
 * <p>Los errores de seguridad ocurren en el filtro, antes de llegar a los
 * controladores, asi que {@code GlobalExceptionHandler} no los ve. Por eso
 * se escriben aqui, una sola vez por respuesta (SEC009).</p>
 */
final class SecurityErrorWriter {

    private SecurityErrorWriter() {
    }

    static void write(HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        if (response.isCommitted()) {
            return;
        }
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, Map.of());

        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(toJson(body));
    }

    /** Los mensajes son textos fijos del programa, solo se escapan comillas y barras. */
    private static String toJson(ErrorResponse body) {
        return "{"
                + "\"timestamp\":\"" + body.timestamp() + "\","
                + "\"status\":" + body.status() + ","
                + "\"error\":\"" + escape(body.error()) + "\","
                + "\"message\":\"" + escape(body.message()) + "\","
                + "\"details\":{}"
                + "}";
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}