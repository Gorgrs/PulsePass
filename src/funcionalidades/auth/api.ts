import { request } from '../../lib/http'
import { AuthResponseSchema, MeSchema, type LoginForm } from './tipos'

export const iniciarSesionApi = (datos: LoginForm) =>
  request('/auth/login', { metodo: 'POST', cuerpo: datos, schema: AuthResponseSchema })
export const obtenerYo = () => request('/auth/me', { schema: MeSchema })
