import { Link } from 'react-router'
import { Cargando, EstadoVacio, ErrorDeCarga } from '../componentes/Estados'
import Icono from '../componentes/Iconos'
import { useSedesActivas } from '../funcionalidades/sedes/hooks'
import { RUTAS } from '../lib/rutas'

export default function ListaSedes() {
  const consulta = useSedesActivas()
  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">Sedes</h1>
      {consulta.isPending && <Cargando />}
      {consulta.isError && <ErrorDeCarga error={consulta.error} onReintentar={() => void consulta.refetch()} />}
      {consulta.data && consulta.data.length === 0 && <EstadoVacio mensaje="Sin sedes activas." />}
      <ul className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {consulta.data?.map((s) => (
          <li key={s.code}>
            <Link to={RUTAS.sede(s.code)} className="block space-y-1 rounded-xl bg-white p-5 shadow-sm hover:shadow-md dark:bg-slate-900">
              <h2 className="text-lg font-bold">{s.name}</h2>
              <p className="flex items-center gap-1 text-sm text-slate-600 dark:text-slate-400"><Icono nombre="ubicacion" className="size-4" /> {s.city}</p>
              <p className="text-sm text-slate-600 dark:text-slate-400">Capacidad: {s.capacity}</p>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  )
}
