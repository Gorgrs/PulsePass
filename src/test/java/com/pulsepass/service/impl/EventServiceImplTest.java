package com.pulsepass.service.impl;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Venue;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.EventMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private VenueRepository venueRepository;
    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private EventMapper eventMapper;

    @InjectMocks
    private EventServiceImpl eventService;

    // ---------- helpers ----------

    private Venue buildVenue(boolean active) {
        Venue venue = new Venue("VEN-SMR-01", "Marina Convention Center",
                "Santa Marta", "Calle 1", 3);
        venue.setActive(active);
        return venue;
    }

    private Event buildEvent(EventStatus status) {
        Event event = new Event("CMF-2026", "Caribbean Music Fest 2026",
                EventCategory.MUSIC, status, LocalDateTime.now().plusDays(30), 18);
        event.setVenue(buildVenue(true));
        return event;
    }

    private CreateEventRequest buildRequest(LocalDateTime date) {
        return new CreateEventRequest("CMF-2026", "Caribbean Music Fest 2026",
                "Festival de musica", EventCategory.MUSIC, date, 18, "VEN-SMR-01");
    }

    private EventResponse buildResponse(EventStatus status) {
        return new EventResponse(1L, "CMF-2026", "Caribbean Music Fest 2026",
                "Festival de musica", EventCategory.MUSIC, status,
                LocalDateTime.now().plusDays(30), 18,
                "VEN-SMR-01", "Marina Convention Center", List.of());
    }

    // ---------- TEST-EVENT-001 ----------
    @Test
    void findByCode_existingEvent_returnsResponse() {
        // ARRANGE
        Event event = buildEvent(EventStatus.DRAFT);
        EventResponse response = buildResponse(EventStatus.DRAFT);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(eventMapper.toResponse(event)).thenReturn(response);

        // ACT
        EventResponse result = eventService.findByCode("CMF-2026");

        // ASSERT
        assertThat(result).isEqualTo(response);
    }

    // ---------- TEST-EVENT-002 ----------
    @Test
    void findByCode_missingEvent_throwsResourceNotFound() {
        // ARRANGE
        when(eventRepository.findByEventCode("NOPE")).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.findByCode("NOPE"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("NOPE");
    }

    // ---------- TEST-EVENT-003 ----------
    @Test
    void create_validEvent_savesAsDraft() {
        // ARRANGE
        CreateEventRequest request = buildRequest(LocalDateTime.now().plusDays(30));
        EventResponse response = buildResponse(EventStatus.DRAFT);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.empty());
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(buildVenue(true)));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventMapper.toResponse(any(Event.class))).thenReturn(response);

        // ACT
        EventResponse result = eventService.create(request);

        // ASSERT
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(result).isEqualTo(response);
    }

    // ---------- TEST-EVENT-004 ----------
    @Test
    void create_missingVenue_throwsAndNeverSaves() {
        // ARRANGE
        CreateEventRequest request = buildRequest(LocalDateTime.now().plusDays(30));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.empty());
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(eventRepository, never()).save(any());
    }

    // ---------- TEST-EVENT-005 ----------
    @Test
    void create_inactiveVenue_throwsBusinessRule() {
        // ARRANGE
        CreateEventRequest request = buildRequest(LocalDateTime.now().plusDays(30));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.empty());
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(buildVenue(false)));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.create(request))
                .isInstanceOf(BusinessRuleException.class);
        verify(eventRepository, never()).save(any());
    }

    // ---------- TEST-EVENT-006 ----------
    @Test
    void create_pastDate_throwsBusinessRule() {
        // ARRANGE
        CreateEventRequest request = buildRequest(LocalDateTime.now().minusDays(1));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.empty());
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(buildVenue(true)));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.create(request))
                .isInstanceOf(BusinessRuleException.class);
        verify(eventRepository, never()).save(any());
    }

    // ---------- TEST-EVENT-007 ----------
    @Test
    void publish_validDraft_changesToPublished() {
        // ARRANGE
        Event event = buildEvent(EventStatus.DRAFT);
        EventResponse response = buildResponse(EventStatus.PUBLISHED);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventMapper.toResponse(any(Event.class))).thenReturn(response);

        // ACT
        EventResponse result = eventService.publish("CMF-2026");

        // ASSERT
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(result.status()).isEqualTo(EventStatus.PUBLISHED);
        verify(eventRepository).save(event);
    }

    // ---------- TEST-EVENT-008 ----------
    @Test
    void publish_cancelledEvent_throwsAndNeverSaves() {
        // ARRANGE
        Event event = buildEvent(EventStatus.CANCELLED);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.publish("CMF-2026"))
                .isInstanceOf(BusinessRuleException.class);
        verify(eventRepository, never()).save(any());
    }
}