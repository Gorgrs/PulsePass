package com.pulsepass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de PulsePass.
 *
 * <p>El alcance de este taller es exclusivamente la capa de persistencia:
 * migraciones Flyway, entidades JPA, repositories Spring Data y pruebas de
 * integracion contra PostgreSQL real. No hay capa Service ni API REST
 * (seccion 18 del PRD, "Fuera de alcance del taller").</p>
 */
@SpringBootApplication
public class PulsePassApplication {

    public static void main(String[] args) {
        SpringApplication.run(PulsePassApplication.class, args);
    }
}
