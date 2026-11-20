import { z } from 'zod'

const EstadoSchema = z.object({
  desde: z.string().optional(),
  registrado: z.boolean().optional(),
  motivo: z.string().optional(),
})
export type EstadoNavegacion = z.infer<typeof EstadoSchema>

// El state de la ubicación llega como unknown: se valida antes de usarlo
export function leerEstado(state: unknown): EstadoNavegacion {
  const r = EstadoSchema.safeParse(state)
  return r.success ? r.data : {}
}
