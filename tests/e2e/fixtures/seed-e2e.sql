-- =====================================================================
-- seed-e2e.sql — Fixtures EXCLUSIVOS del ambiente E2E (pulsepass_e2e)
-- Ejecutar DESPUES de arrancar la app una vez (Flyway ya creo las tablas).
-- Idempotente. No contiene contraseñas.
-- Tablas/columnas segun V1__create_schema.sql.
-- =====================================================================

DO $$
BEGIN
    IF current_database() <> 'pulsepass_e2e' THEN
        RAISE EXCEPTION 'seed-e2e.sql solo se ejecuta sobre pulsepass_e2e (estas en %)', current_database();
    END IF;
END $$;

INSERT INTO venues (code, name, city, address, capacity, active)
VALUES ('VEN-E2E-01', 'E2E Arena Santa Marta', 'Santa Marta', 'Calle E2E # 1-01', 500, TRUE)
ON CONFLICT (code) DO NOTHING;

SELECT code, capacity, active FROM venues WHERE code LIKE 'VEN-E2E-%' ORDER BY code;
SELECT id, stage_name FROM artists ORDER BY id;
