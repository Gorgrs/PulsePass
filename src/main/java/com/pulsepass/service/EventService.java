package com.pulsepass.service;

import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface EventService {

    // El rol ADMIN se exige en SecurityConfig.
    EventResponse create(CreateEventRequest request);

    /**
     * Un evento publicado lo ve cualquiera. Los demas estados (borrador,
     * cancelado, etc.) solo STAFF y ADMIN; para el resto responde 404, como si
     * no existiera, para no revelar que hay un borrador.
     */
    @PreAuthorize("@authorizationService.ensureEventVisible(#eventCode, authentication)")
    EventResponse findByCode(String eventCode);

    List<EventSummaryResponse> findPublishedEvents();

    // El rol ADMIN se exige en SecurityConfig.
    EventResponse publish(String eventCode);

    // El rol ADMIN se exige en SecurityConfig.
    EventResponse addArtist(String eventCode, Long artistId);

    /**
     * Para el publico y los USER, la lista solo trae eventos publicados.
     * STAFF y ADMIN ven todos.
     */
    @PostFilter("@authorizationService.canSeeEventStatus(filterObject.status(), authentication)")
    List<EventSummaryResponse> findByArtist(String stageName);
}