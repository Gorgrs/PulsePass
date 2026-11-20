import clsx from 'clsx'
import type { ComponentProps } from 'react'

export default function Tarjeta({ className, ...resto }: ComponentProps<'div'>) {
  return <div className={clsx('rounded-xl bg-white p-5 shadow-sm dark:bg-slate-900', className)} {...resto} />
}
