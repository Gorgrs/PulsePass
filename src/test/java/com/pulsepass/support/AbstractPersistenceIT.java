package com.pulsepass.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base de todas las pruebas de integracion de persistencia.
 *
 * <p>{@code @Transactional} hace que cada prueba se ejecute dentro de una
 * transaccion que se revierte al terminar: las pruebas no se contaminan entre
 * si y no hace falta limpiar tablas a mano.</p>
 */
@SpringBootTest
@Import(PostgresContainerConfiguration.class)
@Transactional
public abstract class AbstractPersistenceIT {
}
