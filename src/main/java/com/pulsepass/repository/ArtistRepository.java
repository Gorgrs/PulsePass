package com.pulsepass.repository;

import com.pulsepass.domain.Artist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a {@link Artist}.
 */
public interface ArtistRepository extends JpaRepository<Artist, Long> {

    /** FR-ART-002: el stageName es UNIQUE, por eso el resultado es un Optional. */
    Optional<Artist> findByStageNameIgnoreCase(String stageName);

    /** Artistas activos ordenados alfabeticamente. */
    List<Artist> findByActiveTrueOrderByStageNameAsc();

    /** Busqueda parcial por nombre artistico. */
    List<Artist> findByStageNameContainingIgnoreCaseOrderByStageNameAsc(String fragment);

    /**
     * Artistas que participan en un evento determinado.
     *
     * <p>Recorre la relacion N:M en sentido inverso
     * ({@code Artist -> events -> eventCode}). Se resuelve con JPQL porque
     * un Query Method equivalente
     * ({@code findByEventsEventCodeOrderByStageNameAsc}) esconde el JOIN y
     * deja el proposito menos legible (NFR-006, NFR-007).</p>
     */
    @Query("""
           select a
           from Artist a
           join a.events e
           where e.eventCode = :eventCode
           order by a.stageName asc
           """)
    List<Artist> findArtistsByEventCode(@Param("eventCode") String eventCode);
}
