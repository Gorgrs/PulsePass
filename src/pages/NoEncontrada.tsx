import { Link } from 'react-router'
import { RUTAS } from '../lib/rutas'

export default function NoEncontrada() {
  return (
    <div className="py-16 text-center">
      <p className="font-titulo text-6xl font-bold text-marca-600">404</p>
      <h1 className="mt-2 text-2xl font-bold">Página no encontrada</h1>
      <p className="mt-2 text-slate-600 dark:text-slate-400">La página que buscas no existe o fue movida.</p>
      <Link to={RUTAS.inicio} className="mt-6 inline-block rounded-lg bg-marca-600 px-5 py-3 text-sm font-semibold text-white">Volver al inicio</Link>
    </div>
  )
}
