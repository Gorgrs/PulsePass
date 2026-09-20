package com.pulsepass.repository;

import com.pulsepass.domain.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a {@link UserProfile}.
 */
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    /** FR-USR-003: como user_id es UNIQUE, el resultado es a lo sumo uno. */
    Optional<UserProfile> findByUserId(Long userId);

    /**
     * FR-USR-004: perfil de un usuario identificado por su email.
     *
     * <p>Query Method que navega {@code UserProfile -> user -> email}.</p>
     */
    Optional<UserProfile> findByUserEmailIgnoreCase(String email);

    /** Perfiles de una ciudad. */
    List<UserProfile> findByCityIgnoreCaseOrderByLastNameAsc(String city);
}
