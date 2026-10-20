package com.pulsepass.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Base64;

/**
 * Configuracion del JWT, leida de {@code pulsepass.security.jwt.*}.
 *
 * <p>Valida al arrancar: si la clave falta, no es Base64 o tiene menos de
 * 32 bytes, la aplicacion no inicia (SEC004, SEC005).</p>
 *
 * @param issuer         emisor que se escribe en cada token y se exige al validar
 * @param secretBase64   clave HMAC en Base64, leida de la variable de entorno
 * @param accessTokenTtl duracion del token de acceso
 */
@ConfigurationProperties(prefix = "pulsepass.security.jwt")
public record JwtProperties(String issuer, String secretBase64, Duration accessTokenTtl) {

    private static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("El emisor del JWT (issuer) es obligatorio");
        }
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalStateException(
                    "Falta la clave JWT: defina la variable de entorno PULSEPASS_JWT_SECRET_BASE64");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(secretBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("La clave JWT no es Base64 valido");
        }
        if (decoded.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "La clave JWT debe tener al menos " + MIN_SECRET_BYTES + " bytes");
        }
        if (accessTokenTtl == null || accessTokenTtl.isZero() || accessTokenTtl.isNegative()) {
            throw new IllegalStateException("La duracion del token debe ser positiva");
        }
    }

    @Override
    public String toString() {
        // Nunca mostrar la clave (NFR-SEC001).
        return "JwtProperties{issuer='" + issuer + "', accessTokenTtl=" + accessTokenTtl + "}";
    }
}