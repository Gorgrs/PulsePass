import { z } from 'zod'

export const UsuarioSchema = z.looseObject({
  username: z.string(),
  email: z.string(),
  firstName: z.string(),
  lastName: z.string(),
  birthDate: z.string().nullish(),
  phone: z.string().nullish(),
  city: z.string().nullish(),
  active: z.boolean().optional(),
})
export type Usuario = z.infer<typeof UsuarioSchema>

export const RegistroSchema = z.object({
  username: z.string().trim().min(1, 'El usuario es obligatorio'),
  email: z.email('Ingresa un correo válido'),
  firstName: z.string().trim().min(1, 'El nombre es obligatorio'),
  lastName: z.string().trim().min(1, 'El apellido es obligatorio'),
  phone: z.string(),
  city: z.string(),
  birthDate: z
    .string()
    .min(1, 'La fecha de nacimiento es obligatoria')
    .refine((v) => new Date(v).getTime() <= Date.now(), 'La fecha no puede ser futura'),
  password: z
    .string()
    .min(12, 'Mínimo 12 caracteres')
    .refine((v) => new TextEncoder().encode(v).length <= 72, 'Máximo 72 bytes'),
})
export type RegistroForm = z.infer<typeof RegistroSchema>
