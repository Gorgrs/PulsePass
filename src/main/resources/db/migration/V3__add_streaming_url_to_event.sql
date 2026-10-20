-- =====================================================================
-- PulsePass - V3: evento hibrido
-- ---------------------------------------------------------------------
-- FR-EVT-006: un evento puede tener opcionalmente una URL de streaming
-- de hasta 500 caracteres.
--
-- El requisito nuevo NO se agrega editando V1: V1 ya fue aplicada y su
-- checksum esta registrado en flyway_schema_history. El esquema se
-- evoluciona siempre hacia adelante, con una migracion nueva (NFR-002).
-- La columna es nullable porque los eventos presenciales ya existentes
-- no tienen streaming.
-- =====================================================================

ALTER TABLE events
    ADD COLUMN streaming_url VARCHAR(500);
