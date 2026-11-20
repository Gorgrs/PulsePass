package com.pulsepass.controller;

import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.ArtistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArtistController.class)
class ArtistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtistService artistService;

    private ArtistResponse solarBeat() {
        return new ArtistResponse(1L, "Solar Beat", "Colombia", "Pop", true);
    }

    private ArtistResponse neonWaves() {
        return new ArtistResponse(2L, "Neon Waves", "Colombia", "Electronic", true);
    }

    @Test
    void findById_existingArtist_returns200() throws Exception {
        when(artistService.findById(1L)).thenReturn(solarBeat());

        mockMvc.perform(get("/api/artists/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stageName").value("Solar Beat"));

        verify(artistService).findById(1L);
    }

    @Test
    void findById_unknownArtist_returns404() throws Exception {
        when(artistService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Artist not found: 99"));

        mockMvc.perform(get("/api/artists/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Artist not found: 99"));
    }

    @Test
    void findByStageName_returns200() throws Exception {
        when(artistService.findByStageName("Neon Waves")).thenReturn(neonWaves());

        mockMvc.perform(get("/api/artists/by-stage-name").param("stageName", "Neon Waves"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.stageName").value("Neon Waves"));

        verify(artistService).findByStageName("Neon Waves");
    }

    @Test
    void findActiveArtists_returns200() throws Exception {
        when(artistService.findActiveArtists()).thenReturn(List.of(solarBeat(), neonWaves()));

        mockMvc.perform(get("/api/artists/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].stageName").value("Solar Beat"));

        verify(artistService).findActiveArtists();
    }
}