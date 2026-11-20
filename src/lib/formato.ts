const fecha = new Intl.DateTimeFormat('es-CO', { dateStyle: 'long' })
const fechaHora = new Intl.DateTimeFormat('es-CO', { dateStyle: 'long', timeStyle: 'short' })

export function formatoFecha(valor: string): string {
  const d = new Date(valor)
  return Number.isNaN(d.getTime()) ? valor : fecha.format(d)
}

export function formatoFechaHora(valor: string): string {
  const d = new Date(valor)
  return Number.isNaN(d.getTime()) ? valor : fechaHora.format(d)
}

export const moneda = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  maximumFractionDigits: 0,
})
