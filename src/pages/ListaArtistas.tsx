import { Link } from 'react-router'
import { Cargando, EstadoVacio, ErrorDeCarga } from '../componentes/Estados'
import { useArtistasActivos } from '../funcionalidades/artistas/hooks'
import { RUTAS } from '../lib/rutas'

export default function ListaArtistas() {
  const consulta = useArtistasActivos()
  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">Artistas</h1>
      {consulta.isPending && <Cargando />}
      {consulta.isError && <ErrorDeCarga error={consulta.error} onReintentar={() => void consulta.refetch()} />}
      {consulta.data && consulta.data.length === 0 && <EstadoVacio mensaje="Sin artistas activos." />}
      <ul className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
        {consulta.data?.map((a) => (
          <li key={a.id}>
            <Link to={RUTAS.artista(a.id)} className="block rounded-xl bg-white p-6 text-center text-lg font-semibold shadow-sm hover:shadow-md dark:bg-slate-900">
              {a.stageName}
            </Link>
          </li>
        ))}
      </ul>
    </div>
  )
}
