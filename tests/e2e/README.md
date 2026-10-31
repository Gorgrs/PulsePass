# PulsePass — Pruebas E2E manuales con Postman (Fase 5)

Pruebas de caja negra HTTP contra la app real + PostgreSQL. **Manual:** sin scripts, sin Runner, sin Newman.

## Contenido

| Archivo | Para qué |
|---|---|
| `postman/PulsePass-Manual-E2E.postman_collection.json` | 22 rutas, 104 requests en 10 carpetas; cada request con descripción (ID, objetivo, actor, esperado, revisión) |
| `postman/PulsePass-local.postman_environment.json` | Plantilla de variables **sin secretos** |
| `fixtures/seed-e2e.sql` | Venue de prueba (solo `pulsepass_e2e`) |
| `fixtures/README.md` | Fixtures, SQL de roles y casos especiales, y reinicio |
| `checklist-e2e-manual.md` | Orden de ejecución paso a paso + cobertura 22/22 |
| `matriz-cobertura-e2e.md` | Matriz ruta × rol y trazabilidad requisito → caso → resultado |
| `registro-ejecucion-manual.md` | Bitácora para anotar esperado vs observado |

## Preparación

1. Crear la base `pulsepass_e2e`.
2. Arrancar la app apuntando a esa base: `DB_URL=jdbc:postgresql://localhost:5432/pulsepass_e2e`, `DB_USER`, `DB_PASSWORD` y `PULSEPASS_JWT_SECRET_BASE64` (obligatoria, sin valor por defecto; clave aleatoria de al menos 32 bytes, exclusiva de E2E). Flyway aplica V1–V4 al arrancar.
3. Ejecutar `fixtures/seed-e2e.sql` sobre `pulsepass_e2e`.
4. Importar colección y ambiente en Postman; seleccionar el ambiente; escribir las contraseñas en **Current value**.

## Ejecución

- Seguir `checklist-e2e-manual.md` en orden (el número 001–104 está en el nombre de cada request).
- STAFF y ADMIN se registran por HTTP (solo obtienen USER); conservan el rol USER y el rol STAFF/ADMIN se les agrega por SQL en el paso **SQL-1**, antes de sus logins.
- Tras cada login, copiar `accessToken` a la variable del actor (`userAToken`, `userBToken`, `userCToken`, `userDToken`, `staffToken`, `adminToken`).
- Copiar los códigos indicados (`artistId`, `ticketCancelableCode`, `ticketUsableCode`).
- Los tokens duran 15 min: ante un 401 inesperado, repetir el login del actor.
- Política de recurso ajeno: **403**. Borrador consultado por un visitante o por USER: **404**.
- Un 2xx no es PASS por sí solo: revisar cuerpo y, si hubo mutación o denegación, la relectura posterior.
- Si falla un prerrequisito, marcar los casos dependientes como **BLOCKED**.

## Resultados de la ejecución r3

| Dato | Valor |
|---|---|
| Ejecutado por | Romero Satizabal Jorge Andres y María Camila Madrid Torres |
| Rama probada | feature/testE2E |
| Pasos | 104 (PASS: 103, FAIL: 1, BLOCKED: 0, NOT RUN: 0) |
| Defectos | DEF-035 (TC-AUTH-009, JWT alterado): abierto, pendiente de repetir cambiando un carácter del medio de la firma |

Detalle en `registro-ejecucion-manual.md`; cobertura por ruta y rol en `matriz-cobertura-e2e.md`.

## Repetir

Cambiar `runId` (r1 → r2) o recrear `pulsepass_e2e` (ver `fixtures/README.md`). Nunca borrar otra base.

## Seguridad

No versionar tokens ni contraseñas. Si exportas el ambiente, deja vacíos los valores secretos. `foreignKeyToken` y `unsignedToken` son JWT de prueba construidos offline con una clave descartada (no sirven contra la app).
