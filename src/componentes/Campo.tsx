import clsx from 'clsx'
import { useId, type ComponentProps } from 'react'

interface CampoProps extends ComponentProps<'input'> {
  etiqueta: string
  error?: string
}

export const CLASES_CAMPO =
  'mt-1 block min-h-11 w-full rounded-lg border bg-white px-3 py-2 text-slate-900 focus-visible:outline-2 focus-visible:outline-marca-600 dark:bg-slate-900 dark:text-slate-100'

export default function Campo({ etiqueta, error, className, ...resto }: CampoProps) {
  const id = useId()
  const idError = `${id}-error`
  return (
    <div>
      <label htmlFor={id} className="text-sm font-medium text-slate-700 dark:text-slate-300">
        {etiqueta}
      </label>
      <input
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? idError : undefined}
        className={clsx(CLASES_CAMPO, error ? 'border-red-500' : 'border-slate-300 dark:border-slate-600', className)}
        {...resto}
      />
      {error && (
        <p id={idError} className="mt-1 text-sm text-red-600 dark:text-red-400">
          {error}
        </p>
      )}
    </div>
  )
}
