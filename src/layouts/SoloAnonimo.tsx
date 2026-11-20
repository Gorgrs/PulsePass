import { Navigate, Outlet } from 'react-router'
import { Cargando } from '../componentes/Estados'
import { useAuth } from '../funcionalidades/auth/AuthContext'
import { RUTAS } from '../lib/rutas'

export default function SoloAnonimo() {
  const { sesion } = useAuth()
  if (sesion.estado === 'cargando') return <Cargando />
  if (sesion.estado === 'autenticado') return <Navigate to={RUTAS.inicio} replace />
  return <Outlet />
}
