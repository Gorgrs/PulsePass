import clsx from 'clsx'
import type { ReactNode } from 'react'
import Icono from './Iconos'

type Tono = 'error' | 'exito' | 'info' | 'advertencia'

const TONOS: Record<Tono, string> = {
  error: 'border-red-300 bg-red-50 text-red-800 dark:border-red-800 dark:bg-red-950 dark:text-red-200',
  exito: 'border-emerald-300 bg-emerald-50 text-emerald-800 dark:border-emerald-800 dark:bg-emerald-950 dark:text-emerald-200',
  info: 'border-sky-300 bg-sky-50 text-sky-800 dark:border-sky-800 dark:bg-sky-950 dark:text-sky-200',
  advertencia: 'border-amber-300 bg-amber-50 text-amber-900 dark:border-amber-800 dark:bg-amber-950 dark:text-amber-200',
}

export default function Aviso({ tono, children }: { tono: Tono; children: ReactNode }) {
  return (
    <div
      role={tono === 'error' || tono === 'advertencia' ? 'alert' : 'status'}
      className={clsx('flex items-start gap-2 rounded-lg border px-3 py-2 text-sm', TONOS[tono])}
    >
      <Icono nombre={tono === 'exito' ? 'check' : 'alerta'} className="mt-0.5 size-4 shrink-0" />
      <div>{children}</div>
    </div>
  )
}
