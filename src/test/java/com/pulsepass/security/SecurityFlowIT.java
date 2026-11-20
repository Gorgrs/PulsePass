package com.pulsepass.security;

import com.jayway.jsonpath.JsonPath;
import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserRole;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.support.AbstractPersistenceIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de seguridad de punta a punta: registro, login, JWT, roles y
 * propiedad de los datos, con la cadena de filtros real y PostgreSQL real
 * (Testcontainers). Cada prueba se revierte al terminar.
 *
 * <p>Las contrasenas y claves de estas pruebas son ficticias.</p>
 */
@AutoConfigureMockMvc
class SecurityFlowIT extends AbstractPersistenceIT {

    private static final String PASSWORD = "ClaveDePrueba2026";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserRoleRepository userRoleRepository;
    @Autowired
    private JwtProperties jwtProperties;

    // ---------------------------------------------------------------
    // Ayudas
    // ---------------------------------------------------------------

    private String registerJson(String username) {
        return """
                {"username":"%s","email":"%s@pulsepass.test","firstName":"Test","lastName":"User",\
                "birthDate":"2000-01-01","password":"%s"}
                """.formatted(username, username, PASSWORD);
    }

    private void register(String username) throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(username)))
                .andExpect(status().isCreated());
    }

    private ResultActions attemptLogin(String identifier, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"" + identifier + "\",\"password\":\"" + password + "\"}"));
    }

    private String tokenOf(String username) throws Exception {
        MvcResult result = attemptLogin(username, PASSWORD).andExpect(status().isOk()).andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.accessToken");
    }

    private String registerAndLogin(String username) throws Exception {
        register(username);
        return tokenOf(username);
    }

    /** Los roles privilegiados no se piden por HTTP: se asignan directo en la base de datos. */
    private void grantRole(String username, Role role) {
        User user = userRepository.findByUsername(username).orElseThrow();
        userRoleRepository.save(new UserRole(user, role));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private void assertPassedAuthorization(MvcResult result) {
        // Llego a la capa de negocio: no fue rechazado por seguridad (401 ni 403).
        assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
    }

    // ---------------------------------------------------------------
    // Rutas publicas y anonimo
    // ---------------------------------------------------------------

    @Test
    @DisplayName("T-SEC-001: un anonimo puede consultar las rutas publicas")
    void anonymousCanReadPublicRoutes() throws Exception {
        mockMvc.perform(get("/api/events/published")).andExpect(status().isOk());
        mockMvc.perform(get("/api/venues/active")).andExpect(status().isOk());
        mockMvc.perform(get("/api/artists/active")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("T-SEC-002: un anonimo recibe 401 con ErrorResponse en las rutas protegidas")
    void anonymousGets401OnProtectedRoutes() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/events/CMF-2026/publish")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/tickets/TKT-1/use")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/tickets/TKT-1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/tickets/by-user").param("email", "x@pulsepass.test"))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------
    // Registro y login
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Registro: responde 201 sin contrasena ni hash")
    void registerReturnsCreatedWithoutSecrets() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("andrea")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Registro: una contrasena corta responde 400")
    void registerWithShortPasswordReturns400() throws Exception {
        String json = registerJson("andrea").replace(PASSWORD, "corta");

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.password").isNotEmpty());
    }

    @Test
    @DisplayName("NFR-SEC009: el registro publico siempre crea solo el rol USER")
    void publicRegistrationCannotAskForPrivilegedRoles() throws Exception {
        String json = registerJson("intruso").replace("\"password\"",
                "\"roles\":[\"ADMIN\"],\"password\"");
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(tokenOf("intruso"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    @DisplayName("T-SEC-005: login correcto devuelve un token Bearer de 900 segundos")
    void loginReturnsBearerToken() throws Exception {
        register("andrea");

        attemptLogin("andrea", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("El login tambien funciona con el correo")
    void loginWorksWithEmail() throws Exception {
        register("andrea");

        attemptLogin("andrea@pulsepass.test", PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("T-SEC-006: contrasena incorrecta o usuario inexistente dan el mismo 401")
    void badCredentialsGiveTheSameGeneric401() throws Exception {
        register("andrea");

        MvcResult wrongPassword = attemptLogin("andrea", "OtraClaveMuyLarga99")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andReturn();
        MvcResult unknownUser = attemptLogin("nadie", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andReturn();

        String messageOne = JsonPath.read(
                wrongPassword.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.message");
        String messageTwo = JsonPath.read(
                unknownUser.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.message");
        assertThat(messageOne).isEqualTo(messageTwo);
    }

    @Test
    @DisplayName("GET /api/auth/me devuelve usuario y roles, sin secretos")
    void meReturnsMinimalPrincipal() throws Exception {
        String token = registerAndLogin("andrea");

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    // ---------------------------------------------------------------
    // Tokens invalidos
    // ---------------------------------------------------------------

    @Test
    @DisplayName("T-SEC-008: un token con la firma alterada da 401")
    void tamperedTokenGets401() throws Exception {
        String[] parts = registerAndLogin("andrea").split("\\.");
        char first = parts[2].charAt(0);
        parts[2] = (first == 'A' ? 'B' : 'A') + parts[2].substring(1);

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(String.join(".", parts))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("T-SEC-007: un token vencido da 401 con ErrorResponse")
    void expiredTokenGets401() throws Exception {
        register("andrea");
        Long userId = userRepository.findByUsername("andrea").orElseThrow().getId();
        JwtService pastService = new JwtService(
                jwtProperties, Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC));

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(pastService.generateToken(userId))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Un token firmado con otra clave da 401")
    void tokenSignedWithAnotherKeyGets401() throws Exception {
        register("andrea");
        Long userId = userRepository.findByUsername("andrea").orElseThrow().getId();
        String otherSecret = Base64.getEncoder()
                .encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
        JwtService otherService = new JwtService(new JwtProperties(
                jwtProperties.issuer(), otherSecret, jwtProperties.accessTokenTtl()));

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherService.generateToken(userId))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un Bearer basura o un esquema distinto da 401, nunca 500")
    void garbageAuthorizationHeaderGets401() throws Exception {
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer "))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("FR-SECAUTH-007: el token en la URL no se acepta")
    void tokenInQueryStringIsIgnored() throws Exception {
        String token = registerAndLogin("andrea");

        mockMvc.perform(get("/api/auth/me").param("access_token", token))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------
    // Roles
    // ---------------------------------------------------------------

    @Test
    @DisplayName("T-SEC-003 y T-SEC-012: un USER recibe 403 en las rutas de ADMIN y STAFF")
    void userGets403OnPrivilegedRoutes() throws Exception {
        String token = registerAndLogin("andrea");

        mockMvc.perform(post("/api/events").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
        mockMvc.perform(patch("/api/events/CMF-2026/publish")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/events/CMF-2026/artists/1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/tickets/TKT-1/use")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/events/CMF-2026/tickets/paid")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("T-SEC-014: un ADMIN supera la seguridad en publicar y asociar artistas")
    void adminPassesAuthorizationOnAdminRoutes() throws Exception {
        register("admin1");
        grantRole("admin1", Role.ADMIN);
        String token = tokenOf("admin1");

        assertPassedAuthorization(mockMvc.perform(patch("/api/events/NO-EXISTE/publish")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))).andReturn());
        assertPassedAuthorization(mockMvc.perform(post("/api/events/NO-EXISTE/artists/1")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))).andReturn());
        assertPassedAuthorization(mockMvc.perform(get("/api/events/NO-EXISTE/tickets/paid")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))).andReturn());
    }

    @Test
    @DisplayName("T-SEC-013: STAFF puede marcar tickets como usados pero no publicar eventos")
    void staffCanUseTicketsButNotPublish() throws Exception {
        register("laura");
        grantRole("laura", Role.STAFF);
        String token = tokenOf("laura");

        assertPassedAuthorization(mockMvc.perform(patch("/api/tickets/NO-EXISTE/use")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))).andReturn());
        mockMvc.perform(patch("/api/events/NO-EXISTE/publish")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------
    // Propiedad de los datos (anti-IDOR)
    // ---------------------------------------------------------------

    @Test
    @DisplayName("T-SEC-015: un USER solo consulta sus propios datos de usuario")
    void userCanOnlyReadOwnUserData() throws Exception {
        String andreaToken = registerAndLogin("andrea");
        register("carlos");

        mockMvc.perform(get("/api/users/by-email").param("email", "andrea@pulsepass.test")
                        .header(HttpHeaders.AUTHORIZATION, bearer(andreaToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("andrea"));
        mockMvc.perform(get("/api/users/by-username").param("username", "andrea")
                        .header(HttpHeaders.AUTHORIZATION, bearer(andreaToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/by-email").param("email", "carlos@pulsepass.test")
                        .header(HttpHeaders.AUTHORIZATION, bearer(andreaToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/by-username").param("username", "carlos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(andreaToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un ADMIN puede consultar los datos de otro usuario")
    void adminCanReadOtherUsersData() throws Exception {
        register("carlos");
        register("admin1");
        grantRole("admin1", Role.ADMIN);
        String adminToken = tokenOf("admin1");

        mockMvc.perform(get("/api/users/by-email").param("email", "carlos@pulsepass.test")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("carlos"));
    }

    @Test
    @DisplayName("T-SEC-009: un USER no ve la lista de tickets de otro usuario")
    void userCannotListTicketsOfAnotherUser() throws Exception {
        String andreaToken = registerAndLogin("andrea");
        register("carlos");

        mockMvc.perform(get("/api/tickets/by-user").param("email", "carlos@pulsepass.test")
                        .header(HttpHeaders.AUTHORIZATION, bearer(andreaToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/tickets/by-user").param("email", "andrea@pulsepass.test")
                        .header(HttpHeaders.AUTHORIZATION, bearer(andreaToken)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Un ADMIN puede ver la lista de tickets de otro usuario")
    void adminCanListTicketsOfAnotherUser() throws Exception {
        register("carlos");
        register("admin1");
        grantRole("admin1", Role.ADMIN);

        mockMvc.perform(get("/api/tickets/by-user").param("email", "carlos@pulsepass.test")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOf("admin1"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Politica: un ticket que no existe responde 404, no 403")
    void unknownTicketReturns404() throws Exception {
        String token = registerAndLogin("andrea");

        mockMvc.perform(get("/api/tickets/NO-EXISTE").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // Usuario desactivado
    // ---------------------------------------------------------------

    @Test
    @DisplayName("T-SEC-016: un token valido de un usuario desactivado da 401")
    void inactiveUserLosesAccessWithAValidToken() throws Exception {
        String token = registerAndLogin("inactiva");
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        User user = userRepository.findByUsername("inactiva").orElseThrow();
        user.setActive(false);
        userRepository.saveAndFlush(user);

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
        attemptLogin("inactiva", PASSWORD).andExpect(status().isUnauthorized());
    }
}