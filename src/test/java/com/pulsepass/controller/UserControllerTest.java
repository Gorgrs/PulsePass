package com.pulsepass.controller;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.AuthService;
import com.pulsepass.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    // El registro ahora pasa por AuthService (usuario + contrasena + rol en una transaccion).
    @MockitoBean
    private AuthService authService;

    private static final String VALID_USER_JSON = """
            {
              "username": "andrea",
              "email": "andrea@correo.com",
              "firstName": "Andrea",
              "lastName": "Gomez",
              "phone": "3001234567",
              "city": "Santa Marta",
              "birthDate": "1998-05-10",
              "password": "ClaveDePrueba2026"
            }
            """;

    private UserResponse andrea() {
        return new UserResponse(1L, "andrea", "andrea@correo.com", true,
                "Andrea", "Gomez", "3001234567", "Santa Marta", LocalDate.of(1998, 5, 10));
    }

    @Test
    void register_validRequest_returns201() throws Exception {
        when(authService.register(any(RegisterUserRequest.class))).thenReturn(andrea());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.email").value("andrea@correo.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(authService).register(any(RegisterUserRequest.class));
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        String invalidJson = """
                {
                  "username": "andrea",
                  "email": "esto-no-es-un-email",
                  "firstName": "Andrea",
                  "lastName": "Gomez",
                  "birthDate": "1998-05-10",
                  "password": "ClaveDePrueba2026"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.email").value("Email must be valid"));

        verify(authService, never()).register(any(RegisterUserRequest.class));
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        String shortPasswordJson = """
                {
                  "username": "andrea",
                  "email": "andrea@correo.com",
                  "firstName": "Andrea",
                  "lastName": "Gomez",
                  "birthDate": "1998-05-10",
                  "password": "corta"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(shortPasswordJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.password")
                        .value("Password must be between 12 and 72 characters"));

        verify(authService, never()).register(any(RegisterUserRequest.class));
    }

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        when(authService.register(any(RegisterUserRequest.class)))
                .thenThrow(new DuplicateResourceException("Username already exists: andrea"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Username already exists: andrea"));
    }

    @Test
    void findByEmail_returns200() throws Exception {
        when(userService.findByEmail("andrea@correo.com")).thenReturn(andrea());

        mockMvc.perform(get("/api/users/by-email").param("email", "andrea@correo.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("andrea@correo.com"));

        verify(userService).findByEmail("andrea@correo.com");
    }

    @Test
    void findByUsername_returns200() throws Exception {
        when(userService.findByUsername("andrea")).thenReturn(andrea());

        mockMvc.perform(get("/api/users/by-username").param("username", "andrea"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("andrea"));

        verify(userService).findByUsername("andrea");
    }

    @Test
    void findByEmail_unknownUser_returns404() throws Exception {
        when(userService.findByEmail("nadie@correo.com"))
                .thenThrow(new ResourceNotFoundException("User not found: nadie@correo.com"));

        mockMvc.perform(get("/api/users/by-email").param("email", "nadie@correo.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found: nadie@correo.com"));
    }
}