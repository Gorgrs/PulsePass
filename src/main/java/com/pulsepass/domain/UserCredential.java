package com.pulsepass.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Credencial de acceso de un usuario (Fase 4, seguridad).
 *
 * <p>Vive aparte de {@link User} para que el hash de la contrasena nunca
 * viaje por accidente en un DTO. Comparte la clave primaria con el usuario
 * ({@code user_id}), por eso es un 1:1 verdadero (migracion V4).</p>
 *
 * <p>Solo guarda el hash BCrypt, jamas la contrasena en texto plano.</p>
 */
@Entity
@Table(name = "user_credentials")
public class UserCredential {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    protected UserCredential() {
        // Constructor requerido por JPA.
    }

    public UserCredential(User user, String passwordHash) {
        this.user = user;
        this.passwordHash = passwordHash;
        this.passwordChangedAt = Instant.now();
    }

    public Long getUserId() {
        return userId;
    }

    public User getUser() {
        return user;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }

    @Override
    public String toString() {
        // Nunca incluir el hash en el texto de la entidad (NFR-SEC001).
        return "UserCredential{userId=" + userId + "}";
    }
}