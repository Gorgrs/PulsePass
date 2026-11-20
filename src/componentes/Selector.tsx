import clsx from 'clsx'
import { useId, type ComponentProps } from 'react'
import { CLASES_CAMPO } from './Campo'

interface SelectorProps extends ComponentProps<'select'> {
  etiqueta: string
  error?: string
}

export default function Selector({ etiqueta, error, className, children, ...resto }: SelectorProps) {
  const id = useId()
  const idError = `${id}-error`
  return (
    <div>
      <label htmlFor={id} className="text-sm font-medium text-slate-700 dark:text-slate-300">
        {etiqueta}
      </label>
      <select
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? idError : undefined}
        className={clsx(CLASES_CAMPO, error ? 'border-red-500' : 'border-slate-300 dark:border-slate-600', className)}
        {...resto}
      >
        {children}
      </select>
      {error && (
        <p id={idError} className="mt-1 text-sm text-red-600 dark:text-red-400">
          {error}
        </p>
      )}
    </div>
  )
}
