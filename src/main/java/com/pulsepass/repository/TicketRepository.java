package com.pulsepass.repository;

import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a {@link Ticket}.
 */
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // -----------------------------------------------------------------
    // Query Methods
    // -----------------------------------------------------------------

    /** FR-TKT-002 / AC-005: ticketCode es UNIQUE. */
    Optional<Ticket> findByTicketCode(String ticketCode);

    /**
     * FR-TKT-006: entradas de un usuario identificado por su email.
     *
     * <p>Navega {@code Ticket -> user -> email}.</p>
     */
    List<Ticket> findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(String email);

    /** FR-TKT-006: la misma consulta, acotada ademas por estado. */
    List<Ticket> findByUserEmailIgnoreCaseAndStatusOrderByPurchaseDateDesc(String email, TicketStatus status);

    /**
     * FR-TKT-007: entradas de un evento en un estado determinado.
     *
     * <p>Se resuelve con Query Method y no con JPQL: son dos condiciones de
     * igualdad sobre un solo camino de navegacion
     * ({@code Ticket -> event -> eventCode}), y el nombre del metodo todavia
     * se lee sin esfuerzo. La regla del PRD (NFR-007) es reservar JPQL para
     * las consultas que dejan de ser legibles como nombre de metodo.</p>
     */
    List<Ticket> findByEventEventCodeAndStatusOrderByPurchaseDateAsc(String eventCode, TicketStatus status);

    /** Entradas de un evento por tipo y estado. */
    List<Ticket> findByEventEventCodeAndTypeAndStatus(String eventCode, TicketType type, TicketStatus status);

    // -----------------------------------------------------------------
    // @Query + JPQL
    // -----------------------------------------------------------------

    /**
     * FR-TKT-008 / UC-08 / AC-008: numero de entradas en un estado dado para
     * un evento.
     *
     * <p>Usa {@code count} en la base de datos en lugar de traer la lista
     * completa y contarla en Java: solo viaja un numero.</p>
     */
    @Query("""
           select count(t)
           from Ticket t
           where t.event.eventCode = :eventCode
             and t.status = :status
           """)
    long countTicketsByEventCodeAndStatus(@Param("eventCode") String eventCode,
                                          @Param("status") TicketStatus status);

    /**
     * FR-SRC-004: entradas cuyo evento todavia no ha ocurrido, en orden
     * cronologico por fecha del evento.
     *
     * <p>El orden es por un atributo de la entidad asociada, no de Ticket;
     * por eso se escribe en JPQL con un JOIN explicito.</p>
     */
    @Query("""
           select t
           from Ticket t
           join t.event e
           where e.eventDate > :fromDate
           order by e.eventDate asc
           """)
    List<Ticket> findTicketsOfUpcomingEvents(@Param("fromDate") LocalDateTime fromDate);

    /**
     * Entradas de un usuario para eventos de una ciudad.
     *
     * <p>Recorre tres entidades: {@code Ticket -> Event -> Venue}.</p>
     */
    @Query("""
           select t
           from Ticket t
           join t.event e
           join e.venue v
           where lower(t.user.email) = lower(:email)
             and lower(v.city) = lower(:city)
           order by e.eventDate asc
           """)
    List<Ticket> findTicketsByUserEmailAndCity(@Param("email") String email,
                                               @Param("city") String city);
}
