package com.pulsepass.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Crea y valida los JWT de acceso (FR-SECAUTH-005 y FR-SECAUTH-006).
 *
 * <p>Solo se ocupa del token: firma, emisor, fechas y sujeto. No consulta
 * tickets ni ejecuta reglas de negocio.</p>
 */
@Service
@EnableConfigurationProperties(JwtProperties.class)
public class JwtService {

    private static final String EXPECTED_ALGORITHM = "HS256";

    private final JwtProperties properties;
    private final SecretKey key;
    private final Clock clock;

    @Autowired
    public JwtService(JwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    /** Permite a los tests emitir tokens con otra hora (por ejemplo, ya vencidos). */
    JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secretBase64().trim()));
    }

    /** Emite un token firmado cuyo sujeto (sub) es el id persistente del usuario. */
    public String generateToken(Long userId) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(userId.toString())
                .issuer(properties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTokenTtl())))
                .id(UUID.randomUUID().toString())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** Segundos de vigencia del token, para el campo expiresIn de la respuesta. */
    public long getExpiresInSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }

    /**
     * Valida el token y devuelve el id del usuario.
     *
     * <p>Devuelve vacio si el token esta vencido, manipulado, mal formado,
     * lo emitio otro emisor o usa un algoritmo inesperado. Nunca lanza
     * excepcion por un token invalido.</p>
     */
    public Optional<Long> extractUserId(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token);

            if (!EXPECTED_ALGORITHM.equals(jws.getHeader().getAlgorithm())) {
                return Optional.empty();
            }
            Claims claims = jws.getPayload();
            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()
                    || claims.getExpiration() == null
                    || claims.getId() == null) {
                return Optional.empty();
            }
            return Optional.of(Long.valueOf(subject));
        } catch (JwtException | IllegalArgumentException e) {
            // No se registra el token ni el motivo detallado (SEC009).
            return Optional.empty();
        }
    }
}