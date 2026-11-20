import { Navigate, Outlet, useLocation } from 'react-router'
import { Cargando } from '../componentes/Estados'
import { useAuth } from '../funcionalidades/auth/AuthContext'
import type { Rol } from '../funcionalidades/auth/tipos'
import { RUTAS } from '../lib/rutas'

export default function RutaPorRol({ roles }: { roles: Rol[] }) {
  const { sesion } = useAuth()
  const location = useLocation()
  if (sesion.estado === 'cargando') return <Cargando />
  if (sesion.estado === 'anonimo')
    return <Navigate to={RUTAS.login} replace state={{ desde: location.pathname }} />
  if (!roles.some((r) => sesion.usuario.roles.includes(r))) return <Navigate to={RUTAS.sinPermisos} replace />
  return <Outlet />
}
