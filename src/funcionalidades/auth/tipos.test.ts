import { describe, expect, it } from 'vitest'
import { normalizarRoles } from './tipos'

describe('normalizarRoles', () => {
  it('quita el prefijo ROLE_ y descarta roles desconocidos', () => {
    expect(normalizarRoles(['ROLE_USER', 'STAFF', 'OTRO', 'USER'])).toEqual(['USER', 'STAFF'])
  })
})
