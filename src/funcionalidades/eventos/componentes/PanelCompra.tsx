import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useLocation, useNavigate } from 'react-router'
import Aviso from '../../../componentes/Aviso'
import Boton from '../../../componentes/Boton'
import Modal from '../../../componentes/Modal'
import Selector from '../../../componentes/Selector'
import { mensajeDeError } from '../../../lib/http'
import { RUTAS } from '../../../lib/rutas'
import { useAuth } from '../../auth/AuthContext'
import { CompraSchema, TIPOS_TICKET, TicketTypeSchema, type CompraForm } from '../../tickets/tipos'
import { useComprarTicket } from '../../tickets/hooks'
import type { Evento } from '../tipos'

export default function PanelCompra({ evento }: { evento: Evento }) {
  const { sesion, tieneRol } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [confirmar, setConfirmar] = useState(false)
  const comprar = useComprarTicket()
  const { register, handleSubmit, getValues, formState } = useForm<CompraForm>({
    resolver: zodResolver(CompraSchema),
    defaultValues: { type: 'GENERAL' },
  })

  if (sesion.estado === 'cargando') return null
  if (sesion.estado === 'anonimo') {
    return (
      <Link
        to={RUTAS.login}
        state={{ desde: location.pathname }}
        className="block rounded-lg bg-marca-600 px-4 py-3 text-center text-sm font-semibold text-white hover:bg-marca-700"
      >
        Inicia sesión para comprar
      </Link>
    )
  }
  // STAFF y ADMIN no compran desde la UI (la API autoriza la compra a USER)
  if (tieneRol('STAFF', 'ADMIN')) return null

  const disponible = evento.status === 'PUBLISHED'

  const confirmarCompra = () => {
    comprar.mutate(
      { userEmail: sesion.usuario.email, eventCode: evento.eventCode, type: TicketTypeSchema.parse(getValues('type')) },
      { onSuccess: (ticket) => void navigate(RUTAS.confirmacion(ticket.ticketCode)) },
    )
  }

  return (
    <div className="fixed inset-x-0 bottom-0 z-30 space-y-3 rounded-t-xl bg-white p-4 shadow-2xl md:static md:rounded-xl md:shadow-sm dark:bg-slate-900">
      <h2 className="text-lg font-bold">Comprar entrada</h2>
      <form onSubmit={handleSubmit(() => setConfirmar(true))} className="space-y-3">
        <Selector etiqueta="Tipo de entrada" error={formState.errors.type?.message} {...register('type')}>
          {TicketTypeSchema.options.map((t) => (
            <option key={t} value={t}>
              {TIPOS_TICKET[t]}
            </option>
          ))}
        </Selector>
        <Boton type="submit" disabled={!disponible} className="w-full">
          Comprar entrada
        </Boton>
        {!disponible && <p className="text-sm text-slate-500">Este evento no está disponible para la compra.</p>}
      </form>

      <Modal abierto={confirmar} titulo="Confirmar compra" onCerrar={() => setConfirmar(false)}>
        <p className="mb-4 text-sm text-slate-600 dark:text-slate-300">
          Comprarás una entrada <strong>{TIPOS_TICKET[TicketTypeSchema.catch('GENERAL').parse(getValues('type'))]}</strong> para{' '}
          <strong>{evento.name}</strong> con la cuenta {sesion.usuario.email}.
        </p>
        {comprar.error && (
          <div className="mb-4">
            <Aviso tono="error">{mensajeDeError(comprar.error)}</Aviso>
          </div>
        )}
        <div className="flex justify-end gap-2">
          <Boton variante="secundario" onClick={() => setConfirmar(false)}>
            Cancelar
          </Boton>
          <Boton cargando={comprar.isPending} onClick={confirmarCompra}>
            Confirmar compra
          </Boton>
        </div>
      </Modal>
    </div>
  )
}
