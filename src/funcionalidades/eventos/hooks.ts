import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  asociarArtista,
  crearEvento,
  obtenerEvento,
  obtenerPorArtista,
  obtenerPublicados,
  publicarEvento,
} from './api'

export const useEventosPublicados = () =>
  useQuery({ queryKey: ['eventos', 'publicados'], queryFn: obtenerPublicados, staleTime: 60_000 })

export const useEvento = (codigo: string | undefined) =>
  useQuery({
    queryKey: ['evento', codigo],
    queryFn: () => obtenerEvento(codigo ?? ''),
    enabled: Boolean(codigo),
    staleTime: 30_000,
  })

export const useEventosPorArtista = (stageName: string | undefined) =>
  useQuery({
    queryKey: ['eventos', 'artista', stageName],
    queryFn: () => obtenerPorArtista(stageName ?? ''),
    enabled: Boolean(stageName),
    staleTime: 60_000,
  })

export function useCrearEvento() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: crearEvento,
    onSuccess: (evento) => qc.setQueryData(['evento', evento.eventCode], evento),
  })
}

export function usePublicarEvento() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: publicarEvento,
    onSuccess: (_e, codigo) => {
      void qc.invalidateQueries({ queryKey: ['evento', codigo] })
      void qc.invalidateQueries({ queryKey: ['eventos', 'publicados'] })
    },
  })
}

export function useAsociarArtista(codigo: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (artistaId: number) => asociarArtista(codigo, artistaId),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['evento', codigo] })
      void qc.invalidateQueries({ queryKey: ['eventos', 'artista'] })
    },
  })
}
