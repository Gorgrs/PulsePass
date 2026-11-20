package com.pulsepass.repository;

import com.pulsepass.domain.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a {@link Venue}.
 *
 * <p>Todo lo que resuelve este repositorio son consultas sobre atributos
 * propios de la entidad, asi que no hace falta ningun {@code @Query}:
 * los Query Methods bastan (NFR-007).</p>
 */
public interface VenueRepository extends JpaRepository<Venue, Long> {

    /** FR-VEN-001 / AC-001: recuperar un venue por su codigo de negocio. */
    Optional<Venue> findByCode(String code);

    /** Venues de una ciudad, sin distinguir mayusculas. */
    List<Venue> findByCityIgnoreCase(String city);

    /** Venues operativos, en orden alfabetico. */
    List<Venue> findByActiveTrueOrderByNameAsc();

    /** Capacidad minima; util para filtrar recintos antes de programar un evento. */
    List<Venue> findByCapacityGreaterThanEqualOrderByCapacityDesc(Integer capacity);
}
