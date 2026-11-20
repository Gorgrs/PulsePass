package com.pulsepass.service;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Role;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.domain.User;
import com.pulsepass.domain.Venue;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link AuthorizationService} (sin Spring ni base de
 * datos). Andrea tiene el id 1 y Carlos el id 2.
 */
@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    private static final long ANDREA_ID = 1L;
    private static final long CARLOS_ID = 2L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private AuthorizationService authorization;

    // ---------- ayudas ----------

    private static Authentication authenticated(long userId, Role... roles) {
        SecurityPrincipal principal = new SecurityPrincipal(
                userId, "usuario" + userId, "hash-ficticio", true, List.of(roles));
        return UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());
    }

    private static Authentication andrea() {
        return authenticated(ANDREA_ID, Role.USER);
    }

    private static Authentication staff() {
        return authenticated(10L, Role.USER, Role.STAFF);
    }

    private static Authentication admin() {
        return authenticated(11L, Role.USER, Role.ADMIN);
    }

    private static Authentication anonymous() {
        return new AnonymousAuthenticationToken(
                "clave", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
    }

    private static User userWithId(long id, String username) {
        User user = new User(username, username + "@email.com");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static Event event(String code, EventStatus status) {
        Venue venue = TestFixtures.venue("VEN-1", "Santa Marta");
        return TestFixtures.event(code, status, LocalDateTime.now().plusDays(10), venue);
    }

    private static Ticket ticketOf(User owner) {
        return TestFixtures.ticket("TKT-1", TicketType.GENERAL, TicketStatus.PAID, "100000.00",
                owner, event("EVT-1", EventStatus.PUBLISHED));
    }

    // ---------------------------------------------------------------
    // Correos y usernames: rechazo de identidades ajenas
    // ---------------------------------------------------------------

    @Test
    @DisplayName("isSelfByEmail: el correo propio es aceptado")
    void ownEmailIsAccepted() {
        when(userRepository.findByEmailIgnoreCase("andrea@email.com"))
                .thenReturn(Optional.of(userWithId(ANDREA_ID, "andrea")));

        assertThat(authorization.isSelfByEmail("andrea@email.com", andrea())).isTrue();
    }

    @Test
    @DisplayName("isSelfByEmail: el correo de otro usuario es rechazado")
    void someoneElsesEmailIsRejected() {
        when(userRepository.findByEmailIgnoreCase("carlos@email.com"))
                .thenReturn(Optional.of(userWithId(CARLOS_ID, "carlos")));

        assertThat(authorization.isSelfByEmail("carlos@email.com", andrea())).isFalse();
    }

    @Test
    @DisplayName("isSelfByEmail: un correo que no existe es rechazado")
    void unknownEmailIsRejected() {
        assertThat(authorization.isSelfByEmail("nadie@email.com", andrea())).isFalse();
    }

    @Test
    @DisplayName("isSelfByEmail: se ignoran los espacios alrededor del correo")
    void emailIsTrimmed() {
        when(userRepository.findByEmailIgnoreCase("andrea@email.com"))
                .thenReturn(Optional.of(userWithId(ANDREA_ID, "andrea")));

        assertThat(authorization.isSelfByEmail("  andrea@email.com  ", andrea())).isTrue();
    }

    @Test
    @DisplayName("isSelfByEmail: un anonimo o un correo nulo nunca son aceptados")
    void anonymousOrNullEmailIsRejected() {
        assertThat(authorization.isSelfByEmail("andrea@email.com", anonymous())).isFalse();
        assertThat(authorization.isSelfByEmail(null, andrea())).isFalse();
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("isSelfByUsername: acepta el propio username y rechaza el ajeno")
    void usernameOwnershipIsChecked() {
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(userWithId(ANDREA_ID, "andrea")));
        when(userRepository.findByUsername("carlos")).thenReturn(Optional.of(userWithId(CARLOS_ID, "carlos")));

        assertThat(authorization.isSelfByUsername("andrea", andrea())).isTrue();
        assertThat(authorization.isSelfByUsername("carlos", andrea())).isFalse();
    }

    @Test
    @DisplayName("Datos de usuario: un ADMIN puede ver los de otros sin consultar la base de datos")
    void adminCanViewAnyUser() {
        assertThat(authorization.canViewUserByEmail("carlos@email.com", admin())).isTrue();
        assertThat(authorization.canViewUserByUsername("carlos", admin())).isTrue();
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Datos de usuario: un USER no puede ver los de otro")
    void userCannotViewAnotherUser() {
        when(userRepository.findByEmailIgnoreCase("carlos@email.com"))
                .thenReturn(Optional.of(userWithId(CARLOS_ID, "carlos")));
        when(userRepository.findByUsername("carlos"))
                .thenReturn(Optional.of(userWithId(CARLOS_ID, "carlos")));

        assertThat(authorization.canViewUserByEmail("carlos@email.com", andrea())).isFalse();
        assertThat(authorization.canViewUserByUsername("carlos", andrea())).isFalse();
    }

    // ---------------------------------------------------------------
    // Tickets: dueno contra tercero
    // ---------------------------------------------------------------

    @Test
    @DisplayName("canViewTicket: el dueno si, un tercero no")
    void ticketOwnerVersusThirdParty() {
        when(ticketRepository.findByTicketCode("TKT-1"))
                .thenReturn(Optional.of(ticketOf(userWithId(CARLOS_ID, "carlos"))));

        assertThat(authorization.canViewTicket("TKT-1", authenticated(CARLOS_ID, Role.USER))).isTrue();
        assertThat(authorization.canViewTicket("TKT-1", andrea())).isFalse();
    }

    @Test
    @DisplayName("canViewTicket: STAFF y ADMIN pueden verlo sin consultar el ticket")
    void staffAndAdminCanViewAnyTicket() {
        assertThat(authorization.canViewTicket("TKT-1", staff())).isTrue();
        assertThat(authorization.canViewTicket("TKT-1", admin())).isTrue();
        verifyNoInteractions(ticketRepository);
    }

    @Test
    @DisplayName("canViewTicket: un anonimo nunca puede verlo")
    void anonymousCannotViewTicket() {
        assertThat(authorization.canViewTicket("TKT-1", anonymous())).isFalse();
    }

    @Test
    @DisplayName("canViewTicket: un ticket que no existe deja pasar (el servicio responde 404)")
    void unknownTicketLetsTheServiceAnswer404() {
        assertThat(authorization.canViewTicket("NO-EXISTE", andrea())).isTrue();
    }

    @Test
    @DisplayName("canCancelTicket: el dueno y un ADMIN si; un tercero y un STAFF no")
    void cancelIsForOwnerAndAdminOnly() {
        when(ticketRepository.findByTicketCode("TKT-1"))
                .thenReturn(Optional.of(ticketOf(userWithId(CARLOS_ID, "carlos"))));

        assertThat(authorization.canCancelTicket("TKT-1", authenticated(CARLOS_ID, Role.USER))).isTrue();
        assertThat(authorization.canCancelTicket("TKT-1", admin())).isTrue();
        assertThat(authorization.canCancelTicket("TKT-1", andrea())).isFalse();
        assertThat(authorization.canCancelTicket("TKT-1", staff())).isFalse();
    }

    @Test
    @DisplayName("canViewTicketsOfUser: el propio usuario y un ADMIN si; otro USER no")
    void ticketListIsForTheUserAndAdmin() {
        when(userRepository.findByEmailIgnoreCase("andrea@email.com"))
                .thenReturn(Optional.of(userWithId(ANDREA_ID, "andrea")));
        when(userRepository.findByEmailIgnoreCase("carlos@email.com"))
                .thenReturn(Optional.of(userWithId(CARLOS_ID, "carlos")));

        assertThat(authorization.canViewTicketsOfUser("andrea@email.com", andrea())).isTrue();
        assertThat(authorization.canViewTicketsOfUser("carlos@email.com", andrea())).isFalse();
        assertThat(authorization.canViewTicketsOfUser("carlos@email.com", admin())).isTrue();
    }

    // ---------------------------------------------------------------
    // Eventos: publicados contra borradores
    // ---------------------------------------------------------------

    @Test
    @DisplayName("canSeeEventStatus: un evento publicado lo ve cualquiera")
    void publishedEventIsPublic() {
        assertThat(authorization.canSeeEventStatus(EventStatus.PUBLISHED, anonymous())).isTrue();
        assertThat(authorization.canSeeEventStatus(EventStatus.PUBLISHED, andrea())).isTrue();
    }

    @Test
    @DisplayName("canSeeEventStatus: los demas estados solo los ven STAFF y ADMIN")
    void otherStatusesAreForStaffAndAdminOnly() {
        for (EventStatus status : List.of(EventStatus.DRAFT, EventStatus.CANCELLED, EventStatus.FINISHED)) {
            assertThat(authorization.canSeeEventStatus(status, anonymous())).isFalse();
            assertThat(authorization.canSeeEventStatus(status, andrea())).isFalse();
            assertThat(authorization.canSeeEventStatus(status, staff())).isTrue();
            assertThat(authorization.canSeeEventStatus(status, admin())).isTrue();
        }
    }

    @Test
    @DisplayName("ensureEventVisible: un evento publicado deja pasar a cualquiera")
    void publishedEventIsVisible() {
        when(eventRepository.findByEventCode("EVT-1"))
                .thenReturn(Optional.of(event("EVT-1", EventStatus.PUBLISHED)));

        assertThat(authorization.ensureEventVisible("EVT-1", anonymous())).isTrue();
    }

    @Test
    @DisplayName("ensureEventVisible: un borrador responde 404 al anonimo y al USER")
    void draftIsHiddenWithNotFound() {
        when(eventRepository.findByEventCode("EVT-D"))
                .thenReturn(Optional.of(event("EVT-D", EventStatus.DRAFT)));

        assertThatThrownBy(() -> authorization.ensureEventVisible("EVT-D", anonymous()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> authorization.ensureEventVisible("EVT-D", andrea()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("ensureEventVisible: STAFF y ADMIN ven el borrador sin consultar el evento")
    void staffAndAdminSeeDrafts() {
        assertThat(authorization.ensureEventVisible("EVT-D", staff())).isTrue();
        assertThat(authorization.ensureEventVisible("EVT-D", admin())).isTrue();
        verifyNoInteractions(eventRepository);
    }

    @Test
    @DisplayName("ensureEventVisible: un evento que no existe deja pasar (el servicio responde 404)")
    void unknownEventLetsTheServiceAnswer404() {
        assertThat(authorization.ensureEventVisible("NO-EXISTE", andrea())).isTrue();
    }
}