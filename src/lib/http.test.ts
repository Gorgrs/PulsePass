import { afterEach, describe, expect, it, vi } from 'vitest'
import { z } from 'zod'
import { ApiError, fijarToken, registrarAlNoAutorizado, request } from './http'

const respuesta = (cuerpo: unknown, status: number) =>
  new Response(JSON.stringify(cuerpo), { status, headers: { 'Content-Type': 'application/json' } })

afterEach(() => {
  vi.unstubAllGlobals()
  fijarToken(null)
  registrarAlNoAutorizado(null)
})

describe('request', () => {
  it('devuelve los datos validados con zod en un 200', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuesta({ ok: true }, 200)))
    await expect(request('/x', { schema: z.object({ ok: z.boolean() }) })).resolves.toEqual({ ok: true })
  })

  it('convierte un 409 en ApiError con el mensaje del backend', async () => {
    const cuerpo = { timestamp: 't', status: 409, error: 'Conflict', message: 'Ticket ya usado', details: {} }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuesta(cuerpo, 409)))
    const error = await request('/x', { schema: z.unknown() }).catch((e: unknown) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(409)
    expect((error as ApiError).message).toBe('Ticket ya usado')
  })

  it('conserva details en un 400', async () => {
    const cuerpo = { status: 400, message: 'Validation failed', details: { description: 'Requerida' } }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuesta(cuerpo, 400)))
    const error = (await request('/x', { schema: z.unknown() }).catch((e: unknown) => e)) as ApiError
    expect(error.details).toEqual({ description: 'Requerida' })
  })

  it('un fallo de red produce ApiError con status 0', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('fallo')))
    const error = (await request('/x', { schema: z.unknown() }).catch((e: unknown) => e)) as ApiError
    expect(error.status).toBe(0)
  })

  it('un 500 usa el mensaje genérico, sin detalles internos', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuesta({ message: 'NullPointerException' }, 500)))
    const error = (await request('/x', { schema: z.unknown() }).catch((e: unknown) => e)) as ApiError
    expect(error.message).not.toContain('NullPointer')
  })

  it('envía el Bearer y avisa ante un 401 con token', async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta({ message: 'No autorizado' }, 401))
    vi.stubGlobal('fetch', fetchMock)
    const alNoAutorizado = vi.fn()
    registrarAlNoAutorizado(alNoAutorizado)
    fijarToken('abc')
    await request('/x', { schema: z.unknown() }).catch(() => undefined)
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer abc')
    expect(alNoAutorizado).toHaveBeenCalledOnce()
  })

  it('un 401 sin token (login incorrecto) no cierra la sesión', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(respuesta({ message: 'Credenciales inválidas' }, 401)))
    const alNoAutorizado = vi.fn()
    registrarAlNoAutorizado(alNoAutorizado)
    await request('/x', { schema: z.unknown() }).catch(() => undefined)
    expect(alNoAutorizado).not.toHaveBeenCalled()
  })
})
