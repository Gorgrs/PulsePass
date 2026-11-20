import { Link } from 'react-router'
import { Cargando, EstadoError, EstadoVacio } from '../componentes/Estados'
import { useArtistasActivos } from '../funcionalidades/artistas/hooks'
import TarjetaEvento from '../funcionalidades/eventos/componentes/TarjetaEvento'
import { useEventosPublicados } from '../funcionalidades/eventos/hooks'
import { RUTAS } from '../lib/rutas'

export default function Inicio() {
  const eventos = useEventosPublicados()
  const artistas = useArtistasActivos()

  return (
    <div className="space-y-12">
      <section className="rounded-2xl bg-linear-to-r from-marca-600 to-acento-500 p-8 text-white md:p-12">
        <h1 className="text-3xl font-bold md:text-4xl">Vive la música y los eventos de tu ciudad</h1>
        <p className="mt-3 max-w-xl text-white/90">Descubre la cartelera, compra tu entrada y llega directo a la puerta.</p>
        <div className="mt-6 flex flex-wrap gap-3">
          <Link to={RUTAS.eventos} className="rounded-lg bg-white px-5 py-3 text-sm font-semibold text-marca-700">
            Ver cartelera
          </Link>
          <Link to={RUTAS.registro} className="rounded-lg border border-white px-5 py-3 text-sm font-semibold text-white">
            Crear cuenta
          </Link>
        </div>
      </section>

      <section aria-labelledby="proximos">
        <h2 id="proximos" className="mb-4 text-2xl font-bold">Próximos eventos</h2>
        {eventos.isPending && <Cargando />}
        {eventos.isError && <EstadoError mensaje="No pudimos cargar los eventos." onReintentar={() => void eventos.refetch()} />}
        {eventos.data && eventos.data.length === 0 && <EstadoVacio mensaje="Aún no hay eventos publicados." />}
        {eventos.data && eventos.data.length > 0 && (
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {eventos.data.slice(0, 3).map((e) => (
              <TarjetaEvento key={e.eventCode} evento={e} />
            ))}
          </div>
        )}
      </section>

      <section aria-labelledby="artistas-destacados">
        <h2 id="artistas-destacados" className="mb-4 text-2xl font-bold">Artistas</h2>
        {artistas.isPending && <Cargando />}
        {artistas.data && artistas.data.length === 0 && <EstadoVacio mensaje="Sin artistas activos." />}
        <ul className="grid grid-cols-2 gap-4 md:grid-cols-4">
          {artistas.data?.slice(0, 4).map((a) => (
            <li key={a.id}>
              <Link to={RUTAS.artista(a.id)} className="block rounded-xl bg-white p-4 text-center font-semibold shadow-sm hover:shadow-md dark:bg-slate-900">
                {a.stageName}
              </Link>
            </li>
          ))}
        </ul>
      </section>
    </div>
  )
}
