import { z } from 'zod'

export const SedeSchema = z.looseObject({
  code: z.string(),
  name: z.string(),
  city: z.string(),
  capacity: z.number(),
  active: z.boolean().optional(),
})
export type Sede = z.infer<typeof SedeSchema>
