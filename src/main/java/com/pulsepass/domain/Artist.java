package com.pulsepass.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Artista que puede participar en varios eventos (FR-ART-001, BR-003).
 *
 * <p>Lado inverso de la relacion N:M: la tabla asociativa
 * {@code event_artists} la declara {@link Event} mediante {@code @JoinTable},
 * por eso aqui se usa {@code mappedBy = "artists"}.</p>
 */
@Entity
@Table(name = "artists")
public class Artist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stage_name", nullable = false, unique = true, length = 120)
    private String stageName;

    @Column(name = "country", length = 80)
    private String country;

    @Column(name = "genre", length = 80)
    private String genre;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @ManyToMany(mappedBy = "artists")
    private Set<Event> events = new HashSet<>();

    protected Artist() {
        // Constructor requerido por JPA.
    }

    public Artist(String stageName, String country, String genre) {
        this.stageName = stageName;
        this.country = country;
        this.genre = genre;
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<Event> getEvents() {
        return events;
    }

    /**
     * Igualdad por clave de negocio ({@code stageName}, que es UNIQUE) y no
     * por {@code id}: una entidad recien creada todavia no tiene id, y esta
     * clase vive dentro de un {@link Set}.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Artist artist)) {
            return false;
        }
        return stageName != null && stageName.equals(artist.stageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stageName);
    }

    @Override
    public String toString() {
        return "Artist{id=" + id + ", stageName='" + stageName + "'}";
    }
}
