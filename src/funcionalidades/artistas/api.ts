import { z } from 'zod'
import { request } from '../../lib/http'
import { ArtistaSchema } from '../eventos/tipos'

export const obtenerArtistasActivos = () => request('/artists/active', { schema: z.array(ArtistaSchema) })
export const obtenerArtista = (id: number) => request(`/artists/${id}`, { schema: ArtistaSchema })
export const obtenerArtistaPorNombre = (stageName: string) =>
  request(`/artists/by-stage-name?stageName=${encodeURIComponent(stageName)}`, { schema: ArtistaSchema })
