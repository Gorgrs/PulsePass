# Registro de ejecución manual — PulsePass E2E

| Campo | Valor |
|---|---|
| Ejecutado por | Romero Satizabal Jorge Andres y María Camila Madrid Torres |
| Fecha | 18/10/2026 |
| runId | r3 |
| Rama probada | feature/testE2E |
| Base de datos | pulsepass_e2e |

Resultado: `PASS` / `FAIL` / `BLOCKED` / `NOT RUN`. No pegar tokens, contraseñas ni datos personales reales.

Ejecución r3 completada: **103 PASS, 1 FAIL (DEF-035), 0 BLOCKED, 0 NOT RUN** (104 pasos). Un 2xx no es PASS por sí solo: se revisó el cuerpo y la relectura posterior.

| # | Caso | Actor | Ruta | HTTP esperado | HTTP observado | JSON / estado observado | Resultado | Fecha/hora | Observación |
|---|---|---|---|---|---|---|---|---|---|
| 001 | PRE-01 | ANONYMOUS | `GET /api/venues/active` | 200 | 200 | 1 venue activo: `VEN-E2E-01` (`E2E Arena Santa Marta`), `active: true` | PASS | 18/10/2026 | Respuesta con el venue E2E esperado. |
| 002 | TC-BIZ-001a | ANONYMOUS | `GET /api/venues/active` | 200 | 200 | 1 venue activo: `VEN-E2E-01` (`E2E Arena Santa Marta`), `active: true` | PASS | 18/10/2026 | Lista de venues activos coincide con el fixture E2E. |
| 003 | TC-BIZ-001b | ANONYMOUS | `GET /api/venues/{{venueActiveCode}}` | 200 | 200 | Venue `VEN-E2E-01` encontrado, activo, capacidad 500 | PASS | 18/10/2026 | Consulta por código devuelve el venue esperado. |
| 004 | TC-HTTP-005 | ANONYMOUS | `GET /api/venues/CODIGO-INEXISTENTE` | 404 | 404 | Error `Not Found`: `Venue not found: CODIGO-INEXISTENTE` | PASS | 18/10/2026 | Código inexistente rechazado con el estado esperado. |
| 005 | TC-BIZ-002a | ANONYMOUS | `GET /api/artists/active` | 200 | 200 | 5 artistas activos; `Solar Beat` identificado con `id: 1` | PASS | 18/10/2026 | Catálogo activo recibido correctamente; copiar `artistId = 1`. |
| 006 | TC-BIZ-002b | ANONYMOUS | `GET /api/artists/{{artistId}}` | 200 | 200 | Artista `Solar Beat`, `id: 1`, activo, género `Electronic` | PASS | 18/10/2026 | Consulta por `artistId` devuelve el artista esperado. |
| 007 | TC-HTTP-ART-404 | ANONYMOUS | `GET /api/artists/999999` | 404 | 404 | Error `Not Found`: `Artist not found: 999999` | PASS | 18/10/2026 | Identificador de artista inexistente rechazado con el estado esperado. |
| 008 | TC-BIZ-002c | ANONYMOUS | `GET /api/artists/by-stage-name?stageName={{artistStageName}}` | 200 | 200 | Artista `Solar Beat`, `id: 1`, activo, género `Electronic` | PASS | 18/10/2026 | Búsqueda por nombre artístico devuelve el artista esperado. |
| 009 | TC-AUTH-015 | ANONYMOUS | `GET /api/venues/active` | 200 | 200 | 1 venue activo: `VEN-E2E-01` (`E2E Arena Santa Marta`) | PASS | 18/10/2026 | Endpoint público accesible sin autenticación. |
| 010 | TC-AUTH-001 | ANONYMOUS | `POST /api/users` | 201 | 201 | Usuario creado: `userar3`, `usera.r3@pulsepass.test`, activo | PASS | 18/10/2026 | Usuario r3 registrado con el estado HTTP esperado. |
| 011 | TC-BIZ-006a | ANONYMOUS | `POST /api/users` | 201 | 201 | Usuario creado: `userbr3`, `userb.r3@pulsepass.test`, activo | PASS | 18/10/2026 | Usuario B r3 registrado con el estado HTTP esperado. |
| 012 | TC-BIZ-006b | ANONYMOUS | `POST /api/users` | 409 | 409 | Error `Conflict`: usuario `userar3` ya existe | PASS | 18/10/2026 | Registro duplicado rechazado con el estado esperado. |
| 013 | TC-SEC-016a | ANONYMOUS | `POST /api/users` | 201 | 201 | Usuario creado: `usercr3`, `userc.r3@pulsepass.test`, activo; campo `roles` ignorado | PASS | 18/10/2026 | La inyección de rol no otorgó privilegios por HTTP. |
| 014 | TC-HTTP-001 | ANONYMOUS | `POST /api/users` | 400 | 400 | Error `Bad Request`: validación fallida; campos obligatorios requeridos | PASS | 18/10/2026 | Registro sin campos obligatorios rechazado con el estado esperado. |
| 015 | TC-HTTP-002 | ANONYMOUS | `POST /api/users` | 400 | 400 | Error `Bad Request`: `Email must be valid` | PASS | 18/10/2026 | Correo mal formado rechazado con el estado esperado. |
| 016 | TC-HTTP-006 | ANONYMOUS | `POST /api/users` | 400 | 400 | Error `Bad Request`: `Password must be between 12 and 72 characters` | PASS | 18/10/2026 | Contraseña demasiado corta rechazada con el estado esperado. |
| 017 | TC-HTTP-003 | ANONYMOUS | `POST /api/users` | 400 | 400 | Error `Bad Request`: `Malformed JSON request` | PASS | 18/10/2026 | JSON mal formado rechazado con el estado esperado. |
| 018 | PRE-04 | ANONYMOUS | `POST /api/users` | 201 | 201 | Usuario creado: `staffr3`, `staff.r3@pulsepass.test`, activo | PASS | 18/10/2026 | Usuario STAFF r3 registrado para la preparación E2E. |
| 019 | PRE-05 | ANONYMOUS | `POST /api/users` | 201 | 201 | Usuario creado: `adminr3`, `admin.r3@pulsepass.test`, activo | PASS | 18/10/2026 | Usuario ADMIN r3 registrado; el rol debe asignarse posteriormente mediante SQL-1. |
| 020 | TC-AUTH-002 | ANONYMOUS | `POST /api/auth/login` | 200 | 200 | Login correcto; `tokenType: Bearer`; `expiresIn: 900 s`; token recibido y oculto | PASS | 18/10/2026 | Copiar el mismo token, sin compartirlo, a `userAToken` y `expiredToken`. |
| 021 | TC-AUTH-003 | ANONYMOUS | `POST /api/auth/login` | 200 | 200 | Login correcto; `tokenType: Bearer`; `expiresIn: 900 s`; token recibido y oculto | PASS | 18/10/2026 | Login por email válido; token omitido del registro. |
| 022 | TC-AUTH-016a | ANONYMOUS | `POST /api/auth/login` | 200 | 200 | Login correcto; `tokenType: Bearer`; `expiresIn: 900 s`; token recibido y oculto | PASS | 18/10/2026 | Token de USER_B recibido y omitido del registro. |
| 023 | TC-SEC-016b | ANONYMOUS | `POST /api/auth/login` | 200 | 200 | Login correcto; `tokenType: Bearer`; `expiresIn: 900 s`; token recibido y oculto | PASS | 18/10/2026 | Token de USER_C recibido y omitido del registro. |
| 024 | TC-AUTH-016b | ANONYMOUS | `POST /api/auth/login` | 200 | 200 | Login correcto de `staffr3`; `tokenType: Bearer`; `expiresIn: 900 s`; token recibido y oculto | PASS | 18/10/2026 | Token de STAFF r3 capturado sin registrarlo. |
| 025 | TC-AUTH-016c | ANONYMOUS | `POST /api/auth/login` | 200 | 200 | Login correcto de `adminr3`; `tokenType: Bearer`; `expiresIn: 900 s`; token recibido y oculto | PASS | 18/10/2026 | Token de ADMIN r3 capturado sin registrarlo. |
| 026 | TC-AUTH-004 | ANONYMOUS | `POST /api/auth/login` | 401 | 401 | Error `Unauthorized`: `Credenciales inválidas` | PASS | 18/10/2026 | Contraseña incorrecta rechazada con el estado esperado. |
| 027 | TC-AUTH-005 | ANONYMOUS | `POST /api/auth/login` | 401 | 401 | Error `Unauthorized`: `Credenciales inválidas` | PASS | 18/10/2026 | Identificador inexistente rechazado con el estado esperado. |
| 028 | TC-AUTH-006 | USER_A | `GET /api/auth/me` | 200 | 200 | `userId: 12`; usuario `userar3`; rol `USER` | PASS | 18/10/2026 | Identidad y rol de USER_A r3 verificados correctamente. |
| 029 | TC-AUTH-016d | USER_B | `GET /api/auth/me` | 200 | 200 | `userId: 13`; usuario `userbr3`; rol `USER` | PASS | 18/10/2026 | Identidad y rol de USER_B r3 verificados correctamente. |
| 030 | TC-SEC-016c | USER_C | `GET /api/auth/me` | 200 | 200 | `userId: 14`; usuario `usercr3`; rol `USER` | PASS | 18/10/2026 | El campo `roles: ADMIN` inyectado no otorgó privilegios. |
| 031 | TC-AUTH-016e | STAFF | `GET /api/auth/me` | 200 | 200 | `userId: 15`; usuario `staffr3`; roles `USER`, `STAFF` | PASS | 18/10/2026 | Rol STAFF asignado y verificado correctamente. |
| 032 | TC-AUTH-016f | ADMIN | `GET /api/auth/me` | 200 | 200 | `userId: 16`; usuario `adminr3`; roles `USER`, `ADMIN` | PASS | 18/10/2026 | Rol ADMIN asignado y verificado correctamente. |
| 033 | TC-AUTH-007 | ANONYMOUS | `GET /api/auth/me` | 401 | 401 | HTTP 401 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 034 | TC-AUTH-008 | ANONYMOUS | `GET /api/auth/me` | 401 | 401 | HTTP 401 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 035 | TC-AUTH-009 | USER_A | `GET /api/auth/me` | 401 | 200 | HTTP 200; respuesta de /api/auth/me para userar3, rol USER | FAIL | 18/10/2026 | JWT alterado aceptado: devolvió 200 en lugar del 401 esperado. Posible falso positivo de la prueba (ver DEF-035): confirmar re-ejecutando con un carácter distinto de la firma. |
| 036 | TC-AUTH-011 | ANONYMOUS | `GET /api/auth/me` | 401 | 401 | HTTP 401 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 037 | TC-AUTH-011b | ANONYMOUS | `GET /api/auth/me` | 401 | 401 | HTTP 401 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 038 | TC-AUTH-012 | ANONYMOUS | `GET /api/auth/me?access_token={{userAToken}}` | 401 | 401 | HTTP 401 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 039 | TC-SEC-001a | ANONYMOUS | `POST /api/events` | 401 | 401 | HTTP 401 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 040 | TC-SEC-001b | USER_A | `POST /api/events` | 403 | 403 | HTTP 403 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 041 | TC-SEC-001c | STAFF | `POST /api/events` | 403 | 403 | HTTP 403 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 042 | TC-BIZ-003 | ADMIN | `POST /api/events` | 201 | 201 | HTTP 201 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 043 | TC-BIZ-003b | ADMIN | `POST /api/events` | 201 | 201 | HTTP 201 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 044 | TC-BIZ-015a | ADMIN | `POST /api/events` | 409 | 409 | HTTP 409 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 045 | TC-BIZ-015b | ADMIN | `POST /api/events` | 400 | 400 | HTTP 400 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 046 | TC-BIZ-015c | ADMIN | `POST /api/events` | 404 | 404 | HTTP 404 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 047 | TC-SEC-004 | ANONYMOUS | `GET /api/events/{{draftEventCode}}` | 404 | 404 | HTTP 404 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 048 | TC-SEC-015 | ADMIN | `GET /api/events/{{draftEventCode}}` | 200 | 200 | HTTP 200 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 049 | TC-SEC-015b | STAFF | `GET /api/events/{{draftEventCode}}` | 200 | 200 | HTTP 200 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 050 | TC-HTTP-EVT-404 | ANONYMOUS | `GET /api/events/NO-EXISTE-{{runId}}` | 404 | 404 | HTTP 404 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 051 | TC-BIZ-005a | ADMIN | `POST /api/events/{{draftEventCode}}/artists/{{artistId}}` | 200 | 200 | HTTP 200 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 052 | TC-SEC-003 | USER_A | `POST /api/events/{{draftEventCode}}/artists/{{artistId}}` | 403 | 403 | HTTP 403 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 053 | TC-SEC-005 | ANONYMOUS | `GET /api/events/by-artist?stageName={{artistStageName}}` | 200 | 200 | HTTP 200 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 054 | TC-SEC-002a | USER_A | `PATCH /api/events/{{draftEventCode}}/publish` | 403 | 403 | HTTP 403 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 055 | TC-SEC-002b | STAFF | `PATCH /api/events/{{draftEventCode}}/publish` | 403 | 403 | HTTP 403 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 056 | TC-BIZ-004z | ANONYMOUS | `GET /api/events/published` | 200 | 200 | HTTP 200 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 057 | TC-BIZ-004a | ADMIN | `PATCH /api/events/{{draftEventCode}}/publish` | 200 | 200 | HTTP 200 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 058 | TC-BIZ-004b | ANONYMOUS | `GET /api/events/published` | 200 | 200 | HTTP 200 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 059 | TC-BIZ-017 | ANONYMOUS | `GET /api/events/{{publishedEventCode}}` | 200 | 200 | HTTP 200 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 060 | TC-BIZ-005b | ANONYMOUS | `GET /api/events/by-artist?stageName={{artistStageName}}` | 200 | 200 | HTTP 200 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 061 | TC-BIZ-016a | USER_A | `GET /api/users/by-email?email={{userAEmail}}` | 200 | 200 | HTTP 200 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 062 | TC-BIZ-016b | USER_A | `GET /api/users/by-username?username={{userAUsername}}` | 200 | 200 | HTTP 200 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 063 | TC-BIZ-016c | ADMIN | `GET /api/users/by-email?email={{userAEmail}}` | 200 | 200 | HTTP 200 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 064 | TC-SEC-006 | USER_B | `GET /api/users/by-email?email={{userAEmail}}` | 403 | 403 | HTTP 403 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 065 | TC-SEC-007 | USER_B | `GET /api/users/by-username?username={{userAUsername}}` | 403 | 403 | HTTP 403 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 066 | TC-SEC-007b | ANONYMOUS | `GET /api/users/by-username?username={{userAUsername}}` | 401 | 401 | HTTP 401 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 067 | TC-BIZ-007a | USER_A | `POST /api/tickets` | 201 | 201 | HTTP 201 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 068 | TC-BIZ-008a | USER_A | `GET /api/tickets/{{ticketCancelableCode}}` | 200 | 200 | HTTP 200 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 069 | TC-BIZ-008b | USER_A | `GET /api/tickets/by-user?email={{userAEmail}}` | 200 | 200 | HTTP 200 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 070 | TC-BIZ-008c | STAFF | `GET /api/tickets/{{ticketCancelableCode}}` | 200 | 200 | HTTP 200 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 071 | TC-BIZ-008d | ADMIN | `GET /api/tickets/by-user?email={{userAEmail}}` | 200 | 200 | HTTP 200 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 072 | TC-SEC-008 | USER_A | `POST /api/tickets` | 403 | 403 | HTTP 403 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 073 | TC-SEC-008b | USER_B | `GET /api/tickets/by-user?email={{userBEmail}}` | 200 | 200 | HTTP 200 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 074 | TC-SEC-008c | ANONYMOUS | `POST /api/tickets` | 401 | 401 | HTTP 401 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 075 | TC-SEC-009 | USER_B | `GET /api/tickets/{{ticketCancelableCode}}` | 403 | 403 | HTTP 403 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 076 | TC-SEC-009b | ANONYMOUS | `GET /api/tickets/{{ticketCancelableCode}}` | 401 | 401 | HTTP 401 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 077 | TC-HTTP-TKT-404 | STAFF | `GET /api/tickets/TKT-NOEXISTE` | 404 | 404 | HTTP 404 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 078 | TC-SEC-010 | USER_B | `GET /api/tickets/by-user?email={{userAEmail}}` | 403 | 403 | HTTP 403 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 079 | TC-SEC-011 | USER_B | `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` | 403 | 403 | HTTP 403 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 080 | TC-SEC-012 | USER_B | `PATCH /api/tickets/{{ticketCancelableCode}}/use` | 403 | 403 | HTTP 403 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 081 | TC-SEC-011b | USER_A | `GET /api/tickets/{{ticketCancelableCode}}` | 200 | 200 | HTTP 200 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 082 | TC-SEC-013 | USER_A | `GET /api/events/{{publishedEventCode}}/tickets/paid` | 403 | 403 | HTTP 403 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 083 | TC-SEC-013b | ANONYMOUS | `GET /api/events/{{publishedEventCode}}/tickets/paid` | 401 | 401 | HTTP 401 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 084 | TC-BIZ-009 | STAFF | `GET /api/events/{{publishedEventCode}}/tickets/paid` | 200 | 200 | HTTP 200 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 085 | TC-BIZ-010 | USER_A | `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` | 200 | 200 | HTTP 200 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 086 | TC-BIZ-010b | USER_A | `GET /api/tickets/{{ticketCancelableCode}}` | 200 | 200 | HTTP 200 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 087 | TC-BIZ-011 | USER_A | `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` | 409 | 409 | HTTP 409 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 088 | TC-BIZ-007b | USER_A | `POST /api/tickets` | 201 | 201 | HTTP 201 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 089 | TC-BIZ-009b | STAFF | `GET /api/events/{{publishedEventCode}}/tickets/paid` | 200 | 200 | HTTP 200 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 090 | TC-SEC-012b | USER_A | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 403 | 403 | HTTP 403 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 091 | TC-BIZ-012 | STAFF | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 200 | 200 | HTTP 200 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 092 | TC-BIZ-012b | STAFF | `GET /api/tickets/{{ticketUsableCode}}` | 200 | 200 | HTTP 200 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 093 | TC-BIZ-013 | STAFF | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 409 | 409 | HTTP 409 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 094 | TC-BIZ-014a | USER_A | `POST /api/tickets` | 409 | 409 | HTTP 409 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 095 | TC-BIZ-014b | USER_A | `POST /api/tickets` | 404 | 404 | HTTP 404 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 096 | TC-HTTP-004 | USER_A | `POST /api/tickets` | 400 | 400 | HTTP 400 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 097 | TC-AUTH-013a | ANONYMOUS | `POST /api/users` | 201 | 201 | HTTP 201 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 098 | TC-AUTH-013b | ANONYMOUS | `POST /api/auth/login` | 200 | 200 | HTTP 200 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 099 | TC-AUTH-013c | USER_D | `GET /api/auth/me` | 200 | 200 | HTTP 200 observado; respuesta JSON recibida | PASS | 18/10/2026 | HTTP observado coincide con el esperado. |
| 100 | TC-SEC-017a | STAFF | `GET /api/events/{{publishedEventCode}}/tickets/paid` | 200 | 200 | HTTP 200 observado; respuesta JSON recibida; 19.70 ms | PASS | 18/10/2026 20:41 | HTTP observado coincide con el esperado. |
| 101 | TC-AUTH-013d | USER_D | `GET /api/auth/me` | 401 | 401 | HTTP 401 observado; usuario desactivado rechazado; 16.06 ms | PASS | 18/10/2026 20:41 | HTTP observado coincide con el esperado. |
| 102 | TC-AUTH-013e | ANONYMOUS | `POST /api/auth/login` | 401 | 401 | HTTP 401 observado; login de usuario desactivado rechazado; 153.58 ms | PASS | 18/10/2026 20:41 | HTTP observado coincide con el esperado. |
| 103 | TC-SEC-017b | STAFF | `GET /api/events/{{publishedEventCode}}/tickets/paid` | 403 | 403 | HTTP 403 observado; acceso rechazado después de revocar STAFF; 14.71 ms | PASS | 18/10/2026 20:41 | HTTP observado coincide con el esperado. |
| 104 | TC-AUTH-010 | USER_A | `GET /api/auth/me` | 401 | 401 | HTTP 401 observado; JWT expirado rechazado; 10.08 ms | PASS | 18/10/2026 20:41 | HTTP observado coincide con el esperado. |

## Consolidación por carpeta

| Carpeta | PASS | FAIL | BLOCKED | NOT RUN |
|---|---|---|---|---|
| 00 Preparación / conectividad | 1 | 0 | 0 | 0 |
| 01 Venues (2 rutas) | 2 | 0 | 0 | 0 |
| 02 Artists (3 rutas) | 3 | 0 | 0 | 0 |
| 03 Auth / usuarios (registro, login, me) | 18 | 0 | 0 | 0 |
| 04 Usuarios / consulta y propiedad (2 rutas) | 3 | 0 | 0 | 0 |
| 05 Events / administración y visibilidad (6 rutas) | 9 | 0 | 0 | 0 |
| 06 Tickets / compra, consulta y ciclo de vida (6 rutas) | 9 | 0 | 0 | 0 |
| 07 Seguridad / escenarios negativos | 35 | 1 | 0 | 0 |
| 08 Validaciones y errores HTTP | 17 | 0 | 0 | 0 |
| 09 Flujos de negocio / verificaciones de estado | 6 | 0 | 0 | 0 |

## Defectos y bloqueos

| ID | Paso | Precondiciones | HTTP esperado | HTTP observado | Reproducción | Estado |
|---|---|---|---|---|---|---|
| DEF-035 | 035 | JWT alterado aceptado; devolvió HTTP 200 en vez de 401 | 401 | 200 | Repetir TC-AUTH-009 con un token cuya firma haya sido modificada y revisar validación criptográfica. | ABIERTO — posible falso positivo: repetir cambiando un carácter del MEDIO de la firma (cambiar solo el último puede dejar el token válido) |
