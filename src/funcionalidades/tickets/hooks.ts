import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  cancelarTicket,
  comprarTicket,
  obtenerTicket,
  obtenerTicketsDeUsuario,
  obtenerTicketsPagados,
  usarTicket,
} from './api'

export const useTicket = (codigo: string | undefined) =>
  useQuery({
    queryKey: ['ticket', codigo],
    queryFn: () => obtenerTicket(codigo ?? ''),
    enabled: Boolean(codigo),
    staleTime: 15_000,
  })

export const useMisTickets = (email: string | undefined) =>
  useQuery({
    queryKey: ['tickets', 'usuario', email],
    queryFn: () => obtenerTicketsDeUsuario(email ?? ''),
    enabled: Boolean(email),
    staleTime: 15_000,
  })

export const useTicketsPagadosPorEvento = (eventCode: string | undefined) =>
  useQuery({
    queryKey: ['tickets', 'pagados', eventCode],
    queryFn: () => obtenerTicketsPagados(eventCode ?? ''),
    enabled: Boolean(eventCode),
    staleTime: 15_000,
  })

export function useComprarTicket() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: comprarTicket,
    onSuccess: (_t, v) => {
      void qc.invalidateQueries({ queryKey: ['tickets', 'usuario', v.userEmail] })
      void qc.invalidateQueries({ queryKey: ['evento', v.eventCode] })
    },
  })
}

export function useCancelarTicket(email: string | undefined) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: cancelarTicket,
    onSuccess: (t, codigo) => {
      void qc.invalidateQueries({ queryKey: ['tickets', 'usuario', email] })
      void qc.invalidateQueries({ queryKey: ['ticket', codigo] })
      if (t.eventCode) void qc.invalidateQueries({ queryKey: ['tickets', 'pagados', t.eventCode] })
    },
  })
}

export function useUsarTicket() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: usarTicket,
    onSuccess: (t, codigo) => {
      void qc.invalidateQueries({ queryKey: ['ticket', codigo] })
      if (t.eventCode) void qc.invalidateQueries({ queryKey: ['tickets', 'pagados', t.eventCode] })
    },
  })
}
