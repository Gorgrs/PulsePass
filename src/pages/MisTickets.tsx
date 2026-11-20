import clsx from 'clsx'
import { useState } from 'react'
import { Link } from 'react-router'
import { Cargando, EstadoVacio, ErrorDeCarga } from '../componentes/Estados'
import { useAuth } from '../funcionalidades/auth/AuthContext'
import TarjetaTicket from '../funcionalidades/tickets/componentes/TarjetaTicket'
import { useMisTickets } from '../funcionalidades/tickets/hooks'
import type { TicketStatus } from '../funcionalidades/tickets/tipos'
import { RUTAS } from '../lib/rutas'

type Filtro = 'TODOS' | Extract<TicketStatus, 'PAID' | 'USED' | 'CANCELLED'>

const FILTROS: { valor: Filtro; etiqueta: string }[] = [
  { valor: 'TODOS', etiqueta: 'Todos' },
  { valor: 'PAID', etiqueta: 'Pagados' },
  { valor: 'USED', etiqueta: 'Usados' },
  { valor: 'CANCELLED', etiqueta: 'Cancelados' },
]

export default function MisTickets() {
  const { sesion } = useAuth()
  const email = sesion.estado === 'autenticado' ? sesion.usuario.email : undefined
  const consulta = useMisTickets(email)
  const [filtro, setFiltro] = useState<Filtro>('TODOS')

  const visibles = (consulta.data ?? []).filter((t) => filtro === 'TODOS' || t.status === filtro)

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">Mis tickets</h1>
      <div className="flex flex-wrap gap-2" role="group" aria-label="Filtrar por estado">
        {FILTROS.map((f) => (
          <button
            key={f.valor}
            type="button"
            aria-pressed={filtro === f.valor}
            onClick={() => setFiltro(f.valor)}
            className={clsx(
              'min-h-11 rounded-full px-4 text-sm font-semibold',
              filtro === f.valor ? 'bg-marca-600 text-white' : 'bg-white text-slate-700 shadow-sm dark:bg-slate-900 dark:text-slate-200',
            )}
          >
            {f.etiqueta}
          </button>
        ))}
      </div>
      {consulta.isPending && <Cargando />}
      {consulta.isError && <ErrorDeCarga error={consulta.error} onReintentar={() => void consulta.refetch()} />}
      {consulta.data && visibles.length === 0 && (
        <EstadoVacio
          mensaje="Aún no tienes tickets"
          accion={<Link to={RUTAS.eventos} className="font-semibold text-marca-700 hover:underline dark:text-marca-500">Ver cartelera</Link>}
        />
      )}
      <div className="grid gap-4 md:grid-cols-2">
        {visibles.map((t) => <TarjetaTicket key={t.ticketCode} ticket={t} />)}
      </div>
    </div>
  )
}
