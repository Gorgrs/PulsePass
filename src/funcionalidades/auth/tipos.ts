import { z } from 'zod'

export const RolSchema = z.enum(['USER', 'STAFF', 'ADMIN'])
export type Rol = z.infer<typeof RolSchema>

export const LoginSchema = z.object({
  identifier: z.string().min(1, 'Ingresa tu usuario o correo'),
  password: z.string().min(1, 'Ingresa tu contraseña'),
})
export type LoginForm = z.infer<typeof LoginSchema>

export const AuthResponseSchema = z.looseObject({
  accessToken: z.string().min(1),
  tokenType: z.string(),
  expiresIn: z.number(),
})

const IdSchema = z.union([z.string(), z.number()]).optional()
export const MeSchema = z.looseObject({
  userId: IdSchema,
  identificador: IdSchema,
  id: IdSchema,
  username: z.string(),
  email: z.string().nullish(),
  roles: z.array(z.string()),
})

export const SesionGuardadaSchema = z.object({ token: z.string().min(1), expiraEn: z.number() })

export interface UsuarioAutenticado {
  id: string
  username: string
  email: string
  roles: Rol[]
}

export type Sesion =
  | { estado: 'cargando' }
  | { estado: 'anonimo'; motivo?: 'expirada' }
  | { estado: 'autenticado'; token: string; expiraEn: number; usuario: UsuarioAutenticado }

export function normalizarRoles(roles: string[]): Rol[] {
  const salida: Rol[] = []
  for (const r of roles) {
    const limpio = RolSchema.safeParse(r.replace(/^ROLE_/, ''))
    if (limpio.success && !salida.includes(limpio.data)) salida.push(limpio.data)
  }
  return salida
}
