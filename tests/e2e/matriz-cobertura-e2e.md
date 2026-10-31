# Matriz de cobertura E2E — PulsePass

## 1. Rutas × roles (caso → HTTP esperado)

Cada celda lista `paso·caso→HTTP esperado`. «—» significa que la ruta no se prueba con ese rol. ✓ = PASS y ✗ = FAIL según `registro-ejecucion-manual.md`; sin marca = sin ejecutar.

| Ruta | ANONYMOUS | USER_A (propio) | USER_B (ajeno) | STAFF | ADMIN |
|---|---|---|---|---|---|
| `GET /api/venues/active` | 001·PRE-01→200 ✓<br>002·TC-BIZ-001a→200 ✓<br>009·TC-AUTH-015→200 ✓ | — | — | — | — |
| `GET /api/venues/{code}` | 003·TC-BIZ-001b→200 ✓<br>004·TC-HTTP-005→404 ✓ | — | — | — | — |
| `GET /api/artists/active` | 005·TC-BIZ-002a→200 ✓ | — | — | — | — |
| `GET /api/artists/{id}` | 006·TC-BIZ-002b→200 ✓<br>007·TC-HTTP-ART-404→404 ✓ | — | — | — | — |
| `GET /api/artists/by-stage-name` | 008·TC-BIZ-002c→200 ✓ | — | — | — | — |
| `POST /api/users` | 010·TC-AUTH-001→201 ✓<br>011·TC-BIZ-006a→201 ✓<br>012·TC-BIZ-006b→409 ✓<br>013·TC-SEC-016a→201 ✓<br>014·TC-HTTP-001→400 ✓<br>015·TC-HTTP-002→400 ✓<br>016·TC-HTTP-006→400 ✓<br>017·TC-HTTP-003→400 ✓<br>018·PRE-04→201 ✓<br>019·PRE-05→201 ✓<br>097·TC-AUTH-013a→201 ✓ | — | — | — | — |
| `POST /api/auth/login` | 020·TC-AUTH-002→200 ✓<br>021·TC-AUTH-003→200 ✓<br>022·TC-AUTH-016a→200 ✓<br>023·TC-SEC-016b→200 ✓<br>024·TC-AUTH-016b→200 ✓<br>025·TC-AUTH-016c→200 ✓<br>026·TC-AUTH-004→401 ✓<br>027·TC-AUTH-005→401 ✓<br>098·TC-AUTH-013b→200 ✓<br>102·TC-AUTH-013e→401 ✓ | — | — | — | — |
| `GET /api/auth/me` | 033·TC-AUTH-007→401 ✓<br>034·TC-AUTH-008→401 ✓<br>036·TC-AUTH-011→401 ✓<br>037·TC-AUTH-011b→401 ✓<br>038·TC-AUTH-012→401 ✓ | 028·TC-AUTH-006→200 ✓<br>030·TC-SEC-016c→200 ✓<br>035·TC-AUTH-009→401 ✗<br>099·TC-AUTH-013c→200 ✓<br>101·TC-AUTH-013d→401 ✓<br>104·TC-AUTH-010→401 ✓ | 029·TC-AUTH-016d→200 ✓ | 031·TC-AUTH-016e→200 ✓ | 032·TC-AUTH-016f→200 ✓ |
| `POST /api/events` | 039·TC-SEC-001a→401 ✓ | 040·TC-SEC-001b→403 ✓ | — | 041·TC-SEC-001c→403 ✓ | 042·TC-BIZ-003→201 ✓<br>043·TC-BIZ-003b→201 ✓<br>044·TC-BIZ-015a→409 ✓<br>045·TC-BIZ-015b→400 ✓<br>046·TC-BIZ-015c→404 ✓ |
| `GET /api/events/{eventCode}` | 047·TC-SEC-004→404 ✓<br>050·TC-HTTP-EVT-404→404 ✓<br>059·TC-BIZ-017→200 ✓ | — | — | 049·TC-SEC-015b→200 ✓ | 048·TC-SEC-015→200 ✓ |
| `POST /api/events/{eventCode}/artists/{artistId}` | — | 052·TC-SEC-003→403 ✓ | — | — | 051·TC-BIZ-005a→200 ✓ |
| `GET /api/events/by-artist` | 053·TC-SEC-005→200 ✓<br>060·TC-BIZ-005b→200 ✓ | — | — | — | — |
| `PATCH /api/events/{eventCode}/publish` | — | 054·TC-SEC-002a→403 ✓ | — | 055·TC-SEC-002b→403 ✓ | 057·TC-BIZ-004a→200 ✓ |
| `GET /api/events/published` | 056·TC-BIZ-004z→200 ✓<br>058·TC-BIZ-004b→200 ✓ | — | — | — | — |
| `GET /api/users/by-email` | — | 061·TC-BIZ-016a→200 ✓ | 064·TC-SEC-006→403 ✓ | — | 063·TC-BIZ-016c→200 ✓ |
| `GET /api/users/by-username` | 066·TC-SEC-007b→401 ✓ | 062·TC-BIZ-016b→200 ✓ | 065·TC-SEC-007→403 ✓ | — | — |
| `POST /api/tickets` | 074·TC-SEC-008c→401 ✓ | 067·TC-BIZ-007a→201 ✓<br>072·TC-SEC-008→403 ✓<br>088·TC-BIZ-007b→201 ✓<br>094·TC-BIZ-014a→409 ✓<br>095·TC-BIZ-014b→404 ✓<br>096·TC-HTTP-004→400 ✓ | — | — | — |
| `GET /api/tickets/{ticketCode}` | 076·TC-SEC-009b→401 ✓ | 068·TC-BIZ-008a→200 ✓<br>081·TC-SEC-011b→200 ✓<br>086·TC-BIZ-010b→200 ✓ | 075·TC-SEC-009→403 ✓ | 070·TC-BIZ-008c→200 ✓<br>077·TC-HTTP-TKT-404→404 ✓<br>092·TC-BIZ-012b→200 ✓ | — |
| `GET /api/tickets/by-user` | — | 069·TC-BIZ-008b→200 ✓ | 073·TC-SEC-008b→200 ✓<br>078·TC-SEC-010→403 ✓ | — | 071·TC-BIZ-008d→200 ✓ |
| `PATCH /api/tickets/{ticketCode}/cancel` | — | 085·TC-BIZ-010→200 ✓<br>087·TC-BIZ-011→409 ✓ | 079·TC-SEC-011→403 ✓ | — | — |
| `PATCH /api/tickets/{ticketCode}/use` | — | 090·TC-SEC-012b→403 ✓ | 080·TC-SEC-012→403 ✓ | 091·TC-BIZ-012→200 ✓<br>093·TC-BIZ-013→409 ✓ | — |
| `GET /api/events/{eventCode}/tickets/paid` | 083·TC-SEC-013b→401 ✓ | 082·TC-SEC-013→403 ✓ | — | 084·TC-BIZ-009→200 ✓<br>089·TC-BIZ-009b→200 ✓<br>100·TC-SEC-017a→200 ✓<br>103·TC-SEC-017b→403 ✓ | — |

Casos por HTTP esperado: 200: 45, 201: 10, 400: 6, 401: 16, 403: 15, 404: 7, 409: 5. Total: 104.

## 2. Trazabilidad requisito → caso → ruta → resultado

| Requisito | Evidencia / casos (pasos) | Resultado |
|---|---|---|
| FR-E2E-001/002 | Colección importable con 22 rutas y ambiente con variables (sección 1 de esta matriz) | PASS |
| FR-E2E-003/004 | Logins y `/api/auth/me` por actor: pasos 020, 021, 022, 024, 025, 029, 031, 032, 023, 030, 028 | PASS |
| FR-E2E-005/006 | Cada request trae HTTP/JSON esperado y la instrucción «Copiar» (artistId, tokens, ticketCancelableCode, ticketUsableCode) | FAIL (TC-AUTH-009) |
| FR-E2E-007 | Matriz de acceso: carpeta 07 (36 casos) y sección 1 | FAIL (TC-AUTH-009) |
| FR-E2E-008 | Relecturas de estado (carpeta 09): pasos 056, 073, 081, 086, 089, 092 | PASS |
| FR-E2E-009 | ErrorResponse con 5 campos: pasos 004, 007, 014, 015, 016, 017, 050, 077, 096, y cada caso 4xx | PASS |
| FR-E2E-010/016 | `fixtures/README.md` y `seed-e2e.sql`; repetición con `runId` o base nueva | Cumplido |
| FR-E2E-011/012 | `checklist-e2e-manual.md` y `registro-ejecucion-manual.md` diligenciada | Cumplido |
| FR-E2E-013 | `.gitignore` y ambiente con secretos vacíos; sin tokens ni contraseñas en evidencias | Cumplido |
| FR-E2E-014/015 | Sección «Defectos y bloqueos» de la bitácora; dependientes de un prerrequisito fallido → BLOCKED | Cumplido |
| FR-E2E-017 | Cuerpos y mensajes tomados de los DTO y del código real de PulsePass | Cumplido |

## 3. Criterios de aceptación (PRD §19) → casos

| AC | Casos (pasos) |
|---|---|
| AC-01 Registro y login | 010, 020, 028 |
| AC-02 ADMIN publica | 042, 043, 051, 057, 059 |
| AC-03 USER sin privilegio | 040, 054 |
| AC-04 Propiedad de ticket | 075, 076, 079, 081, 081 |
| AC-05 Compra ajena | 072, 073, 074, 073 |
| AC-06 Uso por STAFF | 091, 092, 092, 093 |
| AC-07 JWT inválido | 034, 035, 036, 037, 104 |
| AC-08 Borradores | 047, 053, 048, 049 |
| AC-09 ErrorResponse | 004, 007, 014, 015, 016, 017, 050, 077, 096 |
| AC-10 Verificación completa | 22/22 rutas (sección 1) con bitácora diligenciada |
