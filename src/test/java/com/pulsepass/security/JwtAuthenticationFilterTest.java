package com.pulsepass.security;

import com.pulsepass.domain.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link JwtAuthenticationFilter} (sin Spring ni base de
 * datos), con solicitudes simuladas. Los tokens son textos ficticios: el
 * {@link JwtService} esta simulado.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final long USER_ID = 42L;

    @Mock
    private JwtService jwtService;
    @Mock
    private CustomUserDetailsService userDetailsService;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        request = new MockHttpServletRequest("GET", "/api/auth/me");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ---------- ayudas ----------

    private SecurityPrincipal principal(boolean active) {
        return new SecurityPrincipal(USER_ID, "andrea", "hash-ficticio", active, List.of(Role.USER));
    }

    private void assertChainContinued() {
        // MockFilterChain guarda la solicitud solo si el filtro dejo continuar.
        assertThat(chain.getRequest()).isNotNull();
    }

    private Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    // ---------- sin encabezado ----------

    @Test
    @DisplayName("Sin Authorization: no autentica y deja continuar la solicitud")
    void noHeader_continuesWithoutAuthentication() throws Exception {
        filter.doFilter(request, response, chain);

        assertThat(currentAuthentication()).isNull();
        assertChainContinued();
        verifyNoInteractions(jwtService, userDetailsService);
    }

    // ---------- Bearer valido ----------

    @Test
    @DisplayName("Bearer valido: autentica con el principal y sus roles")
    void validBearer_authenticatesWithPrincipalAndRoles() throws Exception {
        request.addHeader("Authorization", "Bearer token-ok");
        when(jwtService.extractUserId("token-ok")).thenReturn(Optional.of(USER_ID));
        when(userDetailsService.loadUserById(USER_ID)).thenReturn(principal(true));

        filter.doFilter(request, response, chain);

        Authentication authentication = currentAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(((SecurityPrincipal) authentication.getPrincipal()).getUserId()).isEqualTo(USER_ID);
        assertThat(authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority))
                .containsExactly("ROLE_USER");
        assertChainContinued();
    }

    @Test
    @DisplayName("El esquema Bearer se acepta sin importar mayusculas")
    void bearerSchemeIsCaseInsensitive() throws Exception {
        request.addHeader("Authorization", "bearer token-ok");
        when(jwtService.extractUserId("token-ok")).thenReturn(Optional.of(USER_ID));
        when(userDetailsService.loadUserById(USER_ID)).thenReturn(principal(true));

        filter.doFilter(request, response, chain);

        assertThat(currentAuthentication()).isNotNull();
    }

    // ---------- Bearer invalido ----------

    @Test
    @DisplayName("Bearer invalido: no autentica, no consulta usuarios y deja continuar")
    void invalidBearer_doesNotAuthenticate() throws Exception {
        request.addHeader("Authorization", "Bearer token-malo");
        when(jwtService.extractUserId("token-malo")).thenReturn(Optional.empty());

        filter.doFilter(request, response, chain);

        assertThat(currentAuthentication()).isNull();
        assertChainContinued();
        verify(userDetailsService, never()).loadUserById(any());
    }

    @Test
    @DisplayName("Token valido de un usuario inactivo: no autentica")
    void inactiveUser_doesNotAuthenticate() throws Exception {
        request.addHeader("Authorization", "Bearer token-ok");
        when(jwtService.extractUserId("token-ok")).thenReturn(Optional.of(USER_ID));
        when(userDetailsService.loadUserById(USER_ID)).thenReturn(principal(false));

        filter.doFilter(request, response, chain);

        assertThat(currentAuthentication()).isNull();
        assertChainContinued();
    }

    @Test
    @DisplayName("Token valido de un usuario que ya no existe: no autentica y no falla")
    void deletedUser_doesNotAuthenticateAndDoesNotFail() throws Exception {
        request.addHeader("Authorization", "Bearer token-ok");
        when(jwtService.extractUserId("token-ok")).thenReturn(Optional.of(USER_ID));
        when(userDetailsService.loadUserById(USER_ID))
                .thenThrow(new UsernameNotFoundException("Credenciales invalidas"));

        filter.doFilter(request, response, chain);

        assertThat(currentAuthentication()).isNull();
        assertChainContinued();
    }

    // ---------- encabezados sospechosos o distintos ----------

    @Test
    @DisplayName("Dos encabezados Authorization: es sospechoso y no se autentica")
    void twoAuthorizationHeaders_areNotAuthenticated() throws Exception {
        request.addHeader("Authorization", "Bearer token-uno");
        request.addHeader("Authorization", "Bearer token-dos");

        filter.doFilter(request, response, chain);

        assertThat(currentAuthentication()).isNull();
        assertChainContinued();
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    @DisplayName("Un esquema distinto de Bearer (Basic) se ignora")
    void nonBearerScheme_isIgnored() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, chain);

        assertThat(currentAuthentication()).isNull();
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    @DisplayName("Un Bearer vacio se ignora")
    void emptyBearer_isIgnored() throws Exception {
        request.addHeader("Authorization", "Bearer ");

        filter.doFilter(request, response, chain);

        assertThat(currentAuthentication()).isNull();
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    @DisplayName("FR-SECAUTH-007: un token en la URL no se lee")
    void tokenInQueryString_isNotRead() throws Exception {
        request.setParameter("access_token", "token-ok");

        filter.doFilter(request, response, chain);

        assertThat(currentAuthentication()).isNull();
        verifyNoInteractions(jwtService, userDetailsService);
    }

    // ---------- autenticacion ya establecida ----------

    @Test
    @DisplayName("No se sobrescribe una autenticacion que ya estaba establecida")
    void existingAuthentication_isNotOverwritten() throws Exception {
        Authentication existing = UsernamePasswordAuthenticationToken.authenticated(
                "otro-usuario", null, List.of());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(existing);
        SecurityContextHolder.setContext(context);
        request.addHeader("Authorization", "Bearer token-ok");

        filter.doFilter(request, response, chain);

        assertThat(currentAuthentication()).isSameAs(existing);
        assertChainContinued();
        verifyNoInteractions(jwtService, userDetailsService);
    }
}