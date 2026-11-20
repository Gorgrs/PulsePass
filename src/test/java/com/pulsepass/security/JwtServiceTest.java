package com.pulsepass.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas unitarias de {@link JwtService} (sin Spring ni base de datos).
 * Las claves son ficticias y exclusivas de las pruebas.
 */
class JwtServiceTest {

    private static final String ISSUER = "pulsepass-api";
    private static final Duration TTL = Duration.ofMinutes(15);
    private static final String SECRET = secretOf(32, (byte) 1);
    private static final String OTHER_SECRET = secretOf(32, (byte) 2);

    private final JwtService service = new JwtService(new JwtProperties(ISSUER, SECRET, TTL));

    private static String secretOf(int length, byte fill) {
        byte[] bytes = new byte[length];
        Arrays.fill(bytes, fill);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static SecretKey keyOf(String base64) {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64));
    }

    @Test
    @DisplayName("Un token recien emitido es valido y devuelve el id del usuario")
    void validTokenReturnsUserId() {
        String token = service.generateToken(42L);

        assertThat(service.extractUserId(token)).contains(42L);
    }

    @Test
    @DisplayName("El token trae sub, iss, iat, exp y jti correctos")
    void tokenHasExpectedClaims() {
        String token = service.generateToken(42L);

        Claims claims = Jwts.parser()
                .verifyWith(keyOf(SECRET))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
                .isEqualTo(TTL.toMillis());
    }

    @Test
    @DisplayName("Dos tokens del mismo usuario tienen jti distinto")
    void eachTokenHasDifferentJti() {
        assertThat(service.generateToken(1L)).isNotEqualTo(service.generateToken(1L));
    }

    @Test
    @DisplayName("expiresIn es 900 segundos")
    void expiresInIsNineHundredSeconds() {
        assertThat(service.getExpiresInSeconds()).isEqualTo(900L);
    }

    @Test
    @DisplayName("Un token vencido se rechaza")
    void expiredTokenIsRejected() {
        Clock oneHourAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC);
        JwtService pastService = new JwtService(new JwtProperties(ISSUER, SECRET, TTL), oneHourAgo);

        String expired = pastService.generateToken(1L);

        assertThat(service.extractUserId(expired)).isEmpty();
    }

    @Test
    @DisplayName("Un token con la firma alterada se rechaza")
    void tamperedSignatureIsRejected() {
        String[] parts = service.generateToken(1L).split("\\.");
        char first = parts[2].charAt(0);
        parts[2] = (first == 'A' ? 'B' : 'A') + parts[2].substring(1);

        assertThat(service.extractUserId(String.join(".", parts))).isEmpty();
    }

    @Test
    @DisplayName("Un token con el payload de otro usuario se rechaza")
    void swappedPayloadIsRejected() {
        String[] userOne = service.generateToken(1L).split("\\.");
        String[] userTwo = service.generateToken(2L).split("\\.");
        String forged = userOne[0] + "." + userTwo[1] + "." + userOne[2];

        assertThat(service.extractUserId(forged)).isEmpty();
    }

    @Test
    @DisplayName("Un token emitido por otro issuer se rechaza")
    void wrongIssuerIsRejected() {
        JwtService other = new JwtService(new JwtProperties("otro-emisor", SECRET, TTL));

        assertThat(service.extractUserId(other.generateToken(1L))).isEmpty();
    }

    @Test
    @DisplayName("Un token firmado con otra clave se rechaza")
    void wrongKeyIsRejected() {
        JwtService other = new JwtService(new JwtProperties(ISSUER, OTHER_SECRET, TTL));

        assertThat(service.extractUserId(other.generateToken(1L))).isEmpty();
    }

    @Test
    @DisplayName("Un token sin firma (alg=none) se rechaza")
    void unsignedTokenIsRejected() {
        String unsigned = Jwts.builder()
                .subject("1")
                .issuer(ISSUER)
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .id(UUID.randomUUID().toString())
                .compact();

        assertThat(service.extractUserId(unsigned)).isEmpty();
    }

    @Test
    @DisplayName("Un token con sub no numerico se rechaza")
    void nonNumericSubjectIsRejected() {
        String token = Jwts.builder()
                .subject("abc")
                .issuer(ISSUER)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .id(UUID.randomUUID().toString())
                .signWith(keyOf(SECRET), Jwts.SIG.HS256)
                .compact();

        assertThat(service.extractUserId(token)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "no-es-un-jwt", "a.b.c"})
    @DisplayName("Un token nulo, vacio o mal formado se rechaza sin lanzar excepcion")
    void malformedTokenIsRejected(String token) {
        assertThat(service.extractUserId(token)).isEmpty();
    }

    @Test
    @DisplayName("Una clave de menos de 32 bytes impide crear la configuracion")
    void weakKeyIsRejected() {
        String weak = secretOf(16, (byte) 1);

        assertThatThrownBy(() -> new JwtProperties(ISSUER, weak, TTL))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Una clave ausente o que no es Base64 impide crear la configuracion")
    void missingOrInvalidKeyIsRejected() {
        assertThatThrownBy(() -> new JwtProperties(ISSUER, null, TTL))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtProperties(ISSUER, "esto no es base64!!", TTL))
                .isInstanceOf(IllegalStateException.class);
    }
}