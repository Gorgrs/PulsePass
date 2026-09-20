package com.pulsepass.domain;

/**
 * Estados de una entrada (FR-TKT-005).
 *
 * <pre>
 * RESERVED -&gt; PAID -&gt; USED
 *          \-&gt; CANCELLED
 * </pre>
 *
 * <p>PAID representa un pago ya confirmado por un sistema externo futuro;
 * el MVP no procesa pagos (seccion 9.2).</p>
 */
public enum TicketStatus {

    RESERVED,
    PAID,
    CANCELLED,
    USED
}
