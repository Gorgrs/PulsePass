import type { ReactNode } from 'react'

interface Columna<T> {
  encabezado: string
  celda: (fila: T) => ReactNode
}

interface TablaProps<T> {
  columnas: Columna<T>[]
  filas: T[]
  clave: (fila: T) => string
}

// Tabla en escritorio; en móvil cada fila se convierte en tarjeta
export default function Tabla<T>({ columnas, filas, clave }: TablaProps<T>) {
  return (
    <>
      <table className="hidden w-full text-left text-sm md:table">
        <thead>
          <tr className="border-b border-slate-200 text-slate-500 dark:border-slate-700">
            {columnas.map((c) => (
              <th key={c.encabezado} scope="col" className="px-3 py-2 font-semibold">
                {c.encabezado}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {filas.map((f) => (
            <tr key={clave(f)} className="border-b border-slate-100 dark:border-slate-800">
              {columnas.map((c) => (
                <td key={c.encabezado} className="px-3 py-3">
                  {c.celda(f)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
      <ul className="space-y-3 md:hidden">
        {filas.map((f) => (
          <li key={clave(f)} className="space-y-1 rounded-lg border border-slate-200 p-3 dark:border-slate-700">
            {columnas.map((c) => (
              <div key={c.encabezado} className="flex justify-between gap-3 text-sm">
                <span className="text-slate-500">{c.encabezado}</span>
                <span className="text-right">{c.celda(f)}</span>
              </div>
            ))}
          </li>
        ))}
      </ul>
    </>
  )
}
