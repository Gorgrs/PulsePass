import clsx from 'clsx'
import { useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router'
import Icono from '../componentes/Iconos'
import { useAuth } from '../funcionalidades/auth/AuthContext'
import { RUTAS } from '../lib/rutas'

interface Enlace {
  a: string
  texto: string
  fin?: boolean
}

function alternarTema(): void {
  const oscuro = document.documentElement.classList.toggle('dark')
  try {
    localStorage.setItem('tema', oscuro ? 'oscuro' : 'claro')
  } catch {
    // sin almacenamiento: el cambio dura solo la sesión de la pestaña
  }
}

const claseEnlace = ({ isActive }: { isActive: boolean }) =>
  clsx(
    'rounded-lg px-3 py-2 text-sm font-medium focus-visible:outline-2 focus-visible:outline-marca-600',
    isActive
      ? 'bg-marca-50 text-marca-700 dark:bg-slate-800 dark:text-marca-500'
      : 'text-slate-700 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800',
  )

export default function Navbar() {
  const { sesion, cerrarSesion, tieneRol } = useAuth()
  const navigate = useNavigate()
  const [menuMovil, setMenuMovil] = useState(false)
  const [menuUsuario, setMenuUsuario] = useState(false)

  const esPersonal = tieneRol('STAFF', 'ADMIN')
  const esSoloUser = tieneRol('USER') && !esPersonal

  const enlaces: Enlace[] = [
    { a: RUTAS.eventos, texto: 'Eventos' },
    { a: RUTAS.artistas, texto: 'Artistas' },
    { a: RUTAS.sedes, texto: 'Sedes' },
  ]
  if (esSoloUser) enlaces.push({ a: RUTAS.misTickets, texto: 'Mis tickets' })
  if (esPersonal) {
    enlaces.push({ a: RUTAS.validacion, texto: 'Validar ingreso' })
    enlaces.push({ a: RUTAS.ticketsPagados(), texto: 'Tickets pagados' })
  }
  if (tieneRol('ADMIN')) enlaces.push({ a: RUTAS.adminEventos, texto: 'Administrar eventos' })

  const salir = () => {
    cerrarSesion()
    setMenuUsuario(false)
    setMenuMovil(false)
    void navigate(RUTAS.inicio)
  }

  const iniciales = sesion.estado === 'autenticado' ? sesion.usuario.username.slice(0, 2).toUpperCase() : ''

  return (
    <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/95 backdrop-blur dark:border-slate-800 dark:bg-slate-900/95">
      <div className="mx-auto flex max-w-6xl items-center gap-4 px-4 py-3">
        <Link to={RUTAS.inicio} className="flex items-center gap-2 font-titulo text-lg font-bold text-marca-700 dark:text-marca-500">
          <Icono nombre="musica" /> PulsePass
        </Link>

        <nav aria-label="Principal" className="hidden flex-1 items-center gap-1 md:flex">
          {enlaces.map((e) => (
            <NavLink key={e.a} to={e.a} end={e.a === RUTAS.inicio} className={claseEnlace}>
              {e.texto}
            </NavLink>
          ))}
        </nav>

        <div className="ml-auto flex items-center gap-2">
          <button
            type="button"
            onClick={alternarTema}
            aria-label="Cambiar tema"
            className="grid size-11 place-items-center rounded-lg text-slate-700 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800"
          >
            <Icono nombre="luna" className="size-5 dark:hidden" />
            <Icono nombre="sol" className="hidden size-5 dark:block" />
          </button>

          {sesion.estado === 'anonimo' && (
            <div className="hidden items-center gap-2 md:flex">
              <Link to={RUTAS.login} className="rounded-lg px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800">
                Ingresar
              </Link>
              <Link to={RUTAS.registro} className="rounded-lg bg-marca-600 px-3 py-2 text-sm font-semibold text-white hover:bg-marca-700">
                Registrarse
              </Link>
            </div>
          )}

          {sesion.estado === 'autenticado' && (
            <div className="relative hidden md:block">
              <button
                type="button"
                aria-haspopup="menu"
                aria-expanded={menuUsuario}
                onClick={() => setMenuUsuario((v) => !v)}
                className="flex min-h-11 items-center gap-2 rounded-lg px-2 hover:bg-slate-100 dark:hover:bg-slate-800"
              >
                <span className="grid size-8 place-items-center rounded-full bg-marca-600 text-xs font-bold text-white">{iniciales}</span>
                <span className="text-sm font-medium">{sesion.usuario.username}</span>
              </button>
              {menuUsuario && (
                <div role="menu" className="absolute right-0 mt-2 w-48 rounded-lg bg-white p-1 shadow-lg dark:bg-slate-800">
                  <Link role="menuitem" to={RUTAS.cuenta} onClick={() => setMenuUsuario(false)} className="block rounded px-3 py-2 text-sm hover:bg-slate-100 dark:hover:bg-slate-700">
                    Mi cuenta
                  </Link>
                  <button role="menuitem" type="button" onClick={salir} className="block w-full rounded px-3 py-2 text-left text-sm hover:bg-slate-100 dark:hover:bg-slate-700">
                    Salir
                  </button>
                </div>
              )}
            </div>
          )}

          <button
            type="button"
            aria-label={menuMovil ? 'Cerrar menú' : 'Abrir menú'}
            aria-expanded={menuMovil}
            onClick={() => setMenuMovil((v) => !v)}
            className="grid size-11 place-items-center rounded-lg md:hidden"
          >
            <Icono nombre={menuMovil ? 'cerrar' : 'menu'} />
          </button>
        </div>
      </div>

      {menuMovil && (
        <nav aria-label="Móvil" className="border-t border-slate-200 px-4 py-3 md:hidden dark:border-slate-800">
          <div className="flex flex-col gap-1">
            {enlaces.map((e) => (
              <NavLink key={e.a} to={e.a} onClick={() => setMenuMovil(false)} className={claseEnlace}>
                {e.texto}
              </NavLink>
            ))}
            <hr className="my-2 border-slate-200 dark:border-slate-700" />
            {sesion.estado === 'anonimo' && (
              <>
                <NavLink to={RUTAS.login} onClick={() => setMenuMovil(false)} className={claseEnlace}>Ingresar</NavLink>
                <NavLink to={RUTAS.registro} onClick={() => setMenuMovil(false)} className={claseEnlace}>Registrarse</NavLink>
              </>
            )}
            {sesion.estado === 'autenticado' && (
              <>
                <NavLink to={RUTAS.cuenta} onClick={() => setMenuMovil(false)} className={claseEnlace}>Mi cuenta</NavLink>
                <button type="button" onClick={salir} className="rounded-lg px-3 py-2 text-left text-sm font-medium hover:bg-slate-100 dark:hover:bg-slate-800">
                  Salir
                </button>
              </>
            )}
          </div>
        </nav>
      )}
    </header>
  )
}
