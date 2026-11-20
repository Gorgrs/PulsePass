import { Link, Outlet } from 'react-router'
import { AvisosProvider } from '../componentes/Avisos'
import { RUTAS } from '../lib/rutas'
import Navbar from './Navbar'

export default function Layout() {
  return (
    <AvisosProvider>
      <a
        href="#contenido"
        className="sr-only focus:not-sr-only focus:absolute focus:left-2 focus:top-2 focus:z-50 focus:rounded focus:bg-white focus:px-3 focus:py-2"
      >
        Saltar al contenido
      </a>
      <Navbar />
      <main id="contenido" className="mx-auto min-h-[70vh] max-w-6xl px-4 py-8">
        <Outlet />
      </main>
      <footer className="border-t border-slate-200 py-6 text-sm text-slate-500 dark:border-slate-800">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-2 px-4">
          <p>© 2026 PulsePass · Caso de estudio académico</p>
          <p className="flex gap-4">
            <Link to={RUTAS.sedes} className="hover:underline">Sedes</Link>
            <Link to={RUTAS.artistas} className="hover:underline">Artistas</Link>
          </p>
        </div>
      </footer>
    </AvisosProvider>
  )
}
