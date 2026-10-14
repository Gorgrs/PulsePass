## Capa de controladores REST (Fase 3)

API REST que expone los Services de las fases anteriores mediante HTTP. Los Controllers solo traducen HTTP: reciben el pedido, delegan en el Service y devuelven un DTO. Las reglas de negocio permanecen en la capa Service, ningún Controller usa un Repository y ningún endpoint devuelve entidades JPA.

### Endpoints

| Recurso | Método | Endpoint | Respuesta |
|---|---|---|---|
| Venues | GET | `/api/venues/{code}` | 200 / 404 |
| Venues | GET | `/api/venues/active` | 200 |
| Eventos | POST | `/api/events` | 201 / 400 |
| Eventos | GET | `/api/events/{eventCode}` | 200 / 404 |
| Eventos | GET | `/api/events/published` | 200 |
| Eventos | PATCH | `/api/events/{eventCode}/publish` | 200 / 409 |
| Eventos | POST | `/api/events/{eventCode}/artists/{artistId}` | 200 / 409 |
| Eventos | GET | `/api/events/by-artist?stageName=...` | 200 |
| Artistas | GET | `/api/artists/{id}` | 200 / 404 |
| Artistas | GET | `/api/artists/by-stage-name?stageName=...` | 200 / 404 |
| Artistas | GET | `/api/artists/active` | 200 |
| Usuarios | POST | `/api/users` | 201 / 400 / 409 |
| Usuarios | GET | `/api/users/by-email?email=...` | 200 / 404 |
| Usuarios | GET | `/api/users/by-username?username=...` | 200 / 404 |
| Tickets | POST | `/api/tickets` | 201 / 400 / 404 / 409 |
| Tickets | GET | `/api/tickets/{ticketCode}` | 200 / 404 |
| Tickets | GET | `/api/tickets/by-user?email=...` | 200 |
| Tickets | GET | `/api/events/{eventCode}/tickets/paid` | 200 |
| Tickets | PATCH | `/api/tickets/{ticketCode}/cancel` | 200 / 409 |
| Tickets | PATCH | `/api/tickets/{ticketCode}/use` | 200 / 409 |

### Manejo de errores

`GlobalExceptionHandler` (`@RestControllerAdvice`) convierte las excepciones en un `ErrorResponse` con el formato `timestamp`, `status`, `error`, `message` y `details`.

| Condición | Código HTTP |
|---|---|
| Bean Validation falla o JSON mal formado | 400 |
| `ResourceNotFoundException` | 404 |
| `DuplicateResourceException` | 409 |
| `BusinessRuleException` | 409 |
| Error inesperado | 500 |

### Pruebas de los Controllers

Usan `@WebMvcTest`, MockMvc y los Services simulados con `@MockitoBean`, así que no necesitan base de datos ni Docker. Hay 33 pruebas, en `src/test/java/com/pulsepass/controller`:

```
.\mvnw test -Dtest="*ControllerTest"
```

Si el proyecto no incluye el wrapper `mvnw`, se pueden correr desde el IDE con clic derecho sobre la carpeta `controller` de `test` y **Run**.