package com.pulsepass.repository;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.domain.Venue;
import com.pulsepass.support.AbstractPersistenceIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Escenario de referencia completo (seccion 16 del PRD) y consultas de
 * descubrimiento FR-SRC-001 a FR-SRC-004.
 *
 * <p>Monta de una sola vez el venue VEN-SMR-01, el evento CMF-2026 con sus
 * tres artistas y los cuatro tickets de ejemplo, y sobre eso ejecuta las
 * consultas de la seccion 14.</p>
 */
class EventSearchIT extends AbstractPersistenceIT {

    private static final LocalDateTime FECHA_FESTIVAL = LocalDateTime.of(2026, 7, 18, 19, 0);

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketRepository ticketRepository;

    private Event festival;

    @BeforeEach
    void montarEscenarioDeReferencia() {
        // 16.1 Venue
        Venue marina = venueRepository.save(new Venue(
                "VEN-SMR-01", "Marina Convention Center",
                "Santa Marta", "Carrera 1 # 22-58", 5000));

        Venue coliseo = venueRepository.save(new Venue(
                "VEN-BOG-01", "Coliseo Norte",
                "Bogota", "Calle 100 # 15-20", 8000));

        // 16.2 Evento principal
        festival = new Event("CMF-2026", "Caribbean Music Fest 2026",
                EventCategory.MUSIC, EventStatus.PUBLISHED, FECHA_FESTIVAL, 18);
        festival.setDescription("Festival de musica del Caribe colombiano.");
        marina.addEvent(festival);
        festival.addArtist(artista("Solar Beat"));
        festival.addArtist(artista("Neon Waves"));
        festival.addArtist(artista("Caribbean Sound"));
        eventRepository.save(festival);

        // Un evento de otra ciudad, para comprobar que los filtros discriminan.
        Event enBogota = new Event("BOG-2026", "Bogota Electronic Night",
                EventCategory.MUSIC, EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 9, 12, 21, 0), 18);
        coliseo.addEvent(enBogota);
        enBogota.addArtist(artista("Solar Beat"));
        eventRepository.save(enBogota);

        // Un evento en borrador, que ninguna consulta de cartelera debe traer.
        Event borrador = new Event("CMF-2027", "Caribbean Music Fest 2027",
                EventCategory.MUSIC, EventStatus.DRAFT,
                LocalDateTime.of(2027, 7, 17, 19, 0), 18);
        marina.addEvent(borrador);
        borrador.addArtist(artista("Solar Beat"));
        eventRepository.save(borrador);

        // 16.3 Tickets de ejemplo
        ticketRepository.saveAll(List.of(
                entrada("TCK-0001", "andrea", TicketType.VIP, TicketStatus.PAID, "250000.00"),
                entrada("TCK-0002", "carlos", TicketType.GENERAL, TicketStatus.PAID, "120000.00"),
                entrada("TCK-0003", "laura", TicketType.GENERAL, TicketStatus.RESERVED, "120000.00"),
                entrada("TCK-0004", "miguel", TicketType.VIP, TicketStatus.CANCELLED, "250000.00")
        ));

        eventRepository.flush();
    }

    private Artist artista(String stageName) {
        return artistRepository.findByStageNameIgnoreCase(stageName).orElseThrow();
    }

    private Ticket entrada(String ticketCode, String nombre, TicketType tipo,
                           TicketStatus estado, String precio) {
        User usuario = new User(nombre, nombre + "@pulsepass.test");
        usuario.assignProfile(new UserProfile(
                nombre, "Apellido", "+57 300 000 0000", "Santa Marta",
                LocalDate.of(2000, 1, 1)));
        userRepository.save(usuario);

        return new Ticket(ticketCode, tipo, new BigDecimal(precio), estado,
                LocalDateTime.of(2026, 5, 10, 12, 0), usuario, festival);
    }

    @Test
    @DisplayName("UC-06 / AC-006: la cartelera publicada excluye el evento en DRAFT")
    void shouldListPublishedBillboard() {
        assertThat(eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED))
                .extracting(Event::getEventCode)
                .containsExactly("CMF-2026", "BOG-2026")
                .doesNotContain("CMF-2027");
    }

    @Test
    @DisplayName("FR-SRC-001 / UC-07 / AC-007: eventos de Solar Beat, cada uno una sola vez")
    void shouldFindEventsByArtist() {
        List<Event> eventos = eventRepository.findEventsByArtistStageName("Solar Beat");

        assertThat(eventos)
                .extracting(Event::getEventCode)
                .containsExactly("CMF-2026", "BOG-2026", "CMF-2027");
        assertThat(eventos).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("FR-SRC-002: filtra por ciudad del venue y nombre del artista")
    void shouldFindEventsByCityAndArtist() {
        assertThat(eventRepository.findEventsByCityAndArtist("Santa Marta", "Solar Beat"))
                .extracting(Event::getEventCode)
                .containsExactly("CMF-2026", "CMF-2027")
                .doesNotContain("BOG-2026");
    }

    @Test
    @DisplayName("FR-SRC-002: un artista que no toca en esa ciudad no devuelve nada")
    void shouldReturnEmptyWhenArtistDoesNotPlayInThatCity() {
        assertThat(eventRepository.findEventsByCityAndArtist("Bogota", "Caribbean Sound"))
                .isEmpty();
    }

    @Test
    @DisplayName("FR-SRC-003 / UC-09: recomendados = publicados + posteriores a una fecha + ciudad + artista parcial")
    void shouldFindRecommendedEvents() {
        List<Event> recomendados = eventRepository.findRecommendedEvents(
                EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                "santa marta",
                "solar");

        // CMF-2027 queda fuera por estar en DRAFT y BOG-2026 por la ciudad.
        assertThat(recomendados)
                .extracting(Event::getEventCode)
                .containsExactly("CMF-2026");
    }

    @Test
    @DisplayName("FR-SRC-003: la fecha de corte descarta los eventos ya pasados")
    void shouldExcludeEventsBeforeTheCutoffDate() {
        assertThat(eventRepository.findRecommendedEvents(
                EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 12, 31, 23, 59),
                "santa marta",
                "solar"))
                .isEmpty();
    }

    @Test
    @DisplayName("UC-08 / AC-008: de los cuatro tickets del escenario solo dos son PAID")
    void shouldCountSalesOfTheScenario() {
        assertThat(ticketRepository.countTicketsByEventCodeAndStatus("CMF-2026", TicketStatus.PAID))
                .isEqualTo(2L);

        assertThat(ticketRepository.findByEventEventCodeAndStatusOrderByPurchaseDateAsc(
                "CMF-2026", TicketStatus.PAID))
                .extracting(Ticket::getTicketCode)
                .containsExactlyInAnyOrder("TCK-0001", "TCK-0002");
    }

    @Test
    @DisplayName("El evento de referencia carga su venue y sus tres artistas en una sola consulta")
    void shouldFetchEventWithVenueAndArtists() {
        Event encontrado = eventRepository.findByEventCodeWithArtists("CMF-2026").orElseThrow();

        assertThat(encontrado.getName()).isEqualTo("Caribbean Music Fest 2026");
        assertThat(encontrado.getVenue().getCity()).isEqualTo("Santa Marta");
        assertThat(encontrado.getArtists())
                .extracting(Artist::getStageName)
                .containsExactlyInAnyOrder("Solar Beat", "Neon Waves", "Caribbean Sound");
    }

    @Test
    @DisplayName("FR-TKT-006: Andrea ve su unica entrada VIP pagada")
    void shouldFindAndreaTickets() {
        assertThat(ticketRepository.findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(
                "andrea@pulsepass.test"))
                .extracting(Ticket::getTicketCode, Ticket::getType, Ticket::getStatus)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                "TCK-0001", TicketType.VIP, TicketStatus.PAID));
    }
}
