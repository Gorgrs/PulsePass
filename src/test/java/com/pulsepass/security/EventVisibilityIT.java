package com.pulsepass.security;

import com.jayway.jsonpath.JsonPath;
import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserRole;
import com.pulsepass.domain.Venue;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Visibilidad de eventos (T-SEC-017): un evento publicado es publico; un
 * borrador solo lo ven STAFF y ADMIN. Para los demas responde 404, igual que si
 * el evento no existiera.
 *
 * <p>Escenario: un evento publicado y un borrador, ambos con el mismo artista.</p>
 */
@AutoConfigureMockMvc
class EventVisibilityIT extends AbstractPersistenceIT {

    private static final String PASSWORD = "ClaveDePrueba2026";
    private static final String PUBLISHED_CODE = "EVT-PUB";
    private static final String DRAFT_CODE = "EVT-DRAFT";

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
    private ArtistRepository artistRepository;

    private String stageName;
    private String userToken;
    private String staffToken;
    private String adminToken;

    @BeforeEach
    void setUpScenario() throws Exception {
        Venue venue = venueRepository.save(TestFixtures.venue("VEN-T1", "Santa Marta"));
        LocalDateTime date = LocalDateTime.now().plusMonths(3);
        Event published = TestFixtures.event(PUBLISHED_CODE, EventStatus.PUBLISHED, date, venue);
        Event draft = TestFixtures.event(DRAFT_CODE, EventStatus.DRAFT, date, venue);

        // Un artista que ya existe en la base de datos (migracion V2).
        Artist artist = artistRepository.findAll().stream().findFirst().orElseThrow();
        stageName = artist.getStageName();
        published.addArtist(artist);
        draft.addArtist(artist);
        eventRepository.save(published);
        eventRepository.save(draft);

        userToken = registerAndLogin("andrea");
        staffToken = tokenWithRole("laura", Role.STAFF);
        adminToken = tokenWithRole("admin1", Role.ADMIN);
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

    // ---------------------------------------------------------------
    // Evento por codigo
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Un anonimo puede ver un evento publicado por su codigo")
    void anonymousSeesAPublishedEvent() throws Exception {
        mockMvc.perform(get("/api/events/" + PUBLISHED_CODE)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("T-SEC-017: un anonimo no ve un borrador (404)")
    void anonymousCannotSeeADraft() throws Exception {
        mockMvc.perform(get("/api/events/" + DRAFT_CODE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Un USER autenticado tampoco ve un borrador (404)")
    void userCannotSeeADraft() throws Exception {
        mockMvc.perform(get("/api/events/" + DRAFT_CODE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("STAFF y ADMIN si ven un borrador")
    void staffAndAdminCanSeeADraft() throws Exception {
        mockMvc.perform(get("/api/events/" + DRAFT_CODE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(staffToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/events/" + DRAFT_CODE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Un borrador y un evento inexistente responden igual para el publico")
    void draftAndUnknownEventLookTheSame() throws Exception {
        mockMvc.perform(get("/api/events/" + DRAFT_CODE)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/events/EVT-NO-EXISTE")).andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // Busqueda por artista
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Por artista: un anonimo solo ve eventos publicados")
    void byArtistAnonymousSeesOnlyPublished() throws Exception {
        mockMvc.perform(get("/api/events/by-artist").param("stageName", stageName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventCode").value(PUBLISHED_CODE));
    }

    @Test
    @DisplayName("Por artista: un USER solo ve eventos publicados")
    void byArtistUserSeesOnlyPublished() throws Exception {
        mockMvc.perform(get("/api/events/by-artist").param("stageName", stageName)
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventCode").value(PUBLISHED_CODE));
    }

    @Test
    @DisplayName("Por artista: STAFF y ADMIN ven todos los eventos")
    void byArtistStaffAndAdminSeeEverything() throws Exception {
        mockMvc.perform(get("/api/events/by-artist").param("stageName", stageName)
                        .header(HttpHeaders.AUTHORIZATION, bearer(staffToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/api/events/by-artist").param("stageName", stageName)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ---------------------------------------------------------------
    // Lista de publicados
    // ---------------------------------------------------------------

    @Test
    @DisplayName("La lista publica de eventos no incluye borradores")
    void publishedListDoesNotIncludeDrafts() throws Exception {
        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.eventCode == '" + PUBLISHED_CODE + "')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.eventCode == '" + DRAFT_CODE + "')]").isEmpty());
    }
}