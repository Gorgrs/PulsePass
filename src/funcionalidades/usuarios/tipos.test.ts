import { describe, expect, it } from 'vitest'
import { RegistroSchema } from './tipos'

const valido = {
  username: 'andrea',
  email: 'andrea@example.com',
  firstName: 'Andrea',
  lastName: 'Gómez',
  phone: '',
  city: '',
  birthDate: '1995-05-10',
  password: 'UnaFraseSegura2026!',
}

describe('RegistroSchema', () => {
  it('acepta un registro válido', () => {
    expect(RegistroSchema.safeParse(valido).success).toBe(true)
  })
  it('rechaza contraseñas de menos de 12 caracteres', () => {
    expect(RegistroSchema.safeParse({ ...valido, password: 'corta' }).success).toBe(false)
  })
  it('rechaza contraseñas de más de 72 bytes UTF-8', () => {
    expect(RegistroSchema.safeParse({ ...valido, password: 'ñ'.repeat(40) }).success).toBe(false)
  })
  it('rechaza correos inválidos y fechas futuras', () => {
    expect(RegistroSchema.safeParse({ ...valido, email: 'no-es-correo' }).success).toBe(false)
    expect(RegistroSchema.safeParse({ ...valido, birthDate: '2999-01-01' }).success).toBe(false)
  })
})
