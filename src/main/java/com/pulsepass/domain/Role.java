package com.pulsepass.domain;

/**
 * Roles de acceso de PulsePass. Los nombres coinciden con los valores
 * permitidos en la tabla user_roles (migracion V4).
 */
public enum Role {
    USER,
    STAFF,
    ADMIN
}