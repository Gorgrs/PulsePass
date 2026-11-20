import type { FieldPath, FieldValues, UseFormSetError } from 'react-hook-form'
import { ApiError } from './http'

// Asigna details del 400 a los campos homónimos; devuelve el mensaje general si no hay campo
export function aplicarErroresServidor<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
  campos: readonly string[],
): string | null {
  if (!(error instanceof ApiError)) return 'Ocurrió un error inesperado. Inténtalo más tarde.'
  const claves = Object.keys(error.details)
  let huboCampo = false
  for (const clave of claves) {
    if (campos.includes(clave)) {
      setError(clave as FieldPath<T>, { message: error.details[clave] })
      huboCampo = true
    }
  }
  return huboCampo && error.status === 400 ? null : error.message
}
