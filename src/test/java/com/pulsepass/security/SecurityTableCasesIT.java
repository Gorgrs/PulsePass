package com.pulsepass.security;

import com.jayway.jsonpath.JsonPath;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserRole;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.support.AbstractPersistenceIT;
import com.pulsepass.support.TestFixtures;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tres casos de la tabla T-SEC del PRD (seccion 17.2) que no estaban probados
 * con su descripcion exacta: T-SEC-004, T-SEC-018 y T-SEC-019.
 * Las contrasenas son ficticias.
 */
@AutoConfigureMockMvc
class SecurityTableCasesIT extends AbstractPersistenceIT {

    private static final String PASSWORD = "ClaveDePrueba2026";

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

    // ---------- ayudas ----------

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

    private String adminToken() throws Exception {
        String token = registerAndLogin("admin1");
        User user = userRepository.findByUsername("admin1").orElseThrow();
        userRoleRepository.save(new UserRole(user, Role.ADMIN));
        return token;
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    /** Una respuesta de error no debe traer la contrasena, un hash ni una traza tecnica. */
    private static void assertNoSensitiveData(String body) {
        assertThat(body)
                .doesNotContain(PASSWORD)
                .doesNotContainIgnoringCase("password")
                .doesNotContainIgnoringCase("hash")
                .doesNotContain("$2a$", "$2b$")
                .doesNotContain("Exception")
                .doesNotContain("stackTrace")
                .doesNotContain("at com.pulsepass");
    }

    // ---------- T-SEC-004 ----------

    @Test
    @DisplayName("T-SEC-004: un ADMIN crea un evento valido y recibe 201")
    void adminCreatesAValidEvent() throws Exception {
        venueRepository.save(TestFixtures.venue("VEN-T1", "Santa Marta"));
        String token = adminToken();
        String eventJson = """
                {"eventCode":"EVT-ADMIN-1","name":"Festival de prueba","category":"MUSIC",\
                "description":"Evento creado por un ADMIN","eventDate":"%s",\
                "venueCode":"VEN-T1","minimumAge":18}
                """.formatted(LocalDateTime.now().plusMonths(6).withNano(0));

        mockMvc.perform(post("/api/events")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson))
                .andExpect(status().isCreated());

        Event created = eventRepository.findByEventCode("EVT-ADMIN-1").orElseThrow();
        assertThat(created.getStatus()).isEqualTo(EventStatus.DRAFT);
    }

    // ---------- T-SEC-018 ----------

    @Test
    @DisplayName("T-SEC-018: una solicitud invalida de un usuario autorizado da 400, no 403")
    void authorizedUserWithAnInvalidRequestGets400() throws Exception {
        String token = registerAndLogin("andrea");
        String invalidPurchase = "{\"userEmail\":\"esto-no-es-un-correo\",\"eventCode\":\"EVT-1\",\"type\":\"GENERAL\"}";

        mockMvc.perform(post("/api/tickets")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPurchase))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.userEmail").value("User email must be valid"));
    }

    // ---------- T-SEC-019 ----------

    @Test
    @DisplayName("T-SEC-019: un login fallido no devuelve traza, contrasena ni hash")
    void failedLoginLeaksNothing() throws Exception {
        registerAndLogin("andrea");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"andrea\",\"password\":\"OtraClaveMuyLarga99\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertNoSensitiveData(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("T-SEC-019: un JWT invalido no devuelve traza, contrasena ni hash")
    void invalidJwtLeaksNothing() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertNoSensitiveData(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}