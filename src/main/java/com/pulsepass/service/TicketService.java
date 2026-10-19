package com.pulsepass.service;

import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

public interface TicketService {

    /** El correo de la compra debe pertenecer a quien esta autenticado. */
    @PreAuthorize("@authorizationService.isSelfByEmail(#request.userEmail(), authentication)")
    TicketResponse purchase(PurchaseTicketRequest request);

    /** Dueno del ticket, o STAFF / ADMIN. */
    @PreAuthorize("@authorizationService.canViewTicket(#ticketCode, authentication)")
    TicketResponse findByCode(String ticketCode);

    /** Los tickets del propio usuario, o un ADMIN. */
    @PreAuthorize("@authorizationService.canViewTicketsOfUser(#email, authentication)")
    List<TicketResponse> findByUserEmail(String email);

    // El rol STAFF / ADMIN se exige en SecurityConfig.
    List<TicketResponse> findPaidTicketsByEvent(String eventCode);

    /** Dueno del ticket, o un ADMIN. Se comprueba antes de cambiar el estado. */
    @PreAuthorize("@authorizationService.canCancelTicket(#ticketCode, authentication)")
    TicketResponse cancel(String ticketCode);

    // El rol STAFF / ADMIN se exige en SecurityConfig.
    TicketResponse markAsUsed(String ticketCode);
}