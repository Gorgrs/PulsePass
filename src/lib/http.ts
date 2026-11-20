import { z } from 'zod'

export class ApiError extends Error {
  status: number
  details: Record<string, string>

  constructor(status: number, message: string, details: Record<string, string> = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.details = details
  }
}

const ErrorResponseSchema = z.looseObject({
  status: z.number().optional(),
  message: z.string(),
  details: z.record(z.string(), z.string()).nullish(),
})

const BASE: string = import.meta.env.VITE_API_BASE_URL ?? '/api'
export const MENSAJE_INESPERADO = 'Ocurrió un error inesperado. Inténtalo más tarde.'

let tokenActual: string | null = null
let alNoAutorizado: (() => void) | null = null

export function fijarToken(token: string | null): void {
  tokenActual = token
}

export function registrarAlNoAutorizado(fn: (() => void) | null): void {
  alNoAutorizado = fn
}

interface Opciones<T> {
  metodo?: 'GET' | 'POST' | 'PATCH'
  cuerpo?: unknown
  schema: z.ZodType<T>
}

export async function request<T>(ruta: string, opciones: Opciones<T>): Promise<T> {
  const { metodo = 'GET', cuerpo, schema } = opciones
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (cuerpo !== undefined) headers['Content-Type'] = 'application/json'
  const token = tokenActual
  if (token) headers.Authorization = `Bearer ${token}`

  let respuesta: Response
  try {
    respuesta = await fetch(`${BASE}${ruta}`, {
      method: metodo,
      headers,
      body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined,
    })
  } catch {
    throw new ApiError(0, 'No pudimos conectar con el servidor.')
  }

  if (!respuesta.ok) {
    let mensaje = MENSAJE_INESPERADO
    let details: Record<string, string> = {}
    try {
      const cuerpoError = ErrorResponseSchema.safeParse(await respuesta.json())
      if (cuerpoError.success && respuesta.status < 500) {
        mensaje = cuerpoError.data.message
        details = cuerpoError.data.details ?? {}
      }
    } catch {
      // el cuerpo no era JSON: se usa el mensaje genérico
    }
    // Un 401 con token presente significa sesión vencida o inválida
    if (respuesta.status === 401 && token) alNoAutorizado?.()
    throw new ApiError(respuesta.status, mensaje, details)
  }

  const datos: unknown = await respuesta.json()
  return schema.parse(datos)
}

export function mensajeDeError(error: unknown): string {
  return error instanceof ApiError ? error.message : MENSAJE_INESPERADO
}
