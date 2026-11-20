import { Link } from 'react-router'
import { RUTAS } from '../lib/rutas'

export default function SinPermisos() {
  return (
    <div className="py-16 text-center">
      <p className="font-titulo text-6xl font-bold text-acento-500">403</p>
      <h1 className="mt-2 text-2xl font-bold">No tienes permisos</h1>
      <p className="mt-2 text-slate-600 dark:text-slate-400">Tu cuenta no puede acceder a esta página.</p>
      <Link to={RUTAS.inicio} className="mt-6 inline-block rounded-lg bg-marca-600 px-5 py-3 text-sm font-semibold text-white">Volver al inicio</Link>
    </div>
  )
}
