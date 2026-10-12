package com.pulsepass.controller;

import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
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

@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    private static final String VALID_PURCHASE_JSON = """
            {
              "userEmail": "andrea@correo.com",
              "eventCode": "CMF-2026",
              "type": "GENERAL"
            }
            """;

    private TicketResponse ticket(TicketStatus ticketStatus) {
        return new TicketResponse(1L, "TKT-0001", TicketType.GENERAL,
                new BigDecimal("50000"), ticketStatus,
                LocalDateTime.of(2026, 10, 12, 14, 0),
                "andrea@correo.com", "CMF-2026", "Caribbean Music Fest 2026");
    }

    @Test
    void purchase_validRequest_returns201() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenReturn(ticket(TicketStatus.PAID));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PURCHASE_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TKT-0001"))
                .andExpect(jsonPath("$.status").value("PAID"));

        verify(ticketService).purchase(any(PurchaseTicketRequest.class));
    }

    @Test
    void purchase_invalidRequest_returns400() throws Exception {
        String invalidJson = """
                {
                  "userEmail": "no-es-un-email",
                  "eventCode": ""
                }
                """;

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.userEmail").value("User email must be valid"))
                .andExpect(jsonPath("$.details.eventCode").value("Event code is required"))
                .andExpect(jsonPath("$.details.type").value("Ticket type is required"));

        verify(ticketService, never()).purchase(any(PurchaseTicketRequest.class));
    }

    @Test
    void purchase_unknownUser_returns404() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found: andrea@correo.com"));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PURCHASE_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found: andrea@correo.com"));
    }

    @Test
    void purchase_businessRuleViolated_returns409() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new BusinessRuleException("User does not meet the minimum age"));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_PURCHASE_JSON))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("User does not meet the minimum age"));
    }

    @Test
    void findByCode_existingTicket_returns200() throws Exception {
        when(ticketService.findByCode("TKT-0001")).thenReturn(ticket(TicketStatus.PAID));

        mockMvc.perform(get("/api/tickets/TKT-0001"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TKT-0001"))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"));

        verify(ticketService).findByCode("TKT-0001");
    }

    @Test
    void findByUserEmail_returns200() throws Exception {
        when(ticketService.findByUserEmail("andrea@correo.com"))
                .thenReturn(List.of(ticket(TicketStatus.PAID)));

        mockMvc.perform(get("/api/tickets/by-user").param("email", "andrea@correo.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userEmail").value("andrea@correo.com"));

        verify(ticketService).findByUserEmail("andrea@correo.com");
    }

    @Test
    void findPaidTicketsByEvent_returns200() throws Exception {
        when(ticketService.findPaidTicketsByEvent("CMF-2026"))
                .thenReturn(List.of(ticket(TicketStatus.PAID)));

        mockMvc.perform(get("/api/events/CMF-2026/tickets/paid"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PAID"));

        verify(ticketService).findPaidTicketsByEvent("CMF-2026");
    }

    @Test
    void cancel_validTransition_returns200() throws Exception {
        when(ticketService.cancel("TKT-0001")).thenReturn(ticket(TicketStatus.CANCELLED));

        mockMvc.perform(patch("/api/tickets/TKT-0001/cancel"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(ticketService).cancel("TKT-0001");
    }

    @Test
    void cancel_invalidTransition_returns409() throws Exception {
        when(ticketService.cancel("TKT-0001"))
                .thenThrow(new BusinessRuleException("A used ticket cannot be cancelled"));

        mockMvc.perform(patch("/api/tickets/TKT-0001/cancel"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("A used ticket cannot be cancelled"));
    }

    @Test
    void markAsUsed_validTransition_returns200() throws Exception {
        when(ticketService.markAsUsed("TKT-0001")).thenReturn(ticket(TicketStatus.USED));

        mockMvc.perform(patch("/api/tickets/TKT-0001/use"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("USED"));

        verify(ticketService).markAsUsed("TKT-0001");
    }

    @Test
    void markAsUsed_invalidTransition_returns409() throws Exception {
        when(ticketService.markAsUsed("TKT-0001"))
                .thenThrow(new BusinessRuleException("A cancelled ticket cannot be used"));

        mockMvc.perform(patch("/api/tickets/TKT-0001/use"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("A cancelled ticket cannot be used"));
    }
}