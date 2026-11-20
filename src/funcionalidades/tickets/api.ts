import { z } from 'zod'
import { request } from '../../lib/http'
import { TicketSchema, type TicketType } from './tipos'

const ListaTickets = z.array(TicketSchema)

export const comprarTicket = (datos: { userEmail: string; eventCode: string; type: TicketType }) =>
  request('/tickets', { metodo: 'POST', cuerpo: datos, schema: TicketSchema })
export const obtenerTicket = (codigo: string) =>
  request(`/tickets/${encodeURIComponent(codigo)}`, { schema: TicketSchema })
export const obtenerTicketsDeUsuario = (email: string) =>
  request(`/tickets/by-user?email=${encodeURIComponent(email)}`, { schema: ListaTickets })
export const obtenerTicketsPagados = (eventCode: string) =>
  request(`/events/${encodeURIComponent(eventCode)}/tickets/paid`, { schema: ListaTickets })
export const cancelarTicket = (codigo: string) =>
  request(`/tickets/${encodeURIComponent(codigo)}/cancel`, { metodo: 'PATCH', schema: TicketSchema })
export const usarTicket = (codigo: string) =>
  request(`/tickets/${encodeURIComponent(codigo)}/use`, { metodo: 'PATCH', schema: TicketSchema })
