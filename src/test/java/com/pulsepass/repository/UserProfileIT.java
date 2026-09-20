package com.pulsepass.repository;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.support.AbstractPersistenceIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * QT-004: relacion 1:1 User - UserProfile. Cubre FR-USR-001 a FR-USR-004,
 * BR-004 y AC-004.
 */
class UserProfileIT extends AbstractPersistenceIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    private UserProfile perfilDeAndrea() {
        return new UserProfile("Andrea", "Romero", "+57 300 000 0000",
                "Santa Marta", LocalDate.of(2000, 5, 14));
    }

    @Test
    @DisplayName("UC-04: usuario y perfil se guardan juntos gracias al cascade")
    void shouldPersistUserAndProfileThroughCascade() {
        User andrea = new User("andrea", "andrea@pulsepass.test");
        UserProfile perfil = perfilDeAndrea();
        andrea.assignProfile(perfil);

        User saved = userRepository.saveAndFlush(andrea);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getProfile().getId()).isNotNull();
        // assignProfile mantiene los dos lados: el perfil conoce a su usuario.
        assertThat(saved.getProfile().getUser().getUsername()).isEqualTo("andrea");
    }

    @Test
    @DisplayName("FR-USR-004: el perfil se recupera navegando desde el email del usuario")
    void shouldFindProfileByUserEmail() {
        User andrea = new User("andrea", "andrea@pulsepass.test");
        andrea.assignProfile(perfilDeAndrea());
        userRepository.saveAndFlush(andrea);

        assertThat(userProfileRepository.findByUserEmailIgnoreCase("ANDREA@PULSEPASS.TEST"))
                .get()
                .extracting(UserProfile::getFirstName, UserProfile::getCity)
                .containsExactly("Andrea", "Santa Marta");
    }

    @Test
    @DisplayName("AC-004: la base impide un segundo perfil para el mismo usuario")
    void shouldRejectSecondProfileForSameUser() {
        User andrea = new User("andrea", "andrea@pulsepass.test");
        andrea.assignProfile(perfilDeAndrea());
        User saved = userRepository.saveAndFlush(andrea);

        UserProfile intruso = new UserProfile("Otra", "Persona", null, "Bogota", null);
        intruso.setUser(saved);

        // Sin UNIQUE(user_id) esto pasaria y la relacion seria 1:N encubierta.
        assertThatThrownBy(() -> userProfileRepository.saveAndFlush(intruso))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-USR-002: username y email son unicos")
    void shouldRejectDuplicatedEmail() {
        userRepository.saveAndFlush(new User("andrea", "andrea@pulsepass.test"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(
                new User("andrea2", "andrea@pulsepass.test")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Query Method: el email se busca ignorando mayusculas")
    void shouldFindUserByEmailIgnoringCase() {
        userRepository.saveAndFlush(new User("andrea", "andrea@pulsepass.test"));

        assertThat(userRepository.findByEmailIgnoreCase("Andrea@PulsePass.Test"))
                .get()
                .extracting(User::getUsername)
                .isEqualTo("andrea");
    }
}
