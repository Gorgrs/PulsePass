import { useParams } from 'react-router'
import { Cargando, ErrorDeCarga } from '../componentes/Estados'
import CodigoTexto from '../componentes/CodigoTexto'
import Tarjeta from '../componentes/Tarjeta'
import { useSede } from '../funcionalidades/sedes/hooks'

export default function DetalleSede() {
  const { code } = useParams()
  const consulta = useSede(code)
  if (consulta.isPending) return <Cargando />
  if (consulta.isError) return <ErrorDeCarga error={consulta.error} noEncontrado="Sede no encontrada" onReintentar={() => void consulta.refetch()} />
  const s = consulta.data
  return (
    <Tarjeta className="max-w-xl space-y-2">
      <h1 className="text-3xl font-bold">{s.name}</h1>
      <p>Código: <CodigoTexto>{s.code}</CodigoTexto></p>
      <p>Ciudad: {s.city}</p>
      <p>Capacidad: {s.capacity} personas</p>
    </Tarjeta>
  )
}
