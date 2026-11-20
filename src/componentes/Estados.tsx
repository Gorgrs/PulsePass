import { Navigate } from 'react-router'
import { ApiError } from '../lib/http'
import { RUTAS } from '../lib/rutas'
import Boton from './Boton'
import Icono from './Iconos'

export function Cargando() {
  return (
    <div aria-busy="true" aria-live="polite" className="space-y-3">
      <span className="sr-only">Cargando…</span>
      <div className="h-6 w-1/3 animate-pulse rounded bg-slate-200 dark:bg-slate-800" />
      <div className="h-24 animate-pulse rounded bg-slate-200 dark:bg-slate-800" />
      <div className="h-24 animate-pulse rounded bg-slate-200 dark:bg-slate-800" />
    </div>
  )
}

export function EstadoVacio({ mensaje, accion }: { mensaje: string; accion?: React.ReactNode }) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-xl border border-dashed border-slate-300 p-8 text-center text-slate-600 dark:border-slate-700 dark:text-slate-400">
      <Icono nombre="vacio" className="size-8" />
      <p>{mensaje}</p>
      {accion}
    </div>
  )
}

export function EstadoError({ mensaje, onReintentar }: { mensaje: string; onReintentar?: () => void }) {
  return (
    <div role="alert" className="flex flex-col items-center gap-3 rounded-xl bg-red-50 p-8 text-center text-red-800 dark:bg-red-950 dark:text-red-200">
      <Icono nombre="alerta" className="size-8" />
      <p>{mensaje}</p>
      {onReintentar && (
        <Boton variante="secundario" onClick={onReintentar}>
          Reintentar
        </Boton>
      )}
    </div>
  )
}

// Traduce el error de una carga de página completa (sección 13 del PRD)
export function ErrorDeCarga({
  error,
  onReintentar,
  noEncontrado = 'No disponible.',
}: {
  error: unknown
  onReintentar?: () => void
  noEncontrado?: string
}) {
  if (error instanceof ApiError) {
    if (error.status === 403) return <Navigate to={RUTAS.sinPermisos} replace />
    if (error.status === 404) return <EstadoVacio mensaje={noEncontrado} />
    return <EstadoError mensaje={error.message} onReintentar={onReintentar} />
  }
  return <EstadoError mensaje="Ocurrió un error inesperado. Inténtalo más tarde." onReintentar={onReintentar} />
}
