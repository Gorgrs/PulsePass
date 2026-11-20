import { z } from 'zod'

export const EventCategorySchema = z.enum(['MUSIC', 'SPORTS', 'TECHNOLOGY', 'EDUCATION', 'CULTURE', 'ENTERTAINMENT'])
export const EventStatusSchema = z.enum(['DRAFT', 'PUBLISHED', 'SOLD_OUT', 'CANCELLED', 'FINISHED'])
export type EventCategory = z.infer<typeof EventCategorySchema>
export type EventStatus = z.infer<typeof EventStatusSchema>

export const ArtistaSchema = z.looseObject({
  id: z.number(),
  stageName: z.string(),
  active: z.boolean().optional(),
})
export type Artista = z.infer<typeof ArtistaSchema>

export const EventoResumenSchema = z.looseObject({
  eventCode: z.string(),
  name: z.string(),
  category: EventCategorySchema,
  eventDate: z.string(),
  status: EventStatusSchema,
  venueName: z.string().nullish(),
  venueCity: z.string().nullish(),
  city: z.string().nullish(),
})
export type EventoResumen = z.infer<typeof EventoResumenSchema>

export const EventoSchema = EventoResumenSchema.extend({
  description: z.string().nullish(),
  minimumAge: z.number().nullish(),
  venueCode: z.string().nullish(),
  artists: z.array(ArtistaSchema).nullish(),
})
export type Evento = z.infer<typeof EventoSchema>

export const CrearEventoSchema = z.object({
  eventCode: z.string().trim().min(1, 'El código es obligatorio'),
  name: z.string().trim().min(1, 'El nombre es obligatorio'),
  description: z.string().max(500, 'Máximo 500 caracteres'),
  category: EventCategorySchema,
  eventDate: z.string().min(1, 'La fecha es obligatoria'),
  minimumAge: z
    .number({ error: 'Ingresa un número' })
    .int('Debe ser un entero')
    .min(0, 'No puede ser negativa'),
  venueCode: z.string().min(1, 'Elige una sede'),
})
export type CrearEventoForm = z.infer<typeof CrearEventoSchema>

export const CATEGORIAS: Record<EventCategory, { etiqueta: string; panel: string }> = {
  MUSIC: { etiqueta: 'Música', panel: 'bg-indigo-100 text-indigo-700 dark:bg-indigo-950 dark:text-indigo-300' },
  SPORTS: { etiqueta: 'Deportes', panel: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300' },
  TECHNOLOGY: { etiqueta: 'Tecnología', panel: 'bg-sky-100 text-sky-700 dark:bg-sky-950 dark:text-sky-300' },
  EDUCATION: { etiqueta: 'Educación', panel: 'bg-amber-100 text-amber-700 dark:bg-amber-950 dark:text-amber-300' },
  CULTURE: { etiqueta: 'Cultura', panel: 'bg-rose-100 text-rose-700 dark:bg-rose-950 dark:text-rose-300' },
  ENTERTAINMENT: { etiqueta: 'Entretenimiento', panel: 'bg-fuchsia-100 text-fuchsia-700 dark:bg-fuchsia-950 dark:text-fuchsia-300' },
}

export const ESTADOS_EVENTO: Record<EventStatus, { etiqueta: string; clases: string }> = {
  DRAFT: { etiqueta: 'Borrador', clases: 'bg-slate-200 text-slate-700' },
  PUBLISHED: { etiqueta: 'Publicado', clases: 'bg-emerald-100 text-emerald-800' },
  SOLD_OUT: { etiqueta: 'Agotado', clases: 'bg-amber-100 text-amber-800' },
  CANCELLED: { etiqueta: 'Cancelado', clases: 'bg-red-100 text-red-800' },
  FINISHED: { etiqueta: 'Finalizado', clases: 'bg-slate-300 text-slate-800' },
}
