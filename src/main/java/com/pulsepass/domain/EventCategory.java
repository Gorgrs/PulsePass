package com.pulsepass.domain;

/**
 * Catalogo de categorias de evento (seccion 6.2 del PRD).
 *
 * <p>Se persiste con {@code @Enumerated(EnumType.STRING)} y no como ordinal:
 * un ordinal depende del orden de declaracion, asi que insertar un valor en
 * medio de este enum reinterpretaria silenciosamente las filas ya guardadas
 * (BR-008 / FR-EVT-004).</p>
 */
public enum EventCategory {

    MUSIC,
    SPORTS,
    TECHNOLOGY,
    EDUCATION,
    CULTURE,
    ENTERTAINMENT
}
