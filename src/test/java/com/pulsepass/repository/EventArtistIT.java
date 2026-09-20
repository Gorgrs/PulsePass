package com.pulsepass.repository;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Venue;
import com.pulsepass.support.AbstractPersistenceIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static com.pulsepass.support.TestFixtures.event;
import static com.pulsepass.support.TestFixtures.venue;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * QT-005: relacion N:M Event - Artist. Cubre FR-ART-003, FR-ART-004 y AC-003.
 */
class EventArtistIT extends AbstractPersistenceIT {

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Venue marina;

    @BeforeEach
    void setUp() {
        marina = venueRepository.save(venue("VEN-SMR-01", "Santa Marta"));
    }

    private Artist catalogArtist(String stageName) {
        // Los artistas provienen del catalogo cargado por la migracion V2.
        return artistRepository.findByStageNameIgnoreCase(stageName).orElseThrow();
    }

    @Test
    @DisplayName("FR-ART-003: un evento asocia varios artistas y la tabla intermedia los refleja")
    void shouldAssociateSeveralArtistsToAnEvent() {
        Event festival = event("CMF-2026", EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 7, 18, 19, 0), marina);
        festival.addArtist(catalogArtist("Solar Beat"));
        festival.addArtist(catalogArtist("Neon Waves"));
        festival.addArtist(catalogArtist("Caribbean Sound"));

        Event saved = eventRepository.saveAndFlush(festival);

        assertThat(saved.getArtists()).hasSize(3);

        Integer rows = jdbcTemplate.queryForObject(
                "select count(*) from event_artists where event_id = ?",
                Integer.class, saved.getId());
        assertThat(rows).isEqualTo(3);
    }

    @Test
    @DisplayName("AC-003: no se duplica el mismo par evento-artista")
    void shouldNotDuplicateEventArtistPair() {
        Artist solarBeat = catalogArtist("Solar Beat");

        Event festival = event("CMF-2026", EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 7, 18, 19, 0), marina);
        festival.addArtist(solarBeat);
        festival.addArtist(solarBeat);

        Event saved = eventRepository.saveAndFlush(festival);

        // El Set impide el duplicado en memoria; la PK compuesta de
        // event_artists lo impediria de todos modos en PostgreSQL.
        assertThat(saved.getArtists()).hasSize(1);

        Integer rows = jdbcTemplate.queryForObject(
                "select count(*) from event_artists where event_id = ?",
                Integer.class, saved.getId());
        assertThat(rows).isEqualTo(1);
    }

    @Test
    @DisplayName("FR-ART-004 / AC-007: un artista participa en varios eventos y aparece una sola vez")
    void shouldFindEventsOfAnArtistWithoutDuplicates() {
        Artist solarBeat = catalogArtist("Solar Beat");
        Artist neonWaves = catalogArtist("Neon Waves");

        Event primero = event("CMF-2026", EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 7, 18, 19, 0), marina);
        primero.addArtist(solarBeat);
        primero.addArtist(neonWaves);

        Event segundo = event("NYE-2026", EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 12, 31, 22, 0), marina);
        segundo.addArtist(solarBeat);

        Event tercero = event("TECH-2026", EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 8, 5, 9, 0), marina);
        tercero.addArtist(neonWaves);

        eventRepository.saveAll(List.of(primero, segundo, tercero));
        eventRepository.flush();

        List<Event> eventosDeSolarBeat = eventRepository.findEventsByArtistStageName("Solar Beat");

        assertThat(eventosDeSolarBeat)
                .extracting(Event::getEventCode)
                .containsExactly("CMF-2026", "NYE-2026");
        assertThat(eventosDeSolarBeat).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("La busqueda por artista ignora mayusculas y minusculas")
    void shouldFindEventsByArtistIgnoringCase() {
        Event festival = event("CMF-2026", EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 7, 18, 19, 0), marina);
        festival.addArtist(catalogArtist("Solar Beat"));
        eventRepository.saveAndFlush(festival);

        assertThat(eventRepository.findEventsByArtistStageName("solar beat"))
                .extracting(Event::getEventCode)
                .containsExactly("CMF-2026");
    }

    @Test
    @DisplayName("Navegacion inversa: artistas de un evento, en orden alfabetico")
    void shouldFindArtistsOfAnEvent() {
        Event festival = event("CMF-2026", EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 7, 18, 19, 0), marina);
        festival.addArtist(catalogArtist("Solar Beat"));
        festival.addArtist(catalogArtist("Neon Waves"));
        festival.addArtist(catalogArtist("Caribbean Sound"));
        eventRepository.saveAndFlush(festival);

        assertThat(artistRepository.findArtistsByEventCode("CMF-2026"))
                .extracting(Artist::getStageName)
                .containsExactly("Caribbean Sound", "Neon Waves", "Solar Beat");
    }
}
