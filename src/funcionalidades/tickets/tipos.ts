import { z } from 'zod'

export const TicketTypeSchema = z.enum(['GENERAL', 'VIP', 'BACKSTAGE', 'STUDENT'])
export const TicketStatusSchema = z.enum(['RESERVED', 'PAID', 'CANCELLED', 'USED'])
export type TicketType = z.infer<typeof TicketTypeSchema>
export type TicketStatus = z.infer<typeof TicketStatusSchema>

export const TicketSchema = z.looseObject({
  ticketCode: z.string(),
  type: TicketTypeSchema,
  price: z.coerce.number(),
  status: TicketStatusSchema,
  purchaseDate: z.string().nullish(),
  userEmail: z.string().nullish(),
  eventCode: z.string().nullish(),
  eventName: z.string().nullish(),
})
export type Ticket = z.infer<typeof TicketSchema>

export const CompraSchema = z.object({ type: TicketTypeSchema })
export type CompraForm = z.infer<typeof CompraSchema>

export const TIPOS_TICKET: Record<TicketType, string> = {
  GENERAL: 'General',
  VIP: 'VIP',
  BACKSTAGE: 'Backstage',
  STUDENT: 'Estudiante',
}

export const ESTADOS_TICKET: Record<TicketStatus, { etiqueta: string; clases: string }> = {
  RESERVED: { etiqueta: 'Reservado', clases: 'bg-sky-100 text-sky-800' },
  PAID: { etiqueta: 'Pagado', clases: 'bg-emerald-100 text-emerald-800' },
  USED: { etiqueta: 'Usado', clases: 'bg-slate-300 text-slate-800' },
  CANCELLED: { etiqueta: 'Cancelado', clases: 'bg-red-100 text-red-800' },
}
