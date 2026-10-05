package com.pulsepass.service.impl;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.TicketMapper;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.TicketPriceCalculator;
import com.pulsepass.service.TicketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@Service
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final TicketMapper ticketMapper;
    private final TicketPriceCalculator priceCalculator;

    public TicketServiceImpl(TicketRepository ticketRepository,
                             UserRepository userRepository,
                             EventRepository eventRepository,
                             TicketMapper ticketMapper,
                             TicketPriceCalculator priceCalculator) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.ticketMapper = ticketMapper;
        this.priceCalculator = priceCalculator;
    }

    @Override
    @Transactional
    public TicketResponse purchase(PurchaseTicketRequest request) {
        // BR-TICKET-001: el usuario debe existir
        User user = userRepository.findByEmailIgnoreCase(request.userEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userEmail()));

        // BR-TICKET-002: el usuario debe estar activo
        if (!user.isActive()) {
            throw new BusinessRuleException("User is not active: " + user.getEmail());
        }

        // BR-TICKET-003: el evento debe existir
        Event event = eventRepository.findByEventCode(request.eventCode())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + request.eventCode()));

        // BR-TICKET-004: el evento debe estar PUBLISHED
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException(
                    "Tickets can only be purchased for PUBLISHED events. Current status: " + event.getStatus());
        }

        // BR-TICKET-005: el evento no puede haber ocurrido ya
        if (!event.getEventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Event has already taken place: " + event.getEventCode());
        }

        if (request.type() == null) {
            throw new BusinessRuleException("Ticket type is required.");
        }

        // BR-TICKET-006: edad minima, evaluada en la fecha del evento
        validateMinimumAge(user, event);

        // BR-TICKET-007: capacidad (paidTickets < venue.capacity)
        long paidTickets = ticketRepository
                .countTicketsByEventCodeAndStatus(event.getEventCode(), TicketStatus.PAID);
        int capacity = event.getVenue().getCapacity();
        if (paidTickets >= capacity) {
            throw new BusinessRuleException("Event has no capacity left: " + event.getEventCode());
        }

        // BR-TICKET-009: el precio lo calcula el sistema y nunca puede ser negativo
        BigDecimal price = priceCalculator.calculate(request.type());
        if (price.signum() < 0) {
            throw new BusinessRuleException("Ticket price cannot be negative.");
        }

        Ticket ticket = new Ticket(
                generateTicketCode(),
                request.type(),
                price,
                TicketStatus.PAID,
                LocalDateTime.now(),
                user,
                event
        );
        Ticket saved = ticketRepository.save(ticket);

        // BR-TICKET-008: si esta compra completa la capacidad, el evento pasa a SOLD_OUT
        if (paidTickets + 1 == capacity) {
            event.setStatus(EventStatus.SOLD_OUT);
            eventRepository.save(event);
        }

        return ticketMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse findByCode(String ticketCode) {
        return ticketMapper.toResponse(getTicket(ticketCode));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> findByUserEmail(String email) {
        return ticketRepository.findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(email)
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> findPaidTicketsByEvent(String eventCode) {
        return ticketRepository
                .findByEventEventCodeAndStatusOrderByPurchaseDateAsc(eventCode, TicketStatus.PAID)
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TicketResponse cancel(String ticketCode) {
        Ticket ticket = getTicket(ticketCode);

        // BR-TICKET-010 y 011: solo se cancela un ticket PAID (no USED ni CANCELLED)
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException(
                    "Only PAID tickets can be cancelled. Current status: " + ticket.getStatus());
        }

        // BR-TICKET-012: no se cancela despues de la fecha del evento
        if (!LocalDateTime.now().isBefore(ticket.getEvent().getEventDate())) {
            throw new BusinessRuleException("Ticket cannot be cancelled after the event date.");
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        Ticket saved = ticketRepository.save(ticket);
        return ticketMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TicketResponse markAsUsed(String ticketCode) {
        Ticket ticket = getTicket(ticketCode);

        // BR-TICKET-014: un ticket CANCELLED nunca se puede usar
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new BusinessRuleException("A CANCELLED ticket cannot be used: " + ticketCode);
        }

        // BR-TICKET-013: solo se marca como usado un ticket PAID
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException(
                    "Only PAID tickets can be marked as used. Current status: " + ticket.getStatus());
        }

        ticket.setStatus(TicketStatus.USED);
        Ticket saved = ticketRepository.save(ticket);
        return ticketMapper.toResponse(saved);
    }

    private void validateMinimumAge(User user, Event event) {
        Integer minimumAge = event.getMinimumAge();
        if (minimumAge == null || minimumAge <= 0) {
            return;
        }

        UserProfile profile = user.getProfile();
        if (profile == null || profile.getBirthDate() == null) {
            throw new BusinessRuleException("Cannot verify minimum age: user has no birth date.");
        }

        int ageAtEvent = Period.between(profile.getBirthDate(), event.getEventDate().toLocalDate()).getYears();
        if (ageAtEvent < minimumAge) {
            throw new BusinessRuleException("User does not meet minimum age.");
        }
    }

    private Ticket getTicket(String ticketCode) {
        return ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketCode));
    }

    private String generateTicketCode() {
        return "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}