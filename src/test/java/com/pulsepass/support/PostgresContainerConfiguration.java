package com.pulsepass.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Contenedor PostgreSQL compartido por todas las pruebas de integracion.
 *
 * <p>NFR-004 y NFR-005: las pruebas no dependen de un PostgreSQL instalado a
 * mano y tampoco usan H2. Testcontainers levanta un motor real y Flyway
 * construye el esquema dentro de el.</p>
 *
 * <p>El contenedor se declara como {@code @Bean} en lugar de como campo
 * estatico con {@code @Container}: asi Spring lo arranca y lo detiene junto
 * con el contexto, y como el contexto se cachea entre clases de prueba, todas
 * comparten el mismo contenedor en lugar de levantar uno por clase.</p>
 *
 * <p>{@code @ServiceConnection} sustituye la configuracion manual de
 * {@code spring.datasource.*}: Spring Boot toma url, usuario y contrasena
 * directamente del contenedor.</p>
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresContainerConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:18-alpine")
                .withDatabaseName("pulsepass_test")
                .withUsername("pulsepass")
                .withPassword("pulsepass");
    }
}
