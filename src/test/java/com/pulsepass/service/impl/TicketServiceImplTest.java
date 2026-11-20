package com.pulsepass.service.impl;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.domain.Venue;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.TicketMapper;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.TicketPriceCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    private static final String USER_EMAIL = "andrea@email.com";
    private static final String EVENT_CODE = "CMF-2026";
    private static final BigDecimal VIP_PRICE = new BigDecimal("200000.00");

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private TicketMapper ticketMapper;
    @Mock
    private TicketPriceCalculator priceCalculator;

    @InjectMocks
    private TicketServiceImpl ticketService;

    // ---------- helpers ----------

    private User buildUser(boolean active, LocalDate birthDate) {
        User user = new User("andrea", USER_EMAIL);
        user.setActive(active);
        user.assignProfile(new UserProfile("Andrea", "Perez", "3001234567", "Santa Marta", birthDate));
        return user;
    }

    private Event buildEvent(EventStatus status) {
        Venue venue = new Venue("VEN-SMR-01", "Marina Convention Center",
                "Santa Marta", "Calle 1", 3);
        Event event = new Event(EVENT_CODE, "Caribbean Music Fest 2026",
                EventCategory.MUSIC, status, LocalDateTime.now().plusDays(30), 18);
        event.setVenue(venue);
        return event;
    }

    private Ticket buildTicket(TicketStatus status) {
        return new Ticket("TKT-TEST0001", TicketType.VIP, VIP_PRICE, status,
                LocalDateTime.now(), buildUser(true, LocalDate.now().minusYears(25)),
                buildEvent(EventStatus.PUBLISHED));
    }

    private TicketResponse buildResponse(TicketStatus status) {
        return new TicketResponse(1L, "TKT-TEST0001", TicketType.VIP, VIP_PRICE, status,
                LocalDateTime.now(), USER_EMAIL, EVENT_CODE, "Caribbean Music Fest 2026");
    }

    private PurchaseTicketRequest buildRequest() {
        return new PurchaseTicketRequest(USER_EMAIL, EVENT_CODE, TicketType.VIP);
    }

    // ---------- TEST-TICKET-001 ----------
    @Test
    void purchase_validRequest_savesPaidTicket() {
        // ARRANGE
        User user = buildUser(true, LocalDate.now().minusYears(25));
        Event event = buildEvent(EventStatus.PUBLISHED);
        TicketResponse response = buildResponse(TicketStatus.PAID);
        when(userRepository.findByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(ticketRepository.countTicketsByEventCodeAndStatus(EVENT_CODE, TicketStatus.PAID)).thenReturn(0L);
        when(priceCalculator.calculate(TicketType.VIP)).thenReturn(VIP_PRICE);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(response);

        // ACT
        TicketResponse result = ticketService.purchase(buildRequest());

        // ASSERT
        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TicketStatus.PAID);
        assertThat(captor.getValue().getPrice()).isEqualByComparingTo(VIP_PRICE);
        assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(result).isEqualTo(response);
    }

    // ---------- TEST-TICKET-002 ----------
    @Test
    void purchase_missingUser_throwsResourceNotFound() {
        // ARRANGE
        when(userRepository.findByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(buildRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(USER_EMAIL);
        verify(ticketRepository, never()).save(any());
    }

    // ---------- TEST-TICKET-003 ----------
    @Test
    void purchase_inactiveUser_throwsBusinessRule() {
        // ARRANGE
        User user = buildUser(false, LocalDate.now().minusYears(30));
        when(userRepository.findByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.of(user));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(buildRequest()))
                .isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, never()).save(any());
    }

    // ---------- TEST-TICKET-004 ----------
    @Test
    void purchase_draftEvent_throwsBusinessRule() {
        // ARRANGE
        User user = buildUser(true, LocalDate.now().minusYears(25));
        when(userRepository.findByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode(EVENT_CODE))
                .thenReturn(Optional.of(buildEvent(EventStatus.DRAFT)));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(buildRequest()))
                .isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, never()).save(any());
    }

    // ---------- TEST-TICKET-005 ----------
    @Test
    void purchase_cancelledEvent_throwsBusinessRule() {
        // ARRANGE
        User user = buildUser(true, LocalDate.now().minusYears(25));
        when(userRepository.findByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode(EVENT_CODE))
                .thenReturn(Optional.of(buildEvent(EventStatus.CANCELLED)));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(buildRequest()))
                .isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, never()).save(any());
    }

    // ---------- TEST-TICKET-006 ----------
    @Test
    void purchase_underageUser_throwsBusinessRule() {
        // ARRANGE (Laura, 17 anios; el evento exige 18)
        User laura = buildUser(true, LocalDate.now().minusYears(17));
        when(userRepository.findByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.of(laura));
        when(eventRepository.findByEventCode(EVENT_CODE))
                .thenReturn(Optional.of(buildEvent(EventStatus.PUBLISHED)));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(buildRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("minimum age");
        verify(ticketRepository, never()).save(any());
    }

    // ---------- TEST-TICKET-007 ----------
    @Test
    void purchase_noCapacityLeft_throwsBusinessRule() {
        // ARRANGE (capacidad 3, ya hay 3 tickets pagados)
        User user = buildUser(true, LocalDate.now().minusYears(25));
        when(userRepository.findByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode(EVENT_CODE))
                .thenReturn(Optional.of(buildEvent(EventStatus.PUBLISHED)));
        when(ticketRepository.countTicketsByEventCodeAndStatus(EVENT_CODE, TicketStatus.PAID)).thenReturn(3L);

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.purchase(buildRequest()))
                .isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, never()).save(any());
    }

    // ---------- TEST-TICKET-008 ----------
    @Test
    void purchase_lastAvailableTicket_savesTicketAndMarksEventSoldOut() {
        // ARRANGE (capacidad 3, ya hay 2 pagados: esta compra es la ultima)
        User user = buildUser(true, LocalDate.now().minusYears(25));
        Event event = buildEvent(EventStatus.PUBLISHED);
        when(userRepository.findByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode(EVENT_CODE)).thenReturn(Optional.of(event));
        when(ticketRepository.countTicketsByEventCodeAndStatus(EVENT_CODE, TicketStatus.PAID)).thenReturn(2L);
        when(priceCalculator.calculate(TicketType.VIP)).thenReturn(VIP_PRICE);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(buildResponse(TicketStatus.PAID));

        // ACT
        ticketService.purchase(buildRequest());

        // ASSERT
        verify(ticketRepository).save(any(Ticket.class));
        verify(eventRepository).save(event);
        assertThat(event.getStatus()).isEqualTo(EventStatus.SOLD_OUT);
    }

    // ---------- TEST-TICKET-009 ----------
    @Test
    void cancel_paidTicket_changesToCancelled() {
        // ARRANGE
        Ticket ticket = buildTicket(TicketStatus.PAID);
        when(ticketRepository.findByTicketCode("TKT-TEST0001")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(buildResponse(TicketStatus.CANCELLED));

        // ACT
        TicketResponse result = ticketService.cancel("TKT-TEST0001");

        // ASSERT
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        assertThat(result.status()).isEqualTo(TicketStatus.CANCELLED);
        verify(ticketRepository).save(ticket);
    }

    // ---------- TEST-TICKET-010 ----------
    @Test
    void cancel_usedTicket_throwsBusinessRule() {
        // ARRANGE
        Ticket ticket = buildTicket(TicketStatus.USED);
        when(ticketRepository.findByTicketCode("TKT-TEST0001")).thenReturn(Optional.of(ticket));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.cancel("TKT-TEST0001"))
                .isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, never()).save(any());
    }

    // ---------- TEST-TICKET-011 ----------
    @Test
    void markAsUsed_paidTicket_changesToUsed() {
        // ARRANGE
        Ticket ticket = buildTicket(TicketStatus.PAID);
        when(ticketRepository.findByTicketCode("TKT-TEST0001")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(buildResponse(TicketStatus.USED));

        // ACT
        TicketResponse result = ticketService.markAsUsed("TKT-TEST0001");

        // ASSERT
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.USED);
        assertThat(result.status()).isEqualTo(TicketStatus.USED);
        verify(ticketRepository).save(eq(ticket));
    }

    // ---------- TEST-TICKET-012 ----------
    @Test
    void markAsUsed_cancelledTicket_throwsBusinessRule() {
        // ARRANGE
        Ticket ticket = buildTicket(TicketStatus.CANCELLED);
        when(ticketRepository.findByTicketCode("TKT-TEST0001")).thenReturn(Optional.of(ticket));

        // ACT + ASSERT
        assertThatThrownBy(() -> ticketService.markAsUsed("TKT-TEST0001"))
                .isInstanceOf(BusinessRuleException.class);
        verify(ticketRepository, never()).save(any());
    }
}