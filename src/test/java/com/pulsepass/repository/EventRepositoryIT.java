package com.pulsepass.repository;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Venue;
import com.pulsepass.support.AbstractPersistenceIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.pulsepass.support.TestFixtures.event;
import static com.pulsepass.support.TestFixtures.venue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * QT-003 (Venue 1:N Event), FR-EVT-001 a FR-EVT-006, FR-VEN-004 y AC-002/AC-006.
 */
class EventRepositoryIT extends AbstractPersistenceIT {

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    private Venue marina;
    private Venue coliseo;

    @BeforeEach
    void setUp() {
        marina = venueRepository.save(venue("VEN-SMR-01", "Santa Marta"));
        coliseo = venueRepository.save(venue("VEN-BOG-01", "Bogota"));
    }

    @Test
    @DisplayName("QT-003: un venue alberga varios eventos y la FK vive en events")
    void shouldPersistOneToManyBetweenVenueAndEvents() {
        eventRepository.save(event("EVT-001", EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 3, 10, 20, 0), marina));
        eventRepository.save(event("EVT-002", EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 4, 12, 20, 0), marina));
        eventRepository.flush();

        List<Event> events = eventRepository.findByVenueCodeOrderByEventDateAsc("VEN-SMR-01");

        assertThat(events).hasSize(2);
        assertThat(events).allSatisfy(e ->
                assertThat(e.getVenue().getCode()).isEqualTo("VEN-SMR-01"));
    }

    @Test
    @DisplayName("AC-002: se recupera el evento por eventCode junto con su venue")
    void shouldFindEventByCodeWithItsVenue() {
        eventRepository.save(event("CMF-2026", EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 7, 18, 19, 0), marina));

        Optional<Event> found = eventRepository.findByEventCode("CMF-2026");

        assertThat(found).isPresent();
        assertThat(found.get().getVenue().getCode()).isEqualTo("VEN-SMR-01");
        assertThat(found.get().getVenue().getCity()).isEqualTo("Santa Marta");
    }

    @Test
    @DisplayName("FR-EVT-005 / AC-006: la cartelera solo trae PUBLISHED, en orden cronologico")
    void shouldListOnlyPublishedEventsOrderedByDate() {
        eventRepository.save(event("EVT-DRAFT", EventStatus.DRAFT,
                LocalDateTime.of(2026, 1, 5, 20, 0), marina));
        eventRepository.save(event("EVT-CANCEL", EventStatus.CANCELLED,
                LocalDateTime.of(2026, 1, 6, 20, 0), marina));
        eventRepository.save(event("EVT-LATE", EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 9, 1, 20, 0), marina));
        eventRepository.save(event("EVT-EARLY", EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 2, 1, 20, 0), marina));
        eventRepository.flush();

        List<Event> published = eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED);

        assertThat(published)
                .extracting(Event::getEventCode)
                .containsExactly("EVT-EARLY", "EVT-LATE")
                .doesNotContain("EVT-DRAFT", "EVT-CANCEL");
    }

    @Test
    @DisplayName("FR-VEN-004: los eventos de un venue no incluyen los de otro")
    void shouldNotMixEventsFromOtherVenues() {
        eventRepository.save(event("EVT-SMR", EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 3, 10, 20, 0), marina));
        eventRepository.save(event("EVT-BOG", EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 3, 11, 20, 0), coliseo));
        eventRepository.flush();

        assertThat(eventRepository.findByVenueCodeOrderByEventDateAsc("VEN-SMR-01"))
                .extracting(Event::getEventCode)
                .containsExactly("EVT-SMR");
    }

    @Test
    @DisplayName("Query Method navegando dos niveles: eventos publicados de una ciudad")
    void shouldFindPublishedEventsByCity() {
        eventRepository.save(event("EVT-SMR", EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 3, 10, 20, 0), marina));
        eventRepository.save(event("EVT-SMR-DRAFT", EventStatus.DRAFT,
                LocalDateTime.of(2026, 3, 12, 20, 0), marina));
        eventRepository.flush();

        assertThat(eventRepository.findByVenueCityIgnoreCaseAndStatusOrderByEventDateAsc(
                "santa marta", EventStatus.PUBLISHED))
                .extracting(Event::getEventCode)
                .containsExactly("EVT-SMR");
    }

    @Test
    @DisplayName("FR-EVT-006: streaming_url (migracion V3) es opcional y se persiste")
    void shouldStoreOptionalStreamingUrl() {
        Event presencial = eventRepository.save(event("EVT-LIVE", EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 5, 1, 20, 0), marina));

        Event hibrido = event("EVT-HYBRID", EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 5, 2, 20, 0), marina);
        hibrido.setStreamingUrl("https://stream.pulsepass.test/evt-hybrid");
        eventRepository.save(hibrido);
        eventRepository.flush();

        assertThat(presencial.getStreamingUrl()).isNull();
        assertThat(eventRepository.findByEventCode("EVT-HYBRID"))
                .get()
                .extracting(Event::getStreamingUrl)
                .isEqualTo("https://stream.pulsepass.test/evt-hybrid");
    }

    @Test
    @DisplayName("FR-EVT-002: PostgreSQL rechaza un eventCode duplicado")
    void shouldRejectDuplicatedEventCode() {
        eventRepository.saveAndFlush(event("EVT-DUP", EventStatus.DRAFT,
                LocalDateTime.of(2026, 3, 10, 20, 0), marina));

        assertThatThrownBy(() -> eventRepository.saveAndFlush(
                event("EVT-DUP", EventStatus.DRAFT,
                        LocalDateTime.of(2026, 4, 10, 20, 0), coliseo)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
