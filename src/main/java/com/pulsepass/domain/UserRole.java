package com.pulsepass.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

/**
 * Un rol asignado a un usuario (Fase 4, seguridad).
 *
 * <p>La clave primaria es compuesta, {@code (user_id, role)}, igual que en la
 * tabla {@code user_roles} de la migracion V4: un usuario puede tener varios
 * roles, pero no el mismo rol dos veces.</p>
 */
@Entity
@Table(name = "user_roles")
public class UserRole {

    @EmbeddedId
    private UserRoleId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    protected UserRole() {
        // Constructor requerido por JPA.
    }

    public UserRole(User user, Role role) {
        this.user = user;
        this.id = new UserRoleId(user.getId(), role);
    }

    public UserRoleId getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Role getRole() {
        return id.getRole();
    }

    @Override
    public String toString() {
        return "UserRole{userId=" + id.getUserId() + ", role=" + id.getRole() + "}";
    }

    /**
     * Clave compuesta de {@link UserRole}: usuario + rol.
     */
    @Embeddable
    public static class UserRoleId implements Serializable {

        @Column(name = "user_id")
        private Long userId;

        @Enumerated(EnumType.STRING)
        @Column(name = "role", nullable = false, length = 20)
        private Role role;

        protected UserRoleId() {
            // Constructor requerido por JPA.
        }

        public UserRoleId(Long userId, Role role) {
            this.userId = userId;
            this.role = role;
        }

        public Long getUserId() {
            return userId;
        }

        public Role getRole() {
            return role;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof UserRoleId other)) {
                return false;
            }
            return Objects.equals(userId, other.userId) && role == other.role;
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, role);
        }
    }
}