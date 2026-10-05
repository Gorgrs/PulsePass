package com.pulsepass.service.impl;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Venue;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.EventMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class EventServiceImplAddArtistTest {

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

    private Event buildEvent(EventStatus status) {
        Venue venue = new Venue("VEN-SMR-01", "Marina Convention Center",
                "Santa Marta", "Calle 1", 3);
        Event event = new Event("CMF-2026", "Caribbean Music Fest 2026",
                EventCategory.MUSIC, status, LocalDateTime.now().plusDays(30), 18);
        event.setVenue(venue);
        return event;
    }

    private Artist buildArtist() {
        return new Artist("Solar Beat", "Colombia", "Pop");
    }

    private EventResponse buildResponse() {
        return new EventResponse(1L, "CMF-2026", "Caribbean Music Fest 2026",
                "Festival de musica", EventCategory.MUSIC, EventStatus.DRAFT,
                LocalDateTime.now().plusDays(30), 18,
                "VEN-SMR-01", "Marina Convention Center", List.of());
    }

    // ---------- addArtist: caso valido ----------
    @Test
    void addArtist_validArtist_associatesAndSaves() {
        // ARRANGE
        Event event = buildEvent(EventStatus.DRAFT);
        Artist artist = buildArtist();
        EventResponse response = buildResponse();
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventMapper.toResponse(any(Event.class))).thenReturn(response);

        // ACT
        EventResponse result = eventService.addArtist("CMF-2026", 1L);

        // ASSERT
        assertThat(event.getArtists()).contains(artist);
        assertThat(result).isEqualTo(response);
        verify(eventRepository).save(event);
    }

    // ---------- BR-EVENT-010: artista duplicado ----------
    @Test
    void addArtist_duplicateArtist_throwsDuplicateAndNeverSaves() {
        // ARRANGE
        Event event = buildEvent(EventStatus.DRAFT);
        Artist artist = buildArtist();
        event.addArtist(artist);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(artistRepository.findById(1L)).thenReturn(Optional.of(artist));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.addArtist("CMF-2026", 1L))
                .isInstanceOf(DuplicateResourceException.class);
        verify(eventRepository, never()).save(any());
    }

    // ---------- BR-EVENT-011: evento cancelado ----------
    @Test
    void addArtist_cancelledEvent_throwsBusinessRuleAndNeverSaves() {
        // ARRANGE
        Event event = buildEvent(EventStatus.CANCELLED);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(artistRepository.findById(1L)).thenReturn(Optional.of(buildArtist()));

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.addArtist("CMF-2026", 1L))
                .isInstanceOf(BusinessRuleException.class);
        verify(eventRepository, never()).save(any());
    }

    // ---------- artista inexistente ----------
    @Test
    void addArtist_missingArtist_throwsResourceNotFoundAndNeverSaves() {
        // ARRANGE
        Event event = buildEvent(EventStatus.DRAFT);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(artistRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> eventService.addArtist("CMF-2026", 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
        verify(eventRepository, never()).save(any());
    }
}