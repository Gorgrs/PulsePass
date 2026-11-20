# PulsePass Web (Fase 5)

SPA en React 19 + TypeScript + Tailwind v4 + React Router + TanStack Query + React Hook Form + zod.
Consume la API REST de PulsePass (20 operaciones + 3 de autenticación).

## Requisitos
- Node 22.22 o superior
- API de PulsePass corriendo en `http://localhost:8080` (el proxy de Vite reenvía `/api`)

## Comandos
```bash
npm install
npm run dev        # http://localhost:5173
npm run build
npm run typecheck  # tsc --noEmit
npm test           # Vitest
```

Variables: ver `.env.example` (`VITE_API_BASE_URL`, público, sin secretos).

## Roles de demostración
`POST /api/users` siempre crea un USER. STAFF y ADMIN se asignan por SQL en la base local (semilla de la Fase 4).

## Estructura
- `src/lib`: cliente HTTP (`http.ts`), rutas, formato, errores de formulario
- `src/funcionalidades/<área>`: `tipos.ts` (zod), `api.ts`, `hooks.ts`, componentes
- `src/layouts`: Layout, Navbar y guardas (`RutaProtegida`, `RutaPorRol`, `SoloAnonimo`)
- `src/pages`: las páginas de la sección 8 del PRD (carga perezosa salvo Inicio)

## Decisiones
- El token vive en memoria y `sessionStorage`; solo viaja en `Authorization: Bearer`.
- Un 401 con sesión activa cierra la sesión, limpia la caché y lleva a `/login`.
- El correo de la compra sale de la sesión. Si `/api/auth/me` no lo devuelve, se obtiene con `GET /api/users/by-username`.
- Se añadió la ruta `/staff/eventos` (sin código) para consultar un evento desde el menú de STAFF/ADMIN.
- Los diseños PNG del PRD (sección 9.0) no vienen en este zip: cópialos a `docs/diseno/`.
