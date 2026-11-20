import { Cargando, ErrorDeCarga } from '../componentes/Estados'
import Tarjeta from '../componentes/Tarjeta'
import { useAuth } from '../funcionalidades/auth/AuthContext'
import { useUsuarioPorEmail } from '../funcionalidades/usuarios/hooks'
import { formatoFecha } from '../lib/formato'

export default function Cuenta() {
  const { sesion } = useAuth()
  const email = sesion.estado === 'autenticado' ? sesion.usuario.email : undefined
  const perfil = useUsuarioPorEmail(email)

  if (sesion.estado !== 'autenticado') return null
  return (
    <div className="mx-auto max-w-xl space-y-4">
      <h1 className="text-3xl font-bold">Mi cuenta</h1>
      <Tarjeta className="space-y-1">
        <p><strong>Usuario:</strong> {sesion.usuario.username}</p>
        <p><strong>Correo:</strong> {sesion.usuario.email}</p>
        <p><strong>Roles:</strong> {sesion.usuario.roles.join(', ')}</p>
      </Tarjeta>
      {perfil.isPending && <Cargando />}
      {perfil.isError && <ErrorDeCarga error={perfil.error} onReintentar={() => void perfil.refetch()} />}
      {perfil.data && (
        <Tarjeta className="space-y-1">
          <p><strong>Nombre:</strong> {perfil.data.firstName} {perfil.data.lastName}</p>
          {perfil.data.birthDate && <p><strong>Nacimiento:</strong> {formatoFecha(perfil.data.birthDate)}</p>}
          {perfil.data.city && <p><strong>Ciudad:</strong> {perfil.data.city}</p>}
          {perfil.data.phone && <p><strong>Teléfono:</strong> {perfil.data.phone}</p>}
        </Tarjeta>
      )}
    </div>
  )
}
