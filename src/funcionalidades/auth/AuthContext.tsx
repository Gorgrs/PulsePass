import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { fijarToken, registrarAlNoAutorizado } from '../../lib/http'
import { obtenerUsuarioPorUsername } from '../usuarios/api'
import { iniciarSesionApi, obtenerYo } from './api'
import {
  SesionGuardadaSchema,
  normalizarRoles,
  type LoginForm,
  type Rol,
  type Sesion,
  type UsuarioAutenticado,
} from './tipos'

const CLAVE_SESION = 'pulsepass.sesion'

interface AuthValor {
  sesion: Sesion
  iniciarSesion: (datos: LoginForm) => Promise<void>
  cerrarSesion: (motivo?: 'expirada') => void
  tieneRol: (...roles: Rol[]) => boolean
}

const AuthContext = createContext<AuthValor | null>(null)

function leerGuardada(): { token: string; expiraEn: number } | null {
  try {
    const crudo = sessionStorage.getItem(CLAVE_SESION)
    if (!crudo) return null
    const analizado = SesionGuardadaSchema.safeParse(JSON.parse(crudo))
    if (!analizado.success || analizado.data.expiraEn <= Date.now()) return null
    return analizado.data
  } catch {
    return null
  }
}

function guardar(valor: { token: string; expiraEn: number } | null): void {
  try {
    if (valor) sessionStorage.setItem(CLAVE_SESION, JSON.stringify(valor))
    else sessionStorage.removeItem(CLAVE_SESION)
  } catch {
    // sin sessionStorage: la sesión vive solo en memoria
  }
}

async function cargarUsuario(): Promise<UsuarioAutenticado> {
  const yo = await obtenerYo()
  // Si /auth/me no trae el correo, se obtiene de la propia ficha del usuario
  const email = yo.email ?? (await obtenerUsuarioPorUsername(yo.username)).email
  return {
    id: String(yo.userId ?? yo.identificador ?? yo.id ?? ''),
    username: yo.username,
    email,
    roles: normalizarRoles(yo.roles),
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [sesion, setSesion] = useState<Sesion>({ estado: 'cargando' })

  const cerrarSesion = useCallback(
    (motivo?: 'expirada') => {
      fijarToken(null)
      guardar(null)
      queryClient.clear()
      setSesion({ estado: 'anonimo', motivo })
    },
    [queryClient],
  )

  // Restaurar la sesión al recargar
  useEffect(() => {
    const guardada = leerGuardada()
    if (!guardada) {
      guardar(null)
      setSesion({ estado: 'anonimo' })
      return
    }
    fijarToken(guardada.token)
    let activo = true
    cargarUsuario()
      .then((usuario) => {
        if (activo) setSesion({ estado: 'autenticado', ...guardada, usuario })
      })
      .catch(() => {
        if (activo) cerrarSesion()
      })
    return () => {
      activo = false
    }
  }, [cerrarSesion])

  // Cualquier 401 posterior cierra la sesión local
  useEffect(() => {
    registrarAlNoAutorizado(() => cerrarSesion('expirada'))
    return () => registrarAlNoAutorizado(null)
  }, [cerrarSesion])

  // Cierre programado al vencer el token
  useEffect(() => {
    if (sesion.estado !== 'autenticado') return
    const espera = Math.max(sesion.expiraEn - Date.now(), 0)
    const id = window.setTimeout(() => cerrarSesion('expirada'), espera)
    return () => window.clearTimeout(id)
  }, [sesion, cerrarSesion])

  const iniciarSesion = useCallback(
    async (datos: LoginForm) => {
      const auth = await iniciarSesionApi(datos)
      const guardada = { token: auth.accessToken, expiraEn: Date.now() + auth.expiresIn * 1000 }
      fijarToken(guardada.token)
      try {
        const usuario = await cargarUsuario()
        guardar(guardada)
        setSesion({ estado: 'autenticado', ...guardada, usuario })
      } catch (error) {
        fijarToken(null)
        throw error
      }
    },
    [],
  )

  const tieneRol = useCallback(
    (...roles: Rol[]) => sesion.estado === 'autenticado' && roles.some((r) => sesion.usuario.roles.includes(r)),
    [sesion],
  )

  const valor = useMemo(
    () => ({ sesion, iniciarSesion, cerrarSesion, tieneRol }),
    [sesion, iniciarSesion, cerrarSesion, tieneRol],
  )
  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthValor {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider')
  return ctx
}
