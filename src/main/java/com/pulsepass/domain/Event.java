package com.pulsepass.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Evento de la cartelera (FR-EVT-001).
 *
 * <p>Concentra tres relaciones:</p>
 * <ul>
 *   <li>N:1 hacia {@link Venue} - propietaria, la FK es {@code events.venue_id}.</li>
 *   <li>N:M hacia {@link Artist} - propietaria, declara {@code event_artists}.</li>
 *   <li>1:N hacia {@link Ticket} - lado inverso, la FK vive en {@code tickets.event_id}.</li>
 * </ul>
 */
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_code", nullable = false, unique = true, length = 50)
    private String eventCode;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private EventCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private EventStatus status;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "minimum_age", nullable = false)
    private Integer minimumAge = 0;

    /** Agregada por la migracion V3 (FR-EVT-006); puede ser null. */
    @Column(name = "streaming_url", length = 500)
    private String streamingUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    /**
     * Lado propietario de la relacion N:M. La PK compuesta de la tabla
     * asociativa es la que impide duplicar el par evento-artista (AC-003);
     * el {@link Set} evita ademas que el duplicado llegue siquiera al INSERT.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "event_artists",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "artist_id")
    )
    private Set<Artist> artists = new HashSet<>();

    @OneToMany(mappedBy = "event")
    private List<Ticket> tickets = new ArrayList<>();

    protected Event() {
        // Constructor requerido por JPA.
    }

    public Event(String eventCode,
                 String name,
                 EventCategory category,
                 EventStatus status,
                 LocalDateTime eventDate,
                 Integer minimumAge) {
        this.eventCode = eventCode;
        this.name = name;
        this.category = category;
        this.status = status;
        this.eventDate = eventDate;
        this.minimumAge = minimumAge;
    }

    /** Asocia un artista manteniendo sincronizados ambos lados (FR-ART-003). */
    public void addArtist(Artist artist) {
        artists.add(artist);
        artist.getEvents().add(this);
    }

    public void removeArtist(Artist artist) {
        artists.remove(artist);
        artist.getEvents().remove(this);
    }

    /** Emite una entrada para este evento manteniendo sincronizados ambos lados. */
    public void addTicket(Ticket ticket) {
        tickets.add(ticket);
        ticket.setEvent(this);
    }

    public Long getId() {
        return id;
    }

    public String getEventCode() {
        return eventCode;
    }

    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public EventCategory getCategory() {
        return category;
    }

    public void setCategory(EventCategory category) {
        this.category = category;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public Integer getMinimumAge() {
        return minimumAge;
    }

    public void setMinimumAge(Integer minimumAge) {
        this.minimumAge = minimumAge;
    }

    public String getStreamingUrl() {
        return streamingUrl;
    }

    public void setStreamingUrl(String streamingUrl) {
        this.streamingUrl = streamingUrl;
    }

    public Venue getVenue() {
        return venue;
    }

    public void setVenue(Venue venue) {
        this.venue = venue;
    }

    public Set<Artist> getArtists() {
        return artists;
    }

    public List<Ticket> getTickets() {
        return tickets;
    }

    /** Igualdad por clave de negocio: {@code eventCode} es UNIQUE. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Event event)) {
            return false;
        }
        return eventCode != null && eventCode.equals(event.eventCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventCode);
    }

    @Override
    public String toString() {
        return "Event{id=" + id + ", eventCode='" + eventCode + "', status=" + status + "}";
    }
}
