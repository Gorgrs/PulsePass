# Fixtures E2E

| Fixture | Cómo se crea | Variable Postman |
|---|---|---|
| Venue activo `VEN-E2E-01` | `seed-e2e.sql` | `venueActiveCode` |
| Artistas activos (Solar Beat, …) | Migración Flyway V2 | `artistId` (copiado de la API), `artistStageName` |
| USER_A, USER_B, USER_C, USER_D | Registro público en la colección | `userA*` … `userD*` |
| STAFF / ADMIN | Registro público + SQL de roles (SQL-1) | `staffIdentifier`, `adminIdentifier` |
| Evento publicado (`...-A`) y evento en borrador (`...-B`) | Creados por ADMIN en el flujo C | `publishedEventCode`, `draftEventCode`, `eventToPublishCode` |
| Tickets T1 / T2 | Comprados en el flujo D | `ticketCancelableCode`, `ticketUsableCode` |

Ningún privilegio se asigna por `POST /api/users`. No hay contraseñas en este repositorio.

> Tablas y columnas confirmadas contra las migraciones V1 y V4 (`users`, `user_roles`, `venues`, `artists`).

## SQL-1 — Asignar ADMIN y STAFF (solo `pulsepass_e2e`, tras los pasos de registro PRE-04 y PRE-05)

```sql
-- cambia r1 por tu runId
INSERT INTO user_roles (user_id, role) SELECT id, 'STAFF' FROM users WHERE username = 'staffr1';
INSERT INTO user_roles (user_id, role) SELECT id, 'ADMIN' FROM users WHERE username = 'adminr1';

SELECT u.username, r.role FROM users u JOIN user_roles r ON r.user_id = u.id
WHERE u.username IN ('staffr1', 'adminr1');
```

## SQL-2 — Casos especiales (solo `pulsepass_e2e`)

```sql
-- TC-AUTH-013d / 013e: desactivar USER_D (cambia r1 por tu runId)
UPDATE users SET active = false WHERE username = 'userdr1';

-- TC-SEC-017b: revocar el rol STAFF
DELETE FROM user_roles WHERE role = 'STAFF' AND user_id = (SELECT id FROM users WHERE username = 'staffr1');
```

## Repetir desde cero

Opción A (rápida): cambia `runId` en el ambiente (r1 → r2). Todos los usuarios, eventos y tickets nuevos tendrán códigos distintos; el venue se reutiliza.

Opción B (limpia): borra y recrea **solo** la base E2E, arranca la app (Flyway) y vuelve a ejecutar `seed-e2e.sql`:

```sql
-- conectado a la base postgres, NUNCA a otra base
DROP DATABASE IF EXISTS pulsepass_e2e;
CREATE DATABASE pulsepass_e2e;
```
