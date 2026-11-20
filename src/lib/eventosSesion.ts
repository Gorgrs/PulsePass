import { z } from 'zod'

// Eventos creados en esta sesión (GAP-01): el API no lista borradores
const CLAVE = 'pulsepass.eventosCreados'
const ListaSchema = z.array(z.object({ eventCode: z.string(), name: z.string() }))
export type EventoCreado = z.infer<typeof ListaSchema>[number]

export function leerEventosCreados(): EventoCreado[] {
  try {
    const crudo = sessionStorage.getItem(CLAVE)
    const lista = crudo ? ListaSchema.safeParse(JSON.parse(crudo)) : null
    return lista?.success ? lista.data : []
  } catch {
    return []
  }
}

export function recordarEventoCreado(evento: EventoCreado): void {
  try {
    const otros = leerEventosCreados().filter((e) => e.eventCode !== evento.eventCode)
    sessionStorage.setItem(CLAVE, JSON.stringify([evento, ...otros]))
  } catch {
    // sin sessionStorage: no se recuerda
  }
}
