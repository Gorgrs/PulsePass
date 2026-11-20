import { z } from 'zod'
import { request } from '../../lib/http'
import { SedeSchema } from './tipos'

export const obtenerSedesActivas = () => request('/venues/active', { schema: z.array(SedeSchema) })
export const obtenerSede = (codigo: string) =>
  request(`/venues/${encodeURIComponent(codigo)}`, { schema: SedeSchema })
