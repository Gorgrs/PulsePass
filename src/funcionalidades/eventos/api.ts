import { z } from 'zod'
import { request } from '../../lib/http'
import { EventoResumenSchema, EventoSchema, type CrearEventoForm } from './tipos'

const ListaResumenSchema = z.array(EventoResumenSchema)

export const obtenerPublicados = () => request('/events/published', { schema: ListaResumenSchema })
export const obtenerEvento = (codigo: string) =>
  request(`/events/${encodeURIComponent(codigo)}`, { schema: EventoSchema })
export const obtenerPorArtista = (stageName: string) =>
  request(`/events/by-artist?stageName=${encodeURIComponent(stageName)}`, { schema: ListaResumenSchema })
export const crearEvento = (datos: CrearEventoForm) =>
  request('/events', { metodo: 'POST', cuerpo: datos, schema: EventoSchema })
export const publicarEvento = (codigo: string) =>
  request(`/events/${encodeURIComponent(codigo)}/publish`, { metodo: 'PATCH', schema: EventoSchema })
export const asociarArtista = (codigo: string, artistaId: number) =>
  request(`/events/${encodeURIComponent(codigo)}/artists/${artistaId}`, { metodo: 'POST', schema: EventoSchema })
