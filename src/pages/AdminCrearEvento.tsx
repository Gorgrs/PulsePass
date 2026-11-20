import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router'
import Aviso from '../componentes/Aviso'
import { useAvisos } from '../componentes/Avisos'
import Boton from '../componentes/Boton'
import Campo from '../componentes/Campo'
import Selector from '../componentes/Selector'
import Tarjeta from '../componentes/Tarjeta'
import { useCrearEvento } from '../funcionalidades/eventos/hooks'
import { CATEGORIAS, CrearEventoSchema, EventCategorySchema, type CrearEventoForm } from '../funcionalidades/eventos/tipos'
import { useSedesActivas } from '../funcionalidades/sedes/hooks'
import { ApiError } from '../lib/http'
import { aplicarErroresServidor } from '../lib/erroresFormulario'
import { recordarEventoCreado } from '../lib/eventosSesion'
import { RUTAS } from '../lib/rutas'

const CAMPOS = ['eventCode', 'name', 'description', 'category', 'eventDate', 'minimumAge', 'venueCode'] as const

export default function AdminCrearEvento() {
  const navigate = useNavigate()
  const { notificar } = useAvisos()
  const sedes = useSedesActivas()
  const crear = useCrearEvento()
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const { register, handleSubmit, setError, formState } = useForm<CrearEventoForm>({
    resolver: zodResolver(CrearEventoSchema),
    defaultValues: { category: 'MUSIC', description: '', minimumAge: 0, venueCode: '' },
  })
  const e = formState.errors

  const enviar = (datos: CrearEventoForm) => {
    setErrorGeneral(null)
    crear.mutate(datos, {
      onSuccess: (evento) => {
        recordarEventoCreado({ eventCode: evento.eventCode, name: evento.name })
        notificar('exito', 'Evento creado como borrador.')
        void navigate(RUTAS.adminEvento(evento.eventCode))
      },
      onError: (error) => {
        if (error instanceof ApiError && error.status === 403) {
          void navigate(RUTAS.sinPermisos)
          return
        }
        setErrorGeneral(aplicarErroresServidor<CrearEventoForm>(error, setError, CAMPOS))
      },
    })
  }

  return (
    <Tarjeta className="mx-auto max-w-3xl space-y-4">
      <h1 className="text-2xl font-bold">Crear evento</h1>
      {errorGeneral && <Aviso tono="error">{errorGeneral}</Aviso>}
      <form onSubmit={handleSubmit(enviar)} className="grid gap-4 sm:grid-cols-2" noValidate>
        <Campo etiqueta="Código" error={e.eventCode?.message} {...register('eventCode')} />
        <Campo etiqueta="Nombre" error={e.name?.message} {...register('name')} />
        <Selector etiqueta="Categoría" error={e.category?.message} {...register('category')}>
          {EventCategorySchema.options.map((c) => (
            <option key={c} value={c}>{CATEGORIAS[c].etiqueta}</option>
          ))}
        </Selector>
        <Campo etiqueta="Fecha y hora" type="datetime-local" error={e.eventDate?.message} {...register('eventDate')} />
        <Campo etiqueta="Edad mínima" type="number" min={0} error={e.minimumAge?.message} {...register('minimumAge', { valueAsNumber: true })} />
        <Selector etiqueta="Sede" error={e.venueCode?.message} {...register('venueCode')}>
          <option value="">Elige una sede</option>
          {sedes.data?.map((s) => (
            <option key={s.code} value={s.code}>{s.name} ({s.city})</option>
          ))}
        </Selector>
        <div className="sm:col-span-2">
          <label htmlFor="descripcion" className="text-sm font-medium text-slate-700 dark:text-slate-300">Descripción</label>
          <textarea
            id="descripcion"
            rows={3}
            className="mt-1 block w-full rounded-lg border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900"
            {...register('description')}
          />
          {e.description && <p className="mt-1 text-sm text-red-600">{e.description.message}</p>}
        </div>
        <div className="sm:col-span-2">
          <Boton type="submit" cargando={crear.isPending} className="w-full">Crear evento</Boton>
        </div>
      </form>
    </Tarjeta>
  )
}
