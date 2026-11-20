import { request } from '../../lib/http'
import { UsuarioSchema, type RegistroForm } from './tipos'

export const registrarUsuario = (datos: RegistroForm) => {
  const { phone, city, ...base } = datos
  return request('/users', {
    metodo: 'POST',
    cuerpo: { ...base, ...(phone ? { phone } : {}), ...(city ? { city } : {}) },
    schema: UsuarioSchema,
  })
}
export const obtenerUsuarioPorEmail = (email: string) =>
  request(`/users/by-email?email=${encodeURIComponent(email)}`, { schema: UsuarioSchema })
export const obtenerUsuarioPorUsername = (username: string) =>
  request(`/users/by-username?username=${encodeURIComponent(username)}`, { schema: UsuarioSchema })
