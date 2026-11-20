package com.pulsepass.repository;

import com.pulsepass.domain.Venue;
import com.pulsepass.support.AbstractPersistenceIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * FR-VEN-001 a FR-VEN-003 y AC-001.
 */
class VenuePersistenceTest extends AbstractPersistenceIT {

    @Autowired
    private VenueRepository venueRepository;

    @Test
    @DisplayName("FR-VEN-001: metodos heredados de JpaRepository sobre un venue")
    void shouldPersistVenueUsingInheritedMethods() {
        Venue venue = new Venue("VEN-SMR-01", "Marina Convention Center",
                "Santa Marta", "Carrera 1 # 22-58", 5000);

        Venue saved = venueRepository.save(venue);

        assertThat(saved.getId()).isNotNull();
        assertThat(venueRepository.findById(saved.getId())).isPresent();
        assertThat(venueRepository.existsById(saved.getId())).isTrue();
        assertThat(venueRepository.count()).isPositive();
    }

    @Test
    @DisplayName("AC-001: se recupera por codigo de negocio y su capacidad es mayor que cero")
    void shouldFindVenueByCode() {
        venueRepository.save(new Venue("VEN-SMR-01", "Marina Convention Center",
                "Santa Marta", "Carrera 1 # 22-58", 5000));

        Optional<Venue> found = venueRepository.findByCode("VEN-SMR-01");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Marina Convention Center");
        assertThat(found.get().getCity()).isEqualTo("Santa Marta");
        assertThat(found.get().getCapacity()).isPositive();
    }

    @Test
    @DisplayName("Query Method navegando ciudad, ignorando mayusculas")
    void shouldFindVenuesByCityIgnoringCase() {
        venueRepository.save(new Venue("VEN-SMR-01", "Marina Convention Center",
                "Santa Marta", "Carrera 1 # 22-58", 5000));
        venueRepository.save(new Venue("VEN-BOG-01", "Coliseo Norte",
                "Bogota", "Calle 100 # 15-20", 8000));

        assertThat(venueRepository.findByCityIgnoreCase("santa marta"))
                .extracting(Venue::getCode)
                .containsExactly("VEN-SMR-01");
    }

    @Test
    @DisplayName("FR-VEN-002 / QT-009: PostgreSQL rechaza un codigo de venue duplicado")
    void shouldRejectDuplicatedVenueCode() {
        venueRepository.saveAndFlush(new Venue("VEN-DUP", "Primero",
                "Santa Marta", "Calle 1", 100));

        // saveAndFlush fuerza el INSERT inmediato; sin el, la violacion
        // aparecera mas tarde y no dentro de esta prueba.
        assertThatThrownBy(() -> venueRepository.saveAndFlush(
                new Venue("VEN-DUP", "Segundo", "Bogota", "Calle 2", 200)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-VEN-003: el CHECK de PostgreSQL rechaza capacidad igual a cero")
    void shouldRejectNonPositiveCapacity() {
        // La regla no vive en Java: si se cambiara la entidad, el CHECK
        // seguiria protegiendo la base (NFR-001).
        assertThatThrownBy(() -> venueRepository.saveAndFlush(
                new Venue("VEN-ZERO", "Sin aforo", "Santa Marta", "Calle 3", 0)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
