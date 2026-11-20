import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import Aviso from './Aviso'

type Tipo = 'exito' | 'error' | 'info'
interface Item {
  id: number
  tipo: Tipo
  mensaje: string
}
interface AvisosValor {
  notificar: (tipo: Tipo, mensaje: string) => void
}

const AvisosContext = createContext<AvisosValor | null>(null)
let contador = 0

export function AvisosProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<Item[]>([])

  const cerrar = useCallback((id: number) => setItems((prev) => prev.filter((i) => i.id !== id)), [])
  const notificar = useCallback(
    (tipo: Tipo, mensaje: string) => {
      const id = ++contador
      setItems((prev) => [...prev, { id, tipo, mensaje }])
      if (tipo === 'exito') window.setTimeout(() => cerrar(id), 4000)
    },
    [cerrar],
  )
  const valor = useMemo(() => ({ notificar }), [notificar])

  return (
    <AvisosContext.Provider value={valor}>
      {children}
      <div aria-live="polite" className="fixed inset-x-4 top-20 z-50 mx-auto flex max-w-md flex-col gap-2">
        {items.map((i) => (
          <div key={i.id} className="flex items-start gap-2">
            <div className="flex-1">
              <Aviso tono={i.tipo}>{i.mensaje}</Aviso>
            </div>
            {i.tipo === 'error' && (
              <button
                type="button"
                onClick={() => cerrar(i.id)}
                className="min-h-11 rounded px-2 text-sm font-semibold text-slate-700 dark:text-slate-200"
              >
                Cerrar
              </button>
            )}
          </div>
        ))}
      </div>
    </AvisosContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAvisos(): AvisosValor {
  const ctx = useContext(AvisosContext)
  if (!ctx) throw new Error('useAvisos debe usarse dentro de AvisosProvider')
  return ctx
}
