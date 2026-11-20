import { Link } from 'react-router'
import { BadgeTicket } from '../../../componentes/Badge'
import CodigoTexto from '../../../componentes/CodigoTexto'
import PrecioCOP from '../../../componentes/PrecioCOP'
import { formatoFechaHora } from '../../../lib/formato'
import { RUTAS } from '../../../lib/rutas'
import { TIPOS_TICKET, type Ticket } from '../tipos'
import AccionCancelar from './AccionCancelar'

export default function TarjetaTicket({ ticket }: { ticket: Ticket }) {
  return (
    <div className="space-y-2 rounded-xl bg-white p-4 shadow-sm dark:bg-slate-900">
      <div className="flex items-center justify-between gap-2">
        <Link to={RUTAS.ticket(ticket.ticketCode)} className="hover:underline">
          <CodigoTexto>{ticket.ticketCode}</CodigoTexto>
        </Link>
        <BadgeTicket estado={ticket.status} />
      </div>
      <p className="font-semibold">{ticket.eventName ?? ticket.eventCode}</p>
      <p className="text-sm text-slate-600 dark:text-slate-400">
        {TIPOS_TICKET[ticket.type]} · <PrecioCOP valor={ticket.price} />
        {ticket.purchaseDate && <> · {formatoFechaHora(ticket.purchaseDate)}</>}
      </p>
      <AccionCancelar ticket={ticket} />
    </div>
  )
}
