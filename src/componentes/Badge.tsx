import { ESTADOS_EVENTO, type EventStatus } from '../funcionalidades/eventos/tipos'
import { ESTADOS_TICKET, type TicketStatus } from '../funcionalidades/tickets/tipos'

const BASE = 'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold'

export function BadgeEvento({ estado }: { estado: EventStatus }) {
  const { etiqueta, clases } = ESTADOS_EVENTO[estado]
  return <span className={`${BASE} ${clases}`}>{etiqueta}</span>
}

export function BadgeTicket({ estado }: { estado: TicketStatus }) {
  const { etiqueta, clases } = ESTADOS_TICKET[estado]
  return <span className={`${BASE} ${clases}`}>{etiqueta}</span>
}
