package com.pulsepass.repository;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a {@link Event}. Es el repositorio central del taller: concentra
 * Query Methods simples, Query Methods que navegan relaciones y consultas
 * JPQL con varias asociaciones.
 */
public interface EventRepository extends JpaRepository<Event, Long> {

    // -----------------------------------------------------------------
    // Query Methods sobre atributos propios
    // -----------------------------------------------------------------

    /** FR-EVT-002 / AC-002: eventCode es UNIQUE. */
    Optional<Event> findByEventCode(String eventCode);

    /** FR-EVT-005 / UC-06 / AC-006: cartelera publicada en orden cronologico. */
    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);

    /** Cartelera de una categoria concreta. */
    List<Event> findByCategoryAndStatusOrderByEventDateAsc(EventCategory category, EventStatus status);

    /** Eventos publicados que todavia no han ocurrido. */
    List<Event> findByStatusAndEventDateAfterOrderByEventDateAsc(EventStatus status, LocalDateTime from);

    /** Busqueda parcial por nombre del evento. */
    List<Event> findByNameContainingIgnoreCaseOrderByEventDateAsc(String fragment);

    // -----------------------------------------------------------------
    // Query Methods que navegan una relacion
    // -----------------------------------------------------------------

    /**
     * FR-VEN-004: eventos de un venue identificado por su codigo de negocio.
     *
     * <p>El nombre del metodo recorre {@code Event -> venue -> code}. Spring
     * Data no encuentra una propiedad {@code venueCode} en Event, asi que
     * parte el nombre y resuelve {@code venue.code}. No se usa el ID porque
     * el codigo es el identificador que maneja el negocio.</p>
     */
    List<Event> findByVenueCodeOrderByEventDateAsc(String venueCode);

    /** Eventos de una ciudad: {@code Event -> venue -> city}. */
    List<Event> findByVenueCityIgnoreCaseOrderByEventDateAsc(String city);

    /** Eventos publicados de una ciudad, en orden cronologico. */
    List<Event> findByVenueCityIgnoreCaseAndStatusOrderByEventDateAsc(String city, EventStatus status);

    // -----------------------------------------------------------------
    // @Query + JPQL
    // -----------------------------------------------------------------

    /**
     * FR-ART-004 / FR-SRC-001 / UC-07 / AC-007: eventos en los que participa
     * un artista.
     *
     * <p>Atraviesa la relacion N:M. El {@code distinct} es necesario porque el
     * JOIN contra {@code event_artists} produce una fila por cada coincidencia
     * y un evento podria repetirse en el resultado.</p>
     */
    @Query("""
           select distinct e
           from Event e
           join e.artists a
           where lower(a.stageName) = lower(:stageName)
           order by e.eventDate asc
           """)
    List<Event> findEventsByArtistStageName(@Param("stageName") String stageName);

    /**
     * FR-SRC-002: eventos de una ciudad en los que participa un artista.
     *
     * <p>Combina dos asociaciones distintas en la misma consulta
     * ({@code Event -> venue} y {@code Event -> artists}), que es justamente
     * el caso donde un Query Method dejaria de ser legible.</p>
     */
    @Query("""
           select distinct e
           from Event e
           join e.venue v
           join e.artists a
           where lower(v.city) = lower(:city)
             and lower(a.stageName) = lower(:stageName)
           order by e.eventDate asc
           """)
    List<Event> findEventsByCityAndArtist(@Param("city") String city,
                                          @Param("stageName") String stageName);

    /**
     * FR-SRC-003 / UC-09: eventos recomendados.
     *
     * <p>Eventos publicados posteriores a una fecha, en una ciudad, cuyo
     * artista contenga un texto. La comparacion del artista es
     * case-insensitive y parcial; el resultado va sin duplicados y ordenado
     * por fecha.</p>
     */
    @Query("""
           select distinct e
           from Event e
           join e.venue v
           join e.artists a
           where e.status = :status
             and e.eventDate > :fromDate
             and lower(v.city) = lower(:city)
             and lower(a.stageName) like lower(concat('%', :artistFragment, '%'))
           order by e.eventDate asc
           """)
    List<Event> findRecommendedEvents(@Param("status") EventStatus status,
                                      @Param("fromDate") LocalDateTime fromDate,
                                      @Param("city") String city,
                                      @Param("artistFragment") String artistFragment);

    /**
     * Evento con su venue y sus artistas ya cargados en una sola consulta.
     *
     * <p>{@code join fetch} evita el problema N+1 cuando hay que leer el
     * evento y su reparto fuera de la transaccion: sin el, cada acceso a
     * {@code getArtists()} dispararia un SELECT adicional.</p>
     */
    @Query("""
           select distinct e
           from Event e
           join fetch e.venue
           left join fetch e.artists
           where e.eventCode = :eventCode
           """)
    Optional<Event> findByEventCodeWithArtists(@Param("eventCode") String eventCode);
}
