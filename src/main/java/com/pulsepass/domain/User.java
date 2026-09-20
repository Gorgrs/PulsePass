package com.pulsepass.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Identidad de dominio de un asistente (FR-USR-001).
 *
 * <p>R-004 del PRD: no representa credenciales de seguridad; el MVP no
 * implementa autenticacion.</p>
 *
 * <p>La tabla se llama {@code users} porque {@code user} es palabra
 * reservada en PostgreSQL.</p>
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 60)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    /**
     * Lado inverso del 1:1 (BR-004). La FK {@code user_profiles.user_id} la
     * mantiene {@link UserProfile}; el {@code UNIQUE} sobre esa columna es lo
     * que convierte la relacion en un verdadero 1:1 dentro de PostgreSQL.
     */
    @OneToOne(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private UserProfile profile;

    @OneToMany(mappedBy = "user")
    private List<Ticket> tickets = new ArrayList<>();

    protected User() {
        // Constructor requerido por JPA.
    }

    public User(String username, String email) {
        this.username = username;
        this.email = email;
        this.active = true;
    }

    /**
     * Asocia el perfil manteniendo sincronizados ambos lados.
     *
     * <p>Si solo se asignara {@code this.profile}, el lado propietario
     * seguiria con su FK en null y el INSERT fallaria por {@code NOT NULL}.</p>
     */
    public void assignProfile(UserProfile profile) {
        this.profile = profile;
        if (profile != null) {
            profile.setUser(this);
        }
    }

    public void addTicket(Ticket ticket) {
        tickets.add(ticket);
        ticket.setUser(this);
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public UserProfile getProfile() {
        return profile;
    }

    public List<Ticket> getTickets() {
        return tickets;
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "'}";
    }
}
