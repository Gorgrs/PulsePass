package com.pulsepass.service;

import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.User;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.security.SecurityPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Reglas de propiedad (anti-IDOR): compara a quien hace la solicitud con el
 * dueno del recurso, usando siempre el id persistente que sale de la base de
 * datos. Un correo o un codigo enviado por el cliente nunca basta (SEC006).
 *
 * <p>Se usa desde {@code @PreAuthorize} en {@link TicketService} y
 * {@link UserService}. Si un metodo devuelve false, Spring Security lanza
 * AccessDeniedException y la API responde 403.</p>
 *
 * <p>Politica acordada: un recurso ajeno responde 403. Si el ticket no existe,
 * la regla deja pasar y el servicio responde 404 como antes.</p>
 */
@Service
@Transactional(readOnly = true)
public class AuthorizationService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_STAFF = "ROLE_STAFF";

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    public AuthorizationService(UserRepository userRepository, TicketRepository ticketRepository) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
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
        if (hasRole(authentication, ROLE_ADMIN) || hasRole(authentication, ROLE_STAFF)) {
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
    // Ayudas
    // ---------------------------------------------------------------

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