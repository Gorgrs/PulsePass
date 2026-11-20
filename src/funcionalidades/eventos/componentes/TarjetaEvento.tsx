import { Link } from 'react-router'
import { BadgeEvento } from '../../../componentes/Badge'
import Icono from '../../../componentes/Iconos'
import { formatoFechaHora } from '../../../lib/formato'
import { RUTAS } from '../../../lib/rutas'
import { CATEGORIAS, type EventoResumen } from '../tipos'

export default function TarjetaEvento({ evento }: { evento: EventoResumen }) {
  const categoria = CATEGORIAS[evento.category]
  const lugar = evento.venueName ?? evento.venueCity ?? evento.city
  return (
    <Link
      to={RUTAS.evento(evento.eventCode)}
      className="block overflow-hidden rounded-xl bg-white shadow-sm transition hover:shadow-md focus-visible:outline-2 focus-visible:outline-marca-600 dark:bg-slate-900"
    >
      <div className={`flex h-28 items-center justify-center ${categoria.panel}`}>
        <Icono nombre="musica" className="size-10" />
      </div>
      <div className="space-y-2 p-4">
        <div className="flex items-center justify-between gap-2">
          <span className="text-xs font-semibold text-slate-500">{categoria.etiqueta}</span>
          <BadgeEvento estado={evento.status} />
        </div>
        <h3 className="text-lg font-bold">{evento.name}</h3>
        <p className="flex items-center gap-1 text-sm text-slate-600 dark:text-slate-400">
          <Icono nombre="calendario" className="size-4" /> {formatoFechaHora(evento.eventDate)}
        </p>
        {lugar && (
          <p className="flex items-center gap-1 text-sm text-slate-600 dark:text-slate-400">
            <Icono nombre="ubicacion" className="size-4" /> {lugar}
          </p>
        )}
      </div>
    </Link>
  )
}
