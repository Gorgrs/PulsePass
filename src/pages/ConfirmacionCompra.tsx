import { Link, useParams } from 'react-router'
import CodigoTexto from '../componentes/CodigoTexto'
import { Cargando, EstadoVacio } from '../componentes/Estados'
import Icono from '../componentes/Iconos'
import PrecioCOP from '../componentes/PrecioCOP'
import Tarjeta from '../componentes/Tarjeta'
import { useTicket } from '../funcionalidades/tickets/hooks'
import { TIPOS_TICKET } from '../funcionalidades/tickets/tipos'
import { RUTAS } from '../lib/rutas'

export default function ConfirmacionCompra() {
  const { ticketCode } = useParams()
  const consulta = useTicket(ticketCode)

  if (consulta.isPending) return <Cargando />
  if (consulta.isError) return <EstadoVacio mensaje="Ticket no disponible" />
  const t = consulta.data
  return (
    <Tarjeta className="mx-auto max-w-xl space-y-3 text-center">
      <span className="mx-auto grid size-14 place-items-center rounded-full bg-emerald-100 text-emerald-700">
        <Icono nombre="check" className="size-8" />
      </span>
      <h1 className="text-2xl font-bold">¡Compra confirmada!</h1>
      <p>Tu código: <CodigoTexto>{t.ticketCode}</CodigoTexto></p>
      <p>{t.eventName ?? t.eventCode} · {TIPOS_TICKET[t.type]} · <PrecioCOP valor={t.price} /></p>
      <div className="flex flex-wrap justify-center gap-3 pt-2">
        <Link to={RUTAS.misTickets} className="rounded-lg bg-marca-600 px-5 py-3 text-sm font-semibold text-white">Ver mis tickets</Link>
        <Link to={RUTAS.eventos} className="rounded-lg border border-slate-300 px-5 py-3 text-sm font-semibold">Seguir explorando</Link>
      </div>
    </Tarjeta>
  )
}
