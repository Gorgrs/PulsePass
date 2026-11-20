import { useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import Aviso from '../componentes/Aviso'
import { BadgeTicket } from '../componentes/Badge'
import Boton from '../componentes/Boton'
import Campo from '../componentes/Campo'
import CodigoTexto from '../componentes/CodigoTexto'
import { Cargando, EstadoVacio, ErrorDeCarga } from '../componentes/Estados'
import PrecioCOP from '../componentes/PrecioCOP'
import Tabla from '../componentes/Tabla'
import { useAvisos } from '../componentes/Avisos'
import { useTicketsPagadosPorEvento, useUsarTicket } from '../funcionalidades/tickets/hooks'
import { TIPOS_TICKET } from '../funcionalidades/tickets/tipos'
import { mensajeDeError } from '../lib/http'
import { RUTAS } from '../lib/rutas'

export default function TicketsPagadosEvento() {
  const { eventCode } = useParams()
  const navigate = useNavigate()
  const { notificar } = useAvisos()
  const [codigo, setCodigo] = useState('')
  const consulta = useTicketsPagadosPorEvento(eventCode)
  const usar = useUsarTicket()

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">Tickets pagados{eventCode ? `: ${eventCode}` : ''}</h1>
      <form
        className="flex max-w-md items-end gap-3"
        onSubmit={(e) => {
          e.preventDefault()
          if (codigo.trim()) void navigate(RUTAS.ticketsPagados(codigo.trim()))
        }}
      >
        <div className="flex-1">
          <Campo etiqueta="Código del evento" value={codigo} onChange={(e) => setCodigo(e.target.value)} placeholder="CMF-2026" />
        </div>
        <Boton type="submit">Consultar</Boton>
      </form>

      {eventCode && consulta.isPending && <Cargando />}
      {consulta.isError && <ErrorDeCarga error={consulta.error} noEncontrado="Evento no encontrado" onReintentar={() => void consulta.refetch()} />}
      {usar.error && <Aviso tono="advertencia">{mensajeDeError(usar.error)}</Aviso>}
      {consulta.data && consulta.data.length === 0 && <EstadoVacio mensaje="No hay tickets pagados" />}
      {consulta.data && consulta.data.length > 0 && (
        <Tabla
          filas={consulta.data}
          clave={(t) => t.ticketCode}
          columnas={[
            { encabezado: 'Código', celda: (t) => <CodigoTexto>{t.ticketCode}</CodigoTexto> },
            { encabezado: 'Tipo', celda: (t) => TIPOS_TICKET[t.type] },
            { encabezado: 'Comprador', celda: (t) => t.userEmail ?? '—' },
            { encabezado: 'Precio', celda: (t) => <PrecioCOP valor={t.price} /> },
            { encabezado: 'Estado', celda: (t) => <BadgeTicket estado={t.status} /> },
            {
              encabezado: 'Acción',
              celda: (t) => (
                <Boton
                  variante="secundario"
                  cargando={usar.isPending && usar.variables === t.ticketCode}
                  onClick={() => usar.mutate(t.ticketCode, { onSuccess: () => notificar('exito', 'Ingreso registrado.') })}
                >
                  Usar
                </Boton>
              ),
            },
          ]}
        />
      )}
    </div>
  )
}
