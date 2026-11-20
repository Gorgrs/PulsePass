package com.pulsepass.repository;

import com.pulsepass.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acceso a {@link UserRole}.
 */
public interface UserRoleRepository extends JpaRepository<UserRole, UserRole.UserRoleId> {

    /** Roles actuales de un usuario, buscados por su id persistente. */
    List<UserRole> findByIdUserId(Long userId);
}