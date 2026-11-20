package com.pulsepass.domain;

/**
 * Estados de un evento (seccion 6.2 del PRD).
 *
 * <p>Secuencia conceptual de la seccion 9.1:</p>
 * <pre>
 * DRAFT -&gt; PUBLISHED -&gt; SOLD_OUT -&gt; FINISHED
 *                    \-&gt; CANCELLED
 * </pre>
 *
 * <p>El MVP no implementa la maquina de estados en Java; la coherencia se
 * cuida en los datos y las consultas. El CHECK de PostgreSQL garantiza que
 * no entren valores fuera de este catalogo (FR-EVT-003).</p>
 */
public enum EventStatus {

    DRAFT,
    PUBLISHED,
    SOLD_OUT,
    CANCELLED,
    FINISHED
}
