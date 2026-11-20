import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import Aviso from '../componentes/Aviso'
import { BadgeTicket } from '../componentes/Badge'
import Boton from '../componentes/Boton'
import Campo from '../componentes/Campo'
import CodigoTexto from '../componentes/CodigoTexto'
import { Cargando } from '../componentes/Estados'
import PrecioCOP from '../componentes/PrecioCOP'
import Tarjeta from '../componentes/Tarjeta'
import { useTicket, useUsarTicket } from '../funcionalidades/tickets/hooks'
import { TIPOS_TICKET } from '../funcionalidades/tickets/tipos'
import { ApiError, mensajeDeError } from '../lib/http'

const BusquedaSchema = z.object({ ticketCode: z.string().trim().min(1, 'Ingresa el código del ticket') })
type BusquedaForm = z.infer<typeof BusquedaSchema>

export default function ValidacionIngreso() {
  const [codigo, setCodigo] = useState<string | undefined>()
  const [registrado, setRegistrado] = useState(false)
  const consulta = useTicket(codigo)
  const usar = useUsarTicket()
  const { register, handleSubmit, reset, setFocus, formState } = useForm<BusquedaForm>({
    resolver: zodResolver(BusquedaSchema),
  })

  const buscar = (datos: BusquedaForm) => {
    setRegistrado(false)
    usar.reset()
    setCodigo(datos.ticketCode)
  }

  const registrarIngreso = () => {
    if (!codigo) return
    usar.mutate(codigo, {
      onSuccess: () => {
        setRegistrado(true)
        reset()
        setFocus('ticketCode')
      },
    })
  }

  const t = consulta.data
  return (
    <div className="mx-auto max-w-xl space-y-6">
      <h1 className="text-3xl font-bold">Validación de ingreso</h1>
      <form onSubmit={handleSubmit(buscar)} className="flex items-end gap-3" noValidate>
        <div className="flex-1">
          <Campo etiqueta="Código del ticket" autoFocus placeholder="TKT-XXXXXXXX" error={formState.errors.ticketCode?.message} {...register('ticketCode')} />
        </div>
        <Boton type="submit">Buscar</Boton>
      </form>

      {codigo && consulta.isFetching && !t && <Cargando />}
      {consulta.isError &&
        (consulta.error instanceof ApiError && consulta.error.status === 404 ? (
          <Aviso tono="error">Ticket no encontrado</Aviso>
        ) : consulta.error instanceof ApiError && consulta.error.status === 403 ? (
          <Aviso tono="error">Sin permisos</Aviso>
        ) : (
          <Aviso tono="error">{mensajeDeError(consulta.error)}</Aviso>
        ))}

      {registrado && <Aviso tono="exito">Ingreso registrado.</Aviso>}
      {usar.error && <Aviso tono="advertencia">{mensajeDeError(usar.error)}</Aviso>}

      {t && (
        <Tarjeta className="space-y-2">
          <div className="flex items-center justify-between">
            <CodigoTexto>{t.ticketCode}</CodigoTexto>
            <BadgeTicket estado={t.status} />
          </div>
          <p className="text-lg font-semibold">{t.eventName ?? t.eventCode}</p>
          <p>{TIPOS_TICKET[t.type]} · <PrecioCOP valor={t.price} /></p>
          {t.userEmail && <p className="text-sm text-slate-600 dark:text-slate-400">{t.userEmail}</p>}
          <Boton cargando={usar.isPending} disabled={t.status !== 'PAID'} onClick={registrarIngreso} className="w-full">
            Registrar ingreso
          </Boton>
        </Tarjeta>
      )}
    </div>
  )
}
