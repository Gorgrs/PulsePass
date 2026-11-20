import { useParams } from 'react-router'
import { Cargando, EstadoVacio, ErrorDeCarga } from '../componentes/Estados'
import { useArtista } from '../funcionalidades/artistas/hooks'
import TarjetaEvento from '../funcionalidades/eventos/componentes/TarjetaEvento'
import { useEventosPorArtista } from '../funcionalidades/eventos/hooks'
import NoEncontrada from './NoEncontrada'

export default function DetalleArtista() {
  const { id } = useParams()
  const numero = Number(id)
  const artista = useArtista(numero)
  const eventos = useEventosPorArtista(artista.data?.stageName)

  // id no numérico: 404 de UI sin llamar a la API
  if (!Number.isInteger(numero)) return <NoEncontrada />
  if (artista.isPending) return <Cargando />
  if (artista.isError) return <ErrorDeCarga error={artista.error} noEncontrado="Artista no encontrado" onReintentar={() => void artista.refetch()} />

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">{artista.data.stageName}</h1>
      <h2 className="text-xl font-bold">Eventos</h2>
      {eventos.isPending && <Cargando />}
      {eventos.data && eventos.data.length === 0 && <EstadoVacio mensaje="Sin eventos próximos" />}
      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {eventos.data?.map((e) => <TarjetaEvento key={e.eventCode} evento={e} />)}
      </div>
    </div>
  )
}
