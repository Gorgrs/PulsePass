import { Link, isRouteErrorResponse, useRouteError } from 'react-router'
import { RUTAS } from '../lib/rutas'

export default function ErrorPagina() {
  const error = useRouteError()
  const mensaje = isRouteErrorResponse(error) && error.status === 404
    ? 'No encontramos lo que buscas.'
    : 'Algo salió mal al cargar la página.'
  return (
    <div className="mx-auto max-w-xl px-4 py-24 text-center">
      <h1 className="text-2xl font-bold">Ocurrió un problema</h1>
      <p className="mt-2 text-slate-600">{mensaje}</p>
      <Link to={RUTAS.inicio} className="mt-6 inline-block rounded-lg bg-marca-600 px-5 py-3 text-sm font-semibold text-white">Volver al inicio</Link>
    </div>
  )
}
