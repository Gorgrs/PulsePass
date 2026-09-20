package com.pulsepass.repository;

import com.pulsepass.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a {@link User}.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** FR-USR-002: el email es UNIQUE; se ignoran mayusculas al buscar. */
    Optional<User> findByEmailIgnoreCase(String email);

    /** FR-USR-002: el username tambien es UNIQUE. */
    Optional<User> findByUsername(String username);

    /** Usuarios activos, en orden alfabetico. */
    List<User> findByActiveTrueOrderByUsernameAsc();

    /** Comprobacion barata antes de intentar un INSERT que violaria el UNIQUE. */
    boolean existsByEmailIgnoreCase(String email);
}
