import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import Boton from '../componentes/Boton'
import Campo from '../componentes/Campo'
import { EstadoVacio } from '../componentes/Estados'
import Tarjeta from '../componentes/Tarjeta'
import { leerEventosCreados } from '../lib/eventosSesion'
import { RUTAS } from '../lib/rutas'

export default function AdminEventos() {
  const navigate = useNavigate()
  const [codigo, setCodigo] = useState('')
  const [creados] = useState(leerEventosCreados)

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-3xl font-bold">Administrar eventos</h1>
        <Link to={RUTAS.adminNuevo} className="rounded-lg bg-marca-600 px-4 py-3 text-sm font-semibold text-white">Crear evento</Link>
      </div>
      <Tarjeta>
        <form
          className="flex items-end gap-3"
          onSubmit={(e) => {
            e.preventDefault()
            if (codigo.trim()) void navigate(RUTAS.adminEvento(codigo.trim()))
          }}
        >
          <div className="flex-1">
            <Campo etiqueta="Buscar evento por código" value={codigo} onChange={(e) => setCodigo(e.target.value)} />
          </div>
          <Boton type="submit">Abrir</Boton>
        </form>
      </Tarjeta>
      <section>
        <h2 className="mb-2 text-xl font-bold">Creados en esta sesión</h2>
        {creados.length === 0 ? (
          <EstadoVacio mensaje="Aún no creaste eventos en esta sesión. El API no lista borradores: búscalos por código." />
        ) : (
          <ul className="space-y-2">
            {creados.map((e) => (
              <li key={e.eventCode}>
                <Link to={RUTAS.adminEvento(e.eventCode)} className="block rounded-lg bg-white p-3 shadow-sm hover:shadow-md dark:bg-slate-900">
                  <span className="font-mono text-sm">{e.eventCode}</span> · {e.name}
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
