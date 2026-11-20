import { useParams } from 'react-router'
import { BadgeTicket } from '../componentes/Badge'
import CodigoTexto from '../componentes/CodigoTexto'
import { Cargando, EstadoVacio } from '../componentes/Estados'
import PrecioCOP from '../componentes/PrecioCOP'
import Tarjeta from '../componentes/Tarjeta'
import AccionCancelar from '../funcionalidades/tickets/componentes/AccionCancelar'
import { useTicket } from '../funcionalidades/tickets/hooks'
import { TIPOS_TICKET } from '../funcionalidades/tickets/tipos'
import { formatoFechaHora } from '../lib/formato'
import { ApiError } from '../lib/http'

export default function DetalleTicket() {
  const { ticketCode } = useParams()
  const consulta = useTicket(ticketCode)

  if (consulta.isPending) return <Cargando />
  if (consulta.isError) {
    // 403/404: no se revela si el ticket existe
    if (consulta.error instanceof ApiError && (consulta.error.status === 403 || consulta.error.status === 404))
      return <EstadoVacio mensaje="Ticket no disponible" />
    return <EstadoVacio mensaje="No pudimos cargar el ticket." />
  }
  const t = consulta.data
  return (
    <Tarjeta className="mx-auto max-w-xl space-y-3">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Ticket</h1>
        <BadgeTicket estado={t.status} />
      </div>
      <p>Código: <CodigoTexto>{t.ticketCode}</CodigoTexto></p>
      <p>Evento: {t.eventName ?? t.eventCode}</p>
      <p>Tipo: {TIPOS_TICKET[t.type]}</p>
      <p>Precio: <PrecioCOP valor={t.price} /></p>
      {t.purchaseDate && <p>Comprado: {formatoFechaHora(t.purchaseDate)}</p>}
      <AccionCancelar ticket={t} />
    </Tarjeta>
  )
}
