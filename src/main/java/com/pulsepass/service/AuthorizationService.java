package com.pulsepass.service;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.User;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.security.SecurityPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Reglas de propiedad (anti-IDOR) y de visibilidad: compara a quien hace la
 * solicitud con el dueno del recurso, usando siempre el id persistente que sale
 * de la base de datos. Un correo o un codigo enviado por el cliente nunca basta
 * (SEC006).
 *
 * <p>Se usa desde {@code @PreAuthorize} y {@code @PostFilter} en los servicios.
 * Si un metodo devuelve false, Spring Security lanza AccessDeniedException y la
 * API responde 403.</p>
 *
 * <p>Politica acordada: un ticket o un usuario ajeno responde 403. Si el ticket
 * no existe, la regla deja pasar y el servicio responde 404 como antes. Un
 * evento no publicado se oculta con 404 para quien no es STAFF ni ADMIN.</p>
 */
@Service
@Transactional(readOnly = true)
public class AuthorizationService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_STAFF = "ROLE_STAFF";

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;

    public AuthorizationService(UserRepository userRepository,
                                TicketRepository ticketRepository,
                                EventRepository eventRepository) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
    }

    // ---------------------------------------------------------------
    // Usuarios
    // ---------------------------------------------------------------

    /** El correo pertenece a quien hace la solicitud (comparacion por id persistente). */
    public boolean isSelfByEmail(String email, Authentication authentication) {
        Long principalId = principalId(authentication);
        if (principalId == null || email == null) {
            return false;
        }
        return userRepository.findByEmailIgnoreCase(email.trim())
                .map(User::getId)
                .filter(principalId::equals)
                .isPresent();
    }

    /** El username pertenece a quien hace la solicitud. */
    public boolean isSelfByUsername(String username, Authentication authentication) {
        Long principalId = principalId(authentication);
        if (principalId == null || username == null) {
            return false;
        }
        return userRepository.findByUsername(username.trim())
                .map(User::getId)
                .filter(principalId::equals)
                .isPresent();
    }

    /** GET /api/users/by-email: el propio usuario o un ADMIN. */
    public boolean canViewUserByEmail(String email, Authentication authentication) {
        return hasRole(authentication, ROLE_ADMIN) || isSelfByEmail(email, authentication);
    }

    /** GET /api/users/by-username: el propio usuario o un ADMIN. */
    public boolean canViewUserByUsername(String username, Authentication authentication) {
        return hasRole(authentication, ROLE_ADMIN) || isSelfByUsername(username, authentication);
    }

    // ---------------------------------------------------------------
    // Tickets
    // ---------------------------------------------------------------

    /** GET /api/tickets/by-user: los tickets del propio usuario o un ADMIN. */
    public boolean canViewTicketsOfUser(String email, Authentication authentication) {
        return hasRole(authentication, ROLE_ADMIN) || isSelfByEmail(email, authentication);
    }

    /** GET /api/tickets/{code}: el dueno, o STAFF / ADMIN. */
    public boolean canViewTicket(String ticketCode, Authentication authentication) {
        if (isStaffOrAdmin(authentication)) {
            return true;
        }
        return isTicketOwner(ticketCode, authentication);
    }

    /** PATCH /api/tickets/{code}/cancel: el dueno o un ADMIN. */
    public boolean canCancelTicket(String ticketCode, Authentication authentication) {
        return hasRole(authentication, ROLE_ADMIN) || isTicketOwner(ticketCode, authentication);
    }

    private boolean isTicketOwner(String ticketCode, Authentication authentication) {
        Long principalId = principalId(authentication);
        if (principalId == null) {
            return false;
        }
        Optional<Ticket> ticket = ticketRepository.findByTicketCode(ticketCode);
        if (ticket.isEmpty()) {
            // No hay nada que proteger: el servicio respondera 404.
            return true;
        }
        return principalId.equals(ticket.get().getUser().getId());
    }

    // ---------------------------------------------------------------
    // Eventos
    // ---------------------------------------------------------------

    /** Un evento publicado lo ve cualquiera; los demas estados, solo STAFF y ADMIN. */
    public boolean canSeeEventStatus(EventStatus status, Authentication authentication) {
        return status == EventStatus.PUBLISHED || isStaffOrAdmin(authentication);
    }

    /**
     * GET /api/events/{code}: deja pasar si el evento esta publicado, si no
     * existe (el servicio respondera 404) o si quien pregunta es STAFF / ADMIN.
     * Si el evento existe pero no esta publicado, lanza 404 en vez de
     * responder 403, para no revelar que hay un borrador.
     */
    public boolean ensureEventVisible(String eventCode, Authentication authentication) {
        if (isStaffOrAdmin(authentication)) {
            return true;
        }
        Optional<Event> event = eventRepository.findByEventCode(eventCode);
        if (event.isEmpty() || event.get().getStatus() == EventStatus.PUBLISHED) {
            return true;
        }
        throw new ResourceNotFoundException("Event not found: " + eventCode);
    }

    // ---------------------------------------------------------------
    // Ayudas
    // ---------------------------------------------------------------

    private boolean isStaffOrAdmin(Authentication authentication) {
        return hasRole(authentication, ROLE_ADMIN) || hasRole(authentication, ROLE_STAFF);
    }

    private Long principalId(Authentication authentication) {
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof SecurityPrincipal principal) {
            return principal.getUserId();
        }
        return null;
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }
}