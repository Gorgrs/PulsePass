package com.pulsepass.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Recinto donde ocurre un evento (FR-VEN-001).
 *
 * <p>Lado inverso de la relacion 1:N con {@link Event}: la FK vive
 * fisicamente en {@code events.venue_id}, por eso aqui se usa
 * {@code mappedBy} y el propietario de la relacion es {@link Event}
 * (BR-001, BR-002).</p>
 */
@Entity
@Table(name = "venues")
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "address", nullable = false, length = 200)
    private String address;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "venue")
    private List<Event> events = new ArrayList<>();

    protected Venue() {
        // Constructor requerido por JPA.
    }

    public Venue(String code, String name, String city, String address, Integer capacity) {
        this.code = code;
        this.name = name;
        this.city = city;
        this.address = address;
        this.capacity = capacity;
        this.active = true;
    }

    /**
     * Agrega un evento manteniendo sincronizados ambos lados de la relacion.
     *
     * <p>Sin esta sincronizacion la coleccion en memoria y la FK en base de
     * datos pueden contradecirse dentro de la misma transaccion.</p>
     */
    public void addEvent(Event event) {
        events.add(event);
        event.setVenue(this);
    }

    public void removeEvent(Event event) {
        events.remove(event);
        event.setVenue(null);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<Event> getEvents() {
        return events;
    }
}
