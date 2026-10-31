# Checklist E2E manual — orden de ejecución

Marca cada paso al enviarlo y anota el resultado en `registro-ejecucion-manual.md`. La ejecución `r3` fue completada el 18/10/2026.

## Antes de empezar (DoR)

- [x] PostgreSQL arriba y base `pulsepass_e2e` creada
- [x] App arrancada contra `pulsepass_e2e` con secreto JWT exclusivo de E2E (`PULSEPASS_JWT_SECRET_BASE64`)
- [x] `seed-e2e.sql` ejecutado sin errores (venue `VEN-E2E-01`) y artistas del catálogo inicial presentes
- [x] Colección y ambiente importados; ambiente `PulsePass-local (E2E)` seleccionado
- [x] Contraseñas (`userAPassword`, `userBPassword`, `staffPassword`, `adminPassword`) escritas en **Current value** (no en Initial value); mínimo 12 caracteres
- [x] `runId` nuevo si ya ejecutaste antes sobre la misma base


## Flujo A — Descubrimiento público (sin token)

- [x] **001** `PRE-01` — ANONYMOUS — `GET /api/venues/active` → **200**
- [x] **002** `TC-BIZ-001a` — ANONYMOUS — `GET /api/venues/active` → **200**
- [x] **003** `TC-BIZ-001b` — ANONYMOUS — `GET /api/venues/{{venueActiveCode}}` → **200**
- [x] **004** `TC-HTTP-005` — ANONYMOUS — `GET /api/venues/CODIGO-INEXISTENTE` → **404**
- [x] **005** `TC-BIZ-002a` — ANONYMOUS — `GET /api/artists/active` → **200** · **copiar:** Del artista cuyo stageName sea el de artistStageName (Solar Beat), copiar el valor de "id" → variable artistId. No supongas que es 1.
- [x] **006** `TC-BIZ-002b` — ANONYMOUS — `GET /api/artists/{{artistId}}` → **200**
- [x] **007** `TC-HTTP-ART-404` — ANONYMOUS — `GET /api/artists/999999` → **404**
- [x] **008** `TC-BIZ-002c` — ANONYMOUS — `GET /api/artists/by-stage-name` → **200**
- [x] **009** `TC-AUTH-015` — ANONYMOUS — `GET /api/venues/active` → **200**

## Flujo B — Registro, login, JWT y seguridad de eventos

- [x] **010** `TC-AUTH-001` — ANONYMOUS — `POST /api/users` → **201**
- [x] **011** `TC-BIZ-006a` — ANONYMOUS — `POST /api/users` → **201**
- [x] **012** `TC-BIZ-006b` — ANONYMOUS — `POST /api/users` → **409**
- [x] **013** `TC-SEC-016a` — ANONYMOUS — `POST /api/users` → **201**
- [x] **014** `TC-HTTP-001` — ANONYMOUS — `POST /api/users` → **400**
- [x] **015** `TC-HTTP-002` — ANONYMOUS — `POST /api/users` → **400**
- [x] **016** `TC-HTTP-006` — ANONYMOUS — `POST /api/users` → **400**
- [x] **017** `TC-HTTP-003` — ANONYMOUS — `POST /api/users` → **400**
- [x] **018** `PRE-04` — ANONYMOUS — `POST /api/users` → **201**
- [x] **019** `PRE-05` — ANONYMOUS — `POST /api/users` → **201**
- [x] **SQL-1** — Asignar roles: ejecutar el bloque «Asignar ADMIN y STAFF» de `fixtures/README.md` (cambia r1 por tu runId) y verificar con el SELECT.
- [x] **020** `TC-AUTH-002` — ANONYMOUS — `POST /api/auth/login` → **200** · **copiar:** accessToken → userAToken. Copia el MISMO valor también en expiredToken (lo usarás al final, cuando pasen más de 15 min).
- [x] **021** `TC-AUTH-003` — ANONYMOUS — `POST /api/auth/login` → **200**
- [x] **022** `TC-AUTH-016a` — ANONYMOUS — `POST /api/auth/login` → **200** · **copiar:** accessToken → userBToken.
- [x] **023** `TC-SEC-016b` — ANONYMOUS — `POST /api/auth/login` → **200** · **copiar:** accessToken → userCToken.
- [x] **024** `TC-AUTH-016b` — ANONYMOUS — `POST /api/auth/login` → **200** · **copiar:** accessToken → staffToken.
- [x] **025** `TC-AUTH-016c` — ANONYMOUS — `POST /api/auth/login` → **200** · **copiar:** accessToken → adminToken.
- [x] **026** `TC-AUTH-004` — ANONYMOUS — `POST /api/auth/login` → **401**
- [x] **027** `TC-AUTH-005` — ANONYMOUS — `POST /api/auth/login` → **401**
- [x] **028** `TC-AUTH-006` — USER_A — `GET /api/auth/me` → **200**
- [x] **029** `TC-AUTH-016d` — USER_B — `GET /api/auth/me` → **200**
- [x] **030** `TC-SEC-016c` — USER_C — `GET /api/auth/me` → **200**
- [x] **031** `TC-AUTH-016e` — STAFF — `GET /api/auth/me` → **200**
- [x] **032** `TC-AUTH-016f` — ADMIN — `GET /api/auth/me` → **200**
- [x] **033** `TC-AUTH-007` — ANONYMOUS — `GET /api/auth/me` → **401**
- [x] **034** `TC-AUTH-008` — ANONYMOUS — `GET /api/auth/me` → **401**
- [x] **035** `TC-AUTH-009` — USER_A — `GET /api/auth/me` → **401** · **observado:** **200**; defecto `DEF-035` abierto en el registro.
- [x] **036** `TC-AUTH-011` — ANONYMOUS — `GET /api/auth/me` → **401**
- [x] **037** `TC-AUTH-011b` — ANONYMOUS — `GET /api/auth/me` → **401**
- [x] **038** `TC-AUTH-012` — ANONYMOUS — `GET /api/auth/me` → **401**
- [x] **039** `TC-SEC-001a` — ANONYMOUS — `POST /api/events` → **401**
- [x] **040** `TC-SEC-001b` — USER_A — `POST /api/events` → **403**
- [x] **041** `TC-SEC-001c` — STAFF — `POST /api/events` → **403**

## Flujo C — Gestión del catálogo (ADMIN) y visibilidad de borradores

- [x] **042** `TC-BIZ-003` — ADMIN — `POST /api/events` → **201**
- [x] **043** `TC-BIZ-003b` — ADMIN — `POST /api/events` → **201**
- [x] **044** `TC-BIZ-015a` — ADMIN — `POST /api/events` → **409**
- [x] **045** `TC-BIZ-015b` — ADMIN — `POST /api/events` → **400**
- [x] **046** `TC-BIZ-015c` — ADMIN — `POST /api/events` → **404**
- [x] **047** `TC-SEC-004` — ANONYMOUS — `GET /api/events/{{draftEventCode}}` → **404**
- [x] **048** `TC-SEC-015` — ADMIN — `GET /api/events/{{draftEventCode}}` → **200**
- [x] **049** `TC-SEC-015b` — STAFF — `GET /api/events/{{draftEventCode}}` → **200**
- [x] **050** `TC-HTTP-EVT-404` — ANONYMOUS — `GET /api/events/NO-EXISTE-{{runId}}` → **404**
- [x] **051** `TC-BIZ-005a` — ADMIN — `POST /api/events/{{draftEventCode}}/artists/{{artistId}}` → **200**
- [x] **052** `TC-SEC-003` — USER_A — `POST /api/events/{{draftEventCode}}/artists/{{artistId}}` → **403**
- [x] **053** `TC-SEC-005` — ANONYMOUS — `GET /api/events/by-artist` → **200**
- [x] **054** `TC-SEC-002a` — USER_A — `PATCH /api/events/{{draftEventCode}}/publish` → **403**
- [x] **055** `TC-SEC-002b` — STAFF — `PATCH /api/events/{{draftEventCode}}/publish` → **403**
- [x] **056** `TC-BIZ-004z` — ANONYMOUS — `GET /api/events/published` → **200**
- [x] **057** `TC-BIZ-004a` — ADMIN — `PATCH /api/events/{{draftEventCode}}/publish` → **200**
- [x] **058** `TC-BIZ-004b` — ANONYMOUS — `GET /api/events/published` → **200**
- [x] **059** `TC-BIZ-017` — ANONYMOUS — `GET /api/events/{{publishedEventCode}}` → **200**
- [x] **060** `TC-BIZ-005b` — ANONYMOUS — `GET /api/events/by-artist` → **200**

## Flujo D — Usuarios, compra, propiedad, cancelación y uso

- [x] **061** `TC-BIZ-016a` — USER_A — `GET /api/users/by-email` → **200**
- [x] **062** `TC-BIZ-016b` — USER_A — `GET /api/users/by-username` → **200**
- [x] **063** `TC-BIZ-016c` — ADMIN — `GET /api/users/by-email` → **200**
- [x] **064** `TC-SEC-006` — USER_B — `GET /api/users/by-email` → **403**
- [x] **065** `TC-SEC-007` — USER_B — `GET /api/users/by-username` → **403**
- [x] **066** `TC-SEC-007b` — ANONYMOUS — `GET /api/users/by-username` → **401**
- [x] **067** `TC-BIZ-007a` — USER_A — `POST /api/tickets` → **201** · **copiar:** ticketCode → ticketCancelableCode (T1: se cancelará).
- [x] **068** `TC-BIZ-008a` — USER_A — `GET /api/tickets/{{ticketCancelableCode}}` → **200**
- [x] **069** `TC-BIZ-008b` — USER_A — `GET /api/tickets/by-user` → **200**
- [x] **070** `TC-BIZ-008c` — STAFF — `GET /api/tickets/{{ticketCancelableCode}}` → **200**
- [x] **071** `TC-BIZ-008d` — ADMIN — `GET /api/tickets/by-user` → **200**
- [x] **072** `TC-SEC-008` — USER_A — `POST /api/tickets` → **403**
- [x] **073** `TC-SEC-008b` — USER_B — `GET /api/tickets/by-user` → **200**
- [x] **074** `TC-SEC-008c` — ANONYMOUS — `POST /api/tickets` → **401**
- [x] **075** `TC-SEC-009` — USER_B — `GET /api/tickets/{{ticketCancelableCode}}` → **403**
- [x] **076** `TC-SEC-009b` — ANONYMOUS — `GET /api/tickets/{{ticketCancelableCode}}` → **401**
- [x] **077** `TC-HTTP-TKT-404` — STAFF — `GET /api/tickets/TKT-NOEXISTE` → **404**
- [x] **078** `TC-SEC-010` — USER_B — `GET /api/tickets/by-user` → **403**
- [x] **079** `TC-SEC-011` — USER_B — `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` → **403**
- [x] **080** `TC-SEC-012` — USER_B — `PATCH /api/tickets/{{ticketCancelableCode}}/use` → **403**
- [x] **081** `TC-SEC-011b` — USER_A — `GET /api/tickets/{{ticketCancelableCode}}` → **200**
- [x] **082** `TC-SEC-013` — USER_A — `GET /api/events/{{publishedEventCode}}/tickets/paid` → **403**
- [x] **083** `TC-SEC-013b` — ANONYMOUS — `GET /api/events/{{publishedEventCode}}/tickets/paid` → **401**
- [x] **084** `TC-BIZ-009` — STAFF — `GET /api/events/{{publishedEventCode}}/tickets/paid` → **200**
- [x] **085** `TC-BIZ-010` — USER_A — `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` → **200**
- [x] **086** `TC-BIZ-010b` — USER_A — `GET /api/tickets/{{ticketCancelableCode}}` → **200**
- [x] **087** `TC-BIZ-011` — USER_A — `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` → **409**
- [x] **088** `TC-BIZ-007b` — USER_A — `POST /api/tickets` → **201** · **copiar:** ticketCode → ticketUsableCode (T2: se marcará como usado).
- [x] **089** `TC-BIZ-009b` — STAFF — `GET /api/events/{{publishedEventCode}}/tickets/paid` → **200**
- [x] **090** `TC-SEC-012b` — USER_A — `PATCH /api/tickets/{{ticketUsableCode}}/use` → **403**
- [x] **091** `TC-BIZ-012` — STAFF — `PATCH /api/tickets/{{ticketUsableCode}}/use` → **200**
- [x] **092** `TC-BIZ-012b` — STAFF — `GET /api/tickets/{{ticketUsableCode}}` → **200**
- [x] **093** `TC-BIZ-013` — STAFF — `PATCH /api/tickets/{{ticketUsableCode}}/use` → **409**
- [x] **094** `TC-BIZ-014a` — USER_A — `POST /api/tickets` → **409**
- [x] **095** `TC-BIZ-014b` — USER_A — `POST /api/tickets` → **404**
- [x] **096** `TC-HTTP-004` — USER_A — `POST /api/tickets` → **400**

## Flujo E — Regresión de errores y casos especiales (SQL, expiración)

- [x] **097** `TC-AUTH-013a` — ANONYMOUS — `POST /api/users` → **201**
- [x] **098** `TC-AUTH-013b` — ANONYMOUS — `POST /api/auth/login` → **200** · **copiar:** accessToken → userDToken.
- [x] **099** `TC-AUTH-013c` — USER_D — `GET /api/auth/me` → **200**
- [x] **100** `TC-SEC-017a` — STAFF — `GET /api/events/{{publishedEventCode}}/tickets/paid` → **200**
- [x] **SQL-2** — Desactivar USER_D y revocar el rol STAFF: ejecutar el bloque «Casos especiales» de `fixtures/README.md` (solo en `pulsepass_e2e`).
- [x] **101** `TC-AUTH-013d` — USER_D — `GET /api/auth/me` → **401**
- [x] **102** `TC-AUTH-013e` — ANONYMOUS — `POST /api/auth/login` → **401**
- [x] **103** `TC-SEC-017b` — STAFF — `GET /api/events/{{publishedEventCode}}/tickets/paid` → **403**
- [x] **104** `TC-AUTH-010` — USER_A — `GET /api/auth/me` → **401** · **copiar:** ANTES de enviar: espera a que pasen más de 15 min desde el paso 020 (expiredToken guardado entonces).

## Cobertura 22/22 rutas (caso nominal)

| # | Ruta | Caso nominal | Paso |
|---|---|---|---|
| 1 | `GET /api/venues/{code}` | `TC-BIZ-001b` | 003 |
| 2 | `GET /api/venues/active` | `TC-BIZ-001a` | 002 |
| 3 | `GET /api/artists/{id}` | `TC-BIZ-002b` | 006 |
| 4 | `GET /api/artists/by-stage-name` | `TC-BIZ-002c` | 008 |
| 5 | `GET /api/artists/active` | `TC-BIZ-002a` | 005 |
| 6 | `POST /api/users` | `TC-AUTH-001` | 010 |
| 7 | `GET /api/users/by-email` | `TC-BIZ-016a` | 061 |
| 8 | `GET /api/users/by-username` | `TC-BIZ-016b` | 062 |
| 9 | `POST /api/events` | `TC-BIZ-003` | 042 |
| 10 | `GET /api/events/{eventCode}` | `TC-BIZ-017` | 059 |
| 11 | `GET /api/events/published` | `TC-BIZ-004b` | 058 |
| 12 | `PATCH /api/events/{eventCode}/publish` | `TC-BIZ-004a` | 057 |
| 13 | `POST /api/events/{eventCode}/artists/{artistId}` | `TC-BIZ-005a` | 051 |
| 14 | `GET /api/events/by-artist` | `TC-BIZ-005b` | 060 |
| 15 | `POST /api/tickets` | `TC-BIZ-007a` | 067 |
| 16 | `GET /api/tickets/{ticketCode}` | `TC-BIZ-008a` | 068 |
| 17 | `GET /api/tickets/by-user` | `TC-BIZ-008b` | 069 |
| 18 | `GET /api/events/{eventCode}/tickets/paid` | `TC-BIZ-009` | 084 |
| 19 | `PATCH /api/tickets/{ticketCode}/cancel` | `TC-BIZ-010` | 085 |
| 20 | `PATCH /api/tickets/{ticketCode}/use` | `TC-BIZ-012` | 091 |
| 21 | `POST /api/auth/login` | `TC-AUTH-002` | 020 |
| 22 | `GET /api/auth/me` | `TC-AUTH-006` | 028 |

Total: 22/22 rutas (20 de negocio + login + me). Cada ruta tiene además casos negativos en las carpetas 07 y 08.

## Cierre

- [x] Contar PASS / FAIL / BLOCKED / NOT RUN por carpeta en la bitácora
- [x] Verificar que las 22 rutas tienen al menos un caso nominal ejecutado
- [x] Registrar defectos con precondiciones, pasos, HTTP esperado/observado (DEF-035 documentado; JWT alterado aceptado con HTTP 200)
- [x] Confirmar que ningún archivo exportado contiene tokens ni contraseñas
