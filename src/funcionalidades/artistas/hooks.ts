import { useQuery } from '@tanstack/react-query'
import { obtenerArtista, obtenerArtistaPorNombre, obtenerArtistasActivos } from './api'

export const useArtistasActivos = () =>
  useQuery({ queryKey: ['artistas', 'activos'], queryFn: obtenerArtistasActivos, staleTime: 300_000 })

export const useArtista = (id: number) =>
  useQuery({
    queryKey: ['artista', id],
    queryFn: () => obtenerArtista(id),
    enabled: Number.isInteger(id),
    staleTime: 300_000,
  })

export const useArtistaPorNombre = (stageName: string | undefined) =>
  useQuery({
    queryKey: ['artista', 'nombre', stageName],
    queryFn: () => obtenerArtistaPorNombre(stageName ?? ''),
    enabled: Boolean(stageName),
    staleTime: 300_000,
  })
