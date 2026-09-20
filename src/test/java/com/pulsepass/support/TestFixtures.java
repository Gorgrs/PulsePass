package com.pulsepass.support;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.domain.User;
import com.pulsepass.domain.Venue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Constructores de objetos de prueba, para que cada test exprese solo lo que
 * de verdad esta comprobando.
 */
public final class TestFixtures {

    private TestFixtures() {
    }

    public static Venue venue(String code, String city) {
        return new Venue(code, "Recinto " + code, city, "Calle 1 # 2-3", 5000);
    }

    public static Event event(String eventCode, EventStatus status, LocalDateTime date, Venue venue) {
        Event event = new Event(eventCode, "Evento " + eventCode, EventCategory.MUSIC, status, date, 18);
        venue.addEvent(event);
        return event;
    }

    public static User user(String username) {
        return new User(username, username + "@pulsepass.test");
    }

    public static Ticket ticket(String ticketCode,
                                TicketType type,
                                TicketStatus status,
                                String price,
                                User user,
                                Event event) {
        return new Ticket(
                ticketCode,
                type,
                new BigDecimal(price),
                status,
                LocalDateTime.of(2026, 1, 15, 10, 0),
                user,
                event
        );
    }
}
