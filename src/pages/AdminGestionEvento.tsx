import { useState } from 'react'
import { useParams } from 'react-router'
import Aviso from '../componentes/Aviso'
import { useAvisos } from '../componentes/Avisos'
import { BadgeEvento } from '../componentes/Badge'
import Boton from '../componentes/Boton'
import { Cargando, ErrorDeCarga } from '../componentes/Estados'
import Modal from '../componentes/Modal'
import Selector from '../componentes/Selector'
import Tarjeta from '../componentes/Tarjeta'
import { useArtistasActivos } from '../funcionalidades/artistas/hooks'
import { useAsociarArtista, useEvento, usePublicarEvento } from '../funcionalidades/eventos/hooks'
import { formatoFechaHora } from '../lib/formato'
import { mensajeDeError } from '../lib/http'

export default function AdminGestionEvento() {
  const { eventCode = '' } = useParams()
  const { notificar } = useAvisos()
  const evento = useEvento(eventCode)
  const artistas = useArtistasActivos()
  const publicar = usePublicarEvento()
  const asociar = useAsociarArtista(eventCode)
  const [confirmar, setConfirmar] = useState(false)
  const [artistaId, setArtistaId] = useState('')

  if (evento.isPending) return <Cargando />
  if (evento.isError)
    return <ErrorDeCarga error={evento.error} noEncontrado="Evento no encontrado" onReintentar={() => void evento.refetch()} />

  const e = evento.data
  const yaAsociados = new Set((e.artists ?? []).map((a) => a.id))
  const disponibles = (artistas.data ?? []).filter((a) => !yaAsociados.has(a.id))

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div className="flex flex-wrap items-center gap-3">
        <h1 className="text-3xl font-bold">{e.name}</h1>
        <BadgeEvento estado={e.status} />
      </div>
      <Tarjeta className="space-y-1">
        <p><strong>Código:</strong> <span className="font-mono text-sm">{e.eventCode}</span></p>
        <p><strong>Fecha:</strong> {formatoFechaHora(e.eventDate)}</p>
        {e.venueCode && <p><strong>Sede:</strong> {e.venueName ?? e.venueCode}</p>}
        {e.minimumAge != null && <p><strong>Edad mínima:</strong> {e.minimumAge}</p>}
      </Tarjeta>

      <Tarjeta className="space-y-3">
        <h2 className="text-xl font-bold">Publicación</h2>
        {publicar.error && <Aviso tono="error">{mensajeDeError(publicar.error)}</Aviso>}
        <Boton disabled={e.status !== 'DRAFT'} onClick={() => setConfirmar(true)}>Publicar evento</Boton>
        {e.status !== 'DRAFT' && <p className="text-sm text-slate-500">Solo se pueden publicar eventos en borrador.</p>}
      </Tarjeta>

      <Tarjeta className="space-y-3">
        <h2 className="text-xl font-bold">Artistas</h2>
        {e.artists && e.artists.length > 0 ? (
          <ul className="list-disc pl-5">{e.artists.map((a) => <li key={a.id}>{a.stageName}</li>)}</ul>
        ) : (
          <p className="text-slate-500">Sin artistas asociados.</p>
        )}
        {asociar.error && <Aviso tono="error">{mensajeDeError(asociar.error)}</Aviso>}
        <div className="flex items-end gap-3">
          <div className="flex-1">
            <Selector etiqueta="Asociar artista" value={artistaId} onChange={(ev) => setArtistaId(ev.target.value)}>
              <option value="">Elige un artista</option>
              {disponibles.map((a) => <option key={a.id} value={a.id}>{a.stageName}</option>)}
            </Selector>
          </div>
          <Boton
            cargando={asociar.isPending}
            disabled={!artistaId}
            onClick={() =>
              asociar.mutate(Number(artistaId), {
                onSuccess: () => {
                  setArtistaId('')
                  notificar('exito', 'Artista asociado.')
                },
              })
            }
          >
            Asociar
          </Boton>
        </div>
      </Tarjeta>

      <Modal abierto={confirmar} titulo="Publicar evento" onCerrar={() => setConfirmar(false)}>
        <p className="mb-4 text-sm">Al publicarlo, {e.name} será visible para todo el público.</p>
        <div className="flex justify-end gap-2">
          <Boton variante="secundario" onClick={() => setConfirmar(false)}>Volver</Boton>
          <Boton
            cargando={publicar.isPending}
            onClick={() =>
              publicar.mutate(eventCode, {
                onSuccess: () => {
                  setConfirmar(false)
                  notificar('exito', 'Evento publicado.')
                },
                onError: () => setConfirmar(false),
              })
            }
          >
            Confirmar
          </Boton>
        </div>
      </Modal>
    </div>
  )
}
