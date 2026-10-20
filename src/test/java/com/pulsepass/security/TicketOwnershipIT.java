package com.pulsepass.security;

import com.jayway.jsonpath.JsonPath;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Role;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserRole;
import com.pulsepass.domain.Venue;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.support.AbstractPersistenceIT;
import com.pulsepass.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Propiedad de los tickets (anti-IDOR) con la cadena de seguridad real y
 * PostgreSQL real. Escenario: Andrea y Carlos son usuarios; Carlos tiene un
 * ticket pagado de un evento publicado. Andrea no debe poder tocarlo.
 *
 * <p>Cada prueba se revierte al terminar. Las contrasenas son ficticias.</p>
 */
@AutoConfigureMockMvc
class TicketOwnershipIT extends AbstractPersistenceIT {

    private static final String PASSWORD = "ClaveDePrueba2026";
    private static final String EVENT_CODE = "EVT-PUB";
    private static final String CARLOS_TICKET = "TKT-CARLOS-1";
    private static final String ANDREA_EMAIL = "andrea@pulsepass.test";
    private static final String CARLOS_EMAIL = "carlos@pulsepass.test";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserRoleRepository userRoleRepository;
    @Autowired
    private VenueRepository venueRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private TicketRepository ticketRepository;

    private String andreaToken;
    private String carlosToken;

    @BeforeEach
    void setUpScenario() throws Exception {
        Venue venue = venueRepository.save(TestFixtures.venue("VEN-T1", "Santa Marta"));
        Event event = TestFixtures.event(EVENT_CODE, EventStatus.PUBLISHED,
                LocalDateTime.now().plusMonths(3), venue);
        eventRepository.save(event);

        andreaToken = registerAndLogin("andrea");
        carlosToken = registerAndLogin("carlos");

        User carlos = userRepository.findByUsername("carlos").orElseThrow();
        Event savedEvent = eventRepository.findAll().stream()
                .filter(e -> EVENT_CODE.equals(e.getEventCode()))
                .findFirst()
                .orElseThrow();
        ticketRepository.save(TestFixtures.ticket(
                CARLOS_TICKET, TicketType.GENERAL, TicketStatus.PAID, "100000.00", carlos, savedEvent));
    }

    // ---------------------------------------------------------------
    // Ayudas
    // ---------------------------------------------------------------

    private String registerAndLogin(String username) throws Exception {
        String registerJson = """
                {"username":"%s","email":"%s@pulsepass.test","firstName":"Test","lastName":"User",\
                "birthDate":"2000-01-01","password":"%s"}
                """.formatted(username, username, PASSWORD);
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(registerJson))
                .andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String body = login.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.accessToken");
    }

    /** Los roles privilegiados se asignan directo en la base de datos, nunca por HTTP. */
    private String tokenWithRole(String username, Role role) throws Exception {
        String token = registerAndLogin(username);
        User user = userRepository.findByUsername(username).orElseThrow();
        userRoleRepository.save(new UserRole(user, role));
        return token;
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private String purchaseJson(String email) {
        return """
                {"userEmail":"%s","eventCode":"%s","type":"GENERAL"}
                """.formatted(email, EVENT_CODE);
    }

    private void assertPassedAuthorization(MvcResult result) {
        // Llego a la capa de negocio: no fue rechazado por seguridad (401 ni 403).
        assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
    }

    // ---------------------------------------------------------------
    // Compra
    // ---------------------------------------------------------------

    @Test
    @DisplayName("UC-SEC-04: Andrea compra un ticket a su propio nombre")
    void userPurchasesForHerself() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .header(HttpHeaders.AUTHORIZATION, bearer(andreaToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseJson(ANDREA_EMAIL)))
                .andExpect(status().isCreated());

        assertThat(ticketRepository.findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(ANDREA_EMAIL))
                .hasSize(1);
    }

    @Test
    @DisplayName("UC-SEC-05 y T-SEC-010: Andrea no puede comprar a nombre de Carlos")
    void userCannotPurchaseForSomeoneElse() throws Exception {
        long ticketsBefore = ticketRepository.count();

        mockMvc.perform(post("/api/tickets")
                        .header(HttpHeaders.AUTHORIZATION, bearer(andreaToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseJson(CARLOS_EMAIL)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        assertThat(ticketRepository.count()).isEqualTo(ticketsBefore);
    }

    @Test
    @DisplayName("Un anonimo no puede comprar tickets")
    void anonymousCannotPurchase() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseJson(ANDREA_EMAIL)))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------
    // Consulta
    // ---------------------------------------------------------------

    @Test
    @DisplayName("UC-SEC-06: Andrea no puede ver el ticket de Carlos, aunque conozca su codigo")
    void userCannotViewSomeoneElsesTicket() throws Exception {
        mockMvc.perform(get("/api/tickets/" + CARLOS_TICKET)
                        .header(HttpHeaders.AUTHORIZATION, bearer(andreaToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("El dueno, un STAFF y un ADMIN si pueden ver el ticket de Carlos")
    void ownerStaffAndAdminCanViewTheTicket() throws Exception {
        String staffToken = tokenWithRole("laura", Role.STAFF);
        String adminToken = tokenWithRole("admin1", Role.ADMIN);

        for (String token : new String[]{carlosToken, staffToken, adminToken}) {
            mockMvc.perform(get("/api/tickets/" + CARLOS_TICKET)
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("La lista de tickets de Carlos: la ve el, un ADMIN, pero no Andrea")
    void ticketListOfAUserIsProtected() throws Exception {
        String adminToken = tokenWithRole("admin1", Role.ADMIN);

        mockMvc.perform(get("/api/tickets/by-user").param("email", CARLOS_EMAIL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(andreaToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/tickets/by-user").param("email", CARLOS_EMAIL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(carlosToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/tickets/by-user").param("email", CARLOS_EMAIL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ---------------------------------------------------------------
    // Cancelacion
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Andrea no puede cancelar el ticket de Carlos y el ticket queda intacto")
    void userCannotCancelSomeoneElsesTicket() throws Exception {
        mockMvc.perform(patch("/api/tickets/" + CARLOS_TICKET + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, bearer(andreaToken)))
                .andExpect(status().isForbidden());

        Ticket ticket = ticketRepository.findByTicketCode(CARLOS_TICKET).orElseThrow();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.PAID);
    }

    @Test
    @DisplayName("T-SEC-011: el dueno y un ADMIN superan la seguridad al cancelar")
    void ownerAndAdminPassAuthorizationOnCancel() throws Exception {
        String adminToken = tokenWithRole("admin1", Role.ADMIN);

        assertPassedAuthorization(mockMvc.perform(patch("/api/tickets/" + CARLOS_TICKET + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))).andReturn());
        assertPassedAuthorization(mockMvc.perform(patch("/api/tickets/" + CARLOS_TICKET + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(carlosToken))).andReturn());
    }

    // ---------------------------------------------------------------
    // Uso en la entrada (STAFF / ADMIN)
    // ---------------------------------------------------------------

    @Test
    @DisplayName("T-SEC-012: ningun USER puede marcar un ticket como usado, ni siquiera el suyo")
    void usersCannotMarkTicketsAsUsed() throws Exception {
        mockMvc.perform(patch("/api/tickets/" + CARLOS_TICKET + "/use")
                        .header(HttpHeaders.AUTHORIZATION, bearer(andreaToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/tickets/" + CARLOS_TICKET + "/use")
                        .header(HttpHeaders.AUTHORIZATION, bearer(carlosToken)))
                .andExpect(status().isForbidden());

        Ticket ticket = ticketRepository.findByTicketCode(CARLOS_TICKET).orElseThrow();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.PAID);
    }

    @Test
    @DisplayName("T-SEC-013: un STAFF supera la seguridad al marcar un ticket como usado")
    void staffPassesAuthorizationOnUse() throws Exception {
        String staffToken = tokenWithRole("laura", Role.STAFF);

        assertPassedAuthorization(mockMvc.perform(patch("/api/tickets/" + CARLOS_TICKET + "/use")
                .header(HttpHeaders.AUTHORIZATION, bearer(staffToken))).andReturn());
    }

    @Test
    @DisplayName("Los tickets pagados de un evento solo los ve STAFF o ADMIN")
    void paidTicketsOfAnEventAreForStaffOnly() throws Exception {
        String staffToken = tokenWithRole("laura", Role.STAFF);

        mockMvc.perform(get("/api/events/" + EVENT_CODE + "/tickets/paid")
                        .header(HttpHeaders.AUTHORIZATION, bearer(andreaToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/events/" + EVENT_CODE + "/tickets/paid")
                        .header(HttpHeaders.AUTHORIZATION, bearer(staffToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}