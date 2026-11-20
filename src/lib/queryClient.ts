import { QueryClient } from '@tanstack/react-query'
import { ApiError } from './http'

// 4xx no se reintenta; red y 5xx, una sola vez. Las mutaciones nunca se reintentan.
export function crearQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        refetchOnWindowFocus: false,
        retry: (intentos, error) =>
          error instanceof ApiError && error.status >= 400 && error.status < 500 ? false : intentos < 1,
      },
      mutations: { retry: false },
    },
  })
}
