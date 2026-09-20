package com.pulsepass.repository;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.domain.User;
import com.pulsepass.domain.Venue;
import com.pulsepass.support.AbstractPersistenceIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.pulsepass.support.TestFixtures.event;
import static com.pulsepass.support.TestFixtures.ticket;
import static com.pulsepass.support.TestFixtures.user;
import static com.pulsepass.support.TestFixtures.venue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * QT-006: relaciones Ticket -> User y Ticket -> Event.
 * Cubre FR-TKT-001 a FR-TKT-008 y AC-005.
 */
class TicketRepositoryIT extends AbstractPersistenceIT {

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketRepository ticketRepository;

    private Event festival;
    private Event conferencia;
    private User andrea;
    private User carlos;

    @BeforeEach
    void setUp() {
        Venue marina = venueRepository.save(venue("VEN-SMR-01", "Santa Marta"));
        festival = eventRepository.save(event("CMF-2026", EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 7, 18, 19, 0), marina));
        conferencia = eventRepository.save(event("TECH-2026", EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 8, 5, 9, 0), marina));
        andrea = userRepository.save(user("andrea"));
        carlos = userRepository.save(user("carlos"));
    }

    @Test
    @DisplayName("QT-006 / FR-TKT-001: el ticket queda unido a un usuario y a un evento")
    void shouldPersistTicketWithBothRelations() {
        Ticket saved = ticketRepository.saveAndFlush(
                ticket("TCK-0001", TicketType.VIP, TicketStatus.PAID, "250000.00", andrea, festival));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUser().getUsername()).isEqualTo("andrea");
        assertThat(saved.getEvent().getEventCode()).isEqualTo("CMF-2026");
        assertThat(saved.getPrice()).isEqualByComparingTo(new BigDecimal("250000.00"));
    }

    @Test
    @DisplayName("AC-005: PostgreSQL rechaza un ticketCode duplicado")
    void shouldRejectDuplicatedTicketCode() {
        ticketRepository.saveAndFlush(
                ticket("TCK-0001", TicketType.VIP, TicketStatus.PAID, "250000.00", andrea, festival));

        assertThatThrownBy(() -> ticketRepository.saveAndFlush(
                ticket("TCK-0001", TicketType.GENERAL, TicketStatus.RESERVED, "120000.00", carlos, conferencia)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-TKT-003: el CHECK rechaza un precio negativo")
    void shouldRejectNegativePrice() {
        assertThatThrownBy(() -> ticketRepository.saveAndFlush(
                ticket("TCK-NEG", TicketType.GENERAL, TicketStatus.RESERVED, "-1.00", andrea, festival)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-TKT-006: tickets de un usuario buscados por su email")
    void shouldFindTicketsByUserEmail() {
        ticketRepository.save(ticket("TCK-0001", TicketType.VIP, TicketStatus.PAID,
                "250000.00", andrea, festival));
        ticketRepository.save(ticket("TCK-0002", TicketType.GENERAL, TicketStatus.RESERVED,
                "120000.00", andrea, conferencia));
        ticketRepository.save(ticket("TCK-0003", TicketType.GENERAL, TicketStatus.PAID,
                "120000.00", carlos, festival));
        ticketRepository.flush();

        List<Ticket> ticketsDeAndrea =
                ticketRepository.findByUserEmailIgnoreCaseOrderByPurchaseDateDesc("andrea@pulsepass.test");

        assertThat(ticketsDeAndrea)
                .extracting(Ticket::getTicketCode)
                .containsExactlyInAnyOrder("TCK-0001", "TCK-0002");
    }

    @Test
    @DisplayName("FR-TKT-006: el mismo usuario, acotado ademas por estado")
    void shouldFindTicketsByUserEmailAndStatus() {
        ticketRepository.save(ticket("TCK-0001", TicketType.VIP, TicketStatus.PAID,
                "250000.00", andrea, festival));
        ticketRepository.save(ticket("TCK-0002", TicketType.GENERAL, TicketStatus.RESERVED,
                "120000.00", andrea, conferencia));
        ticketRepository.flush();

        assertThat(ticketRepository.findByUserEmailIgnoreCaseAndStatusOrderByPurchaseDateDesc(
                "andrea@pulsepass.test", TicketStatus.PAID))
                .extracting(Ticket::getTicketCode)
                .containsExactly("TCK-0001");
    }

    @Test
    @DisplayName("FR-TKT-007: solo los tickets PAID del evento solicitado")
    void shouldFindPaidTicketsOfAnEvent() {
        ticketRepository.save(ticket("TCK-0001", TicketType.VIP, TicketStatus.PAID,
                "250000.00", andrea, festival));
        ticketRepository.save(ticket("TCK-0002", TicketType.GENERAL, TicketStatus.RESERVED,
                "120000.00", carlos, festival));
        ticketRepository.save(ticket("TCK-0003", TicketType.GENERAL, TicketStatus.PAID,
                "120000.00", carlos, conferencia));
        ticketRepository.flush();

        assertThat(ticketRepository.findByEventEventCodeAndStatusOrderByPurchaseDateAsc(
                "CMF-2026", TicketStatus.PAID))
                .extracting(Ticket::getTicketCode)
                .containsExactly("TCK-0001");
    }

    @Test
    @DisplayName("FR-TKT-008 / AC-008: el conteo de ventas solo suma los PAID")
    void shouldCountOnlyPaidTickets() {
        ticketRepository.save(ticket("TCK-0001", TicketType.VIP, TicketStatus.PAID,
                "250000.00", andrea, festival));
        ticketRepository.save(ticket("TCK-0002", TicketType.GENERAL, TicketStatus.PAID,
                "120000.00", carlos, festival));
        ticketRepository.save(ticket("TCK-0003", TicketType.GENERAL, TicketStatus.RESERVED,
                "120000.00", carlos, festival));
        ticketRepository.save(ticket("TCK-0004", TicketType.VIP, TicketStatus.CANCELLED,
                "250000.00", andrea, festival));
        ticketRepository.flush();

        assertThat(ticketRepository.countTicketsByEventCodeAndStatus("CMF-2026", TicketStatus.PAID))
                .isEqualTo(2L);
    }

    @Test
    @DisplayName("FR-SRC-004: tickets de eventos futuros, en orden cronologico")
    void shouldFindTicketsOfUpcomingEventsInChronologicalOrder() {
        ticketRepository.save(ticket("TCK-FEST", TicketType.VIP, TicketStatus.PAID,
                "250000.00", andrea, festival));       // 2026-07-18
        ticketRepository.save(ticket("TCK-TECH", TicketType.GENERAL, TicketStatus.PAID,
                "120000.00", carlos, conferencia));    // 2026-08-05
        ticketRepository.flush();

        List<Ticket> futuros = ticketRepository.findTicketsOfUpcomingEvents(
                LocalDateTime.of(2026, 1, 1, 0, 0));

        assertThat(futuros)
                .extracting(Ticket::getTicketCode)
                .containsExactly("TCK-FEST", "TCK-TECH");
    }

    @Test
    @DisplayName("JPQL recorriendo tres entidades: Ticket -> Event -> Venue")
    void shouldFindTicketsByUserAndCity() {
        ticketRepository.saveAndFlush(ticket("TCK-0001", TicketType.VIP, TicketStatus.PAID,
                "250000.00", andrea, festival));

        assertThat(ticketRepository.findTicketsByUserEmailAndCity(
                "andrea@pulsepass.test", "santa marta"))
                .extracting(Ticket::getTicketCode)
                .containsExactly("TCK-0001");
    }
}
