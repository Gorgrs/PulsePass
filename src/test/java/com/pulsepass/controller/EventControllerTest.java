package com.pulsepass.controller;

import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    private static final String VALID_EVENT_JSON = """
            {
              "eventCode": "CMF-2026",
              "name": "Caribbean Music Fest 2026",
              "description": "Festival de musica caribena",
              "category": "MUSIC",
              "eventDate": "2026-12-15T20:00:00",
              "minimumAge": 18,
              "venueCode": "VEN-SMR-01"
            }
            """;

    private EventResponse event(EventStatus eventStatus, List<ArtistResponse> artists) {
        return new EventResponse(1L, "CMF-2026", "Caribbean Music Fest 2026",
                "Festival de musica caribena", EventCategory.MUSIC, eventStatus,
                LocalDateTime.of(2026, 12, 15, 20, 0), 18,
                "VEN-SMR-01", "Marina Convention Center", artists);
    }

    private EventSummaryResponse summary() {
        return new EventSummaryResponse(1L, "CMF-2026", "Caribbean Music Fest 2026",
                EventCategory.MUSIC, EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 12, 15, 20, 0), "Marina Convention Center");
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(eventService.create(any(CreateEventRequest.class)))
                .thenReturn(event(EventStatus.DRAFT, List.of()));

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_EVENT_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        verify(eventService).create(any(CreateEventRequest.class));
    }

    @Test
    void create_invalidRequest_returns400() throws Exception {
        String invalidJson = """
                {
                  "eventCode": "",
                  "name": "Caribbean Music Fest 2026",
                  "category": "MUSIC",
                  "eventDate": "2026-12-15T20:00:00",
                  "minimumAge": 18,
                  "venueCode": ""
                }
                """;

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.eventCode").value("Event code is required"))
                .andExpect(jsonPath("$.details.venueCode").value("Venue code is required"));

        verify(eventService, never()).create(any(CreateEventRequest.class));
    }

    @Test
    void findByCode_existingEvent_returns200() throws Exception {
        when(eventService.findByCode("CMF-2026")).thenReturn(event(EventStatus.DRAFT, List.of()));

        mockMvc.perform(get("/api/events/CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.venueCode").value("VEN-SMR-01"));

        verify(eventService).findByCode("CMF-2026");
    }

    @Test
    void findByCode_unknownEvent_returns404() throws Exception {
        when(eventService.findByCode("XXX-2026"))
                .thenThrow(new ResourceNotFoundException("Event not found: XXX-2026"));

        mockMvc.perform(get("/api/events/XXX-2026"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Event not found: XXX-2026"));
    }

    @Test
    void findPublishedEvents_returns200() throws Exception {
        when(eventService.findPublishedEvents()).thenReturn(List.of(summary()));

        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));

        verify(eventService).findPublishedEvents();
    }

    @Test
    void publish_validTransition_returns200() throws Exception {
        when(eventService.publish("CMF-2026")).thenReturn(event(EventStatus.PUBLISHED, List.of()));

        mockMvc.perform(patch("/api/events/CMF-2026/publish"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(eventService).publish("CMF-2026");
    }

    @Test
    void publish_invalidTransition_returns409() throws Exception {
        when(eventService.publish("CMF-2026"))
                .thenThrow(new BusinessRuleException("Event cannot be published: CMF-2026"));

        mockMvc.perform(patch("/api/events/CMF-2026/publish"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Event cannot be published: CMF-2026"));
    }

    @Test
    void addArtist_returns200() throws Exception {
        List<ArtistResponse> artists = List.of(
                new ArtistResponse(1L, "Solar Beat", "Colombia", "Pop", true));
        when(eventService.addArtist("CMF-2026", 1L)).thenReturn(event(EventStatus.DRAFT, artists));

        mockMvc.perform(post("/api/events/CMF-2026/artists/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.artists.length()").value(1))
                .andExpect(jsonPath("$.artists[0].stageName").value("Solar Beat"));

        verify(eventService).addArtist("CMF-2026", 1L);
    }

    @Test
    void findByArtist_returns200() throws Exception {
        when(eventService.findByArtist("Solar Beat")).thenReturn(List.of(summary()));

        mockMvc.perform(get("/api/events/by-artist").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"));

        verify(eventService).findByArtist("Solar Beat");
    }
}