import { useSearchParams } from 'react-router'
import { Cargando, EstadoVacio, ErrorDeCarga } from '../componentes/Estados'
import Selector from '../componentes/Selector'
import Campo from '../componentes/Campo'
import { useArtistasActivos } from '../funcionalidades/artistas/hooks'
import TarjetaEvento from '../funcionalidades/eventos/componentes/TarjetaEvento'
import { useEventosPorArtista, useEventosPublicados } from '../funcionalidades/eventos/hooks'
import { CATEGORIAS, EventCategorySchema } from '../funcionalidades/eventos/tipos'

export default function CatalogoEventos() {
  const [params, setParams] = useSearchParams()
  const q = params.get('q') ?? ''
  const categoria = params.get('categoria') ?? ''
  const artista = params.get('artista') ?? ''

  const publicados = useEventosPublicados()
  const porArtista = useEventosPorArtista(artista || undefined)
  const artistas = useArtistasActivos()
  const consulta = artista ? porArtista : publicados

  const cambiar = (clave: string, valor: string) =>
    setParams((prev) => {
      const siguiente = new URLSearchParams(prev)
      if (valor) siguiente.set(clave, valor)
      else siguiente.delete(clave)
      return siguiente
    })

  const filtrados = (consulta.data ?? []).filter(
    (e) =>
      (!q || e.name.toLowerCase().includes(q.toLowerCase())) && (!categoria || e.category === categoria),
  )

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold">Cartelera</h1>
      <div className="grid gap-4 rounded-xl bg-white p-4 shadow-sm sm:grid-cols-3 dark:bg-slate-900">
        <Campo etiqueta="Buscar" type="search" value={q} onChange={(e) => cambiar('q', e.target.value)} />
        <Selector etiqueta="Categoría" value={categoria} onChange={(e) => cambiar('categoria', e.target.value)}>
          <option value="">Todas</option>
          {EventCategorySchema.options.map((c) => (
            <option key={c} value={c}>{CATEGORIAS[c].etiqueta}</option>
          ))}
        </Selector>
        <Selector etiqueta="Artista" value={artista} onChange={(e) => cambiar('artista', e.target.value)}>
          <option value="">Todos</option>
          {artistas.data?.map((a) => (
            <option key={a.id} value={a.stageName}>{a.stageName}</option>
          ))}
        </Selector>
      </div>

      {consulta.isFetching && !consulta.data && <Cargando />}
      {consulta.isError && <ErrorDeCarga error={consulta.error} onReintentar={() => void consulta.refetch()} />}
      {consulta.data && filtrados.length === 0 && <EstadoVacio mensaje="Sin resultados para tu búsqueda" />}
      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {filtrados.map((e) => (
          <TarjetaEvento key={e.eventCode} evento={e} />
        ))}
      </div>
    </div>
  )
}
