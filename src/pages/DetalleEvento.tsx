import { Link, useParams } from 'react-router'
import { BadgeEvento } from '../componentes/Badge'
import { Cargando, ErrorDeCarga } from '../componentes/Estados'
import Icono from '../componentes/Iconos'
import Tarjeta from '../componentes/Tarjeta'
import PanelCompra from '../funcionalidades/eventos/componentes/PanelCompra'
import { useEvento } from '../funcionalidades/eventos/hooks'
import { CATEGORIAS } from '../funcionalidades/eventos/tipos'
import { formatoFechaHora } from '../lib/formato'
import { RUTAS } from '../lib/rutas'

export default function DetalleEvento() {
  const { eventCode } = useParams()
  const consulta = useEvento(eventCode)

  if (consulta.isPending) return <Cargando />
  if (consulta.isError)
    return <ErrorDeCarga error={consulta.error} onReintentar={() => void consulta.refetch()} noEncontrado="Evento no disponible" />

  const e = consulta.data
  const categoria = CATEGORIAS[e.category]
  return (
    <div className="grid gap-8 pb-44 md:grid-cols-3 md:pb-0">
      <div className="space-y-6 md:col-span-2">
        <div className={`flex h-48 items-center justify-center rounded-xl ${categoria.panel}`}>
          <Icono nombre="musica" className="size-16" />
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <h1 className="text-3xl font-bold">{e.name}</h1>
          <BadgeEvento estado={e.status} />
        </div>
        <p className="flex items-center gap-2 text-slate-600 dark:text-slate-400">
          <Icono nombre="calendario" className="size-4" /> {formatoFechaHora(e.eventDate)}
          {e.minimumAge != null && <span>· Edad mínima {e.minimumAge}</span>}
        </p>
        {e.description && <p>{e.description}</p>}

        <section>
          <h2 className="mb-2 text-xl font-bold">Artistas</h2>
          {e.artists && e.artists.length > 0 ? (
            <ul className="flex flex-wrap gap-2">
              {e.artists.map((a) => (
                <li key={a.id}>
                  <Link to={RUTAS.artista(a.id)} className="rounded-full bg-marca-50 px-3 py-1 text-sm font-medium text-marca-700 hover:underline dark:bg-slate-800 dark:text-marca-500">
                    {a.stageName}
                  </Link>
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-slate-500">Artistas por confirmar.</p>
          )}
        </section>
      </div>

      <aside className="space-y-4">
        {e.venueCode && (
          <Tarjeta>
            <h2 className="mb-1 flex items-center gap-1 font-bold"><Icono nombre="ubicacion" className="size-4" /> Sede</h2>
            <Link to={RUTAS.sede(e.venueCode)} className="text-marca-700 hover:underline dark:text-marca-500">
              {e.venueName ?? e.venueCode}
            </Link>
          </Tarjeta>
        )}
        <PanelCompra evento={e} />
      </aside>
    </div>
  )
}
