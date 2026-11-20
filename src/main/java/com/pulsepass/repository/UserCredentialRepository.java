package com.pulsepass.repository;

import com.pulsepass.domain.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a {@link UserCredential}.
 *
 * <p>La clave primaria de la credencial es el id del usuario, asi que
 * {@code findById(userId)} ya devuelve la credencial de ese usuario.</p>
 */
public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
}