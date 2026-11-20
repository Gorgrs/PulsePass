import { useState } from 'react'
import Aviso from '../../../componentes/Aviso'
import { useAvisos } from '../../../componentes/Avisos'
import Boton from '../../../componentes/Boton'
import Modal from '../../../componentes/Modal'
import { mensajeDeError } from '../../../lib/http'
import { useAuth } from '../../auth/AuthContext'
import { useCancelarTicket } from '../hooks'
import type { Ticket } from '../tipos'

// "Cancelar" solo se ofrece si el ticket está PAID; el backend sigue siendo la autoridad
export default function AccionCancelar({ ticket }: { ticket: Ticket }) {
  const { sesion } = useAuth()
  const { notificar } = useAvisos()
  const email = sesion.estado === 'autenticado' ? sesion.usuario.email : undefined
  const [abierto, setAbierto] = useState(false)
  const cancelar = useCancelarTicket(email)

  if (ticket.status !== 'PAID') return null

  const cerrar = () => {
    setAbierto(false)
    cancelar.reset()
  }

  return (
    <>
      <Boton variante="peligro" onClick={() => setAbierto(true)}>
        Cancelar ticket
      </Boton>
      <Modal abierto={abierto} titulo="Cancelar ticket" onCerrar={cerrar}>
        <p className="mb-4 text-sm">¿Seguro que quieres cancelar el ticket {ticket.ticketCode}?</p>
        {cancelar.error && (
          <div className="mb-4">
            <Aviso tono="error">{mensajeDeError(cancelar.error)}</Aviso>
          </div>
        )}
        <div className="flex justify-end gap-2">
          <Boton variante="secundario" onClick={cerrar}>
            Volver
          </Boton>
          <Boton
            variante="peligro"
            cargando={cancelar.isPending}
            onClick={() =>
              cancelar.mutate(ticket.ticketCode, {
                onSuccess: () => {
                  setAbierto(false)
                  notificar('exito', 'Ticket cancelado.')
                },
              })
            }
          >
            Sí, cancelar
          </Boton>
        </div>
      </Modal>
    </>
  )
}
