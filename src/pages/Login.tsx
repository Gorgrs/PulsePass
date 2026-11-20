import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useLocation, useNavigate } from 'react-router'
import Aviso from '../componentes/Aviso'
import Boton from '../componentes/Boton'
import Campo from '../componentes/Campo'
import Tarjeta from '../componentes/Tarjeta'
import { useAuth } from '../funcionalidades/auth/AuthContext'
import { LoginSchema, type LoginForm } from '../funcionalidades/auth/tipos'
import { ApiError } from '../lib/http'
import { leerEstado } from '../lib/estadoNavegacion'
import { RUTAS } from '../lib/rutas'

export default function Login() {
  const { iniciarSesion } = useAuth()
  const navigate = useNavigate()
  const estado = leerEstado(useLocation().state)
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const { register, handleSubmit, formState } = useForm<LoginForm>({ resolver: zodResolver(LoginSchema) })

  const enviar = async (datos: LoginForm) => {
    setErrorGeneral(null)
    try {
      await iniciarSesion(datos)
      void navigate(estado.desde ?? RUTAS.inicio, { replace: true })
    } catch (error) {
      if (error instanceof ApiError && (error.status === 401 || error.status === 400)) setErrorGeneral('Credenciales inválidas')
      else if (error instanceof ApiError && error.status === 403) setErrorGeneral('Credenciales inválidas')
      else setErrorGeneral('No pudimos iniciar sesión. Inténtalo más tarde.')
    }
  }

  return (
    <Tarjeta className="mx-auto max-w-md space-y-4">
      <h1 className="text-2xl font-bold">Iniciar sesión</h1>
      {estado.registrado && <Aviso tono="exito">Cuenta creada, inicia sesión.</Aviso>}
      {estado.motivo === 'expirada' && <Aviso tono="advertencia">Tu sesión expiró. Vuelve a ingresar.</Aviso>}
      <form onSubmit={handleSubmit(enviar)} className="space-y-4" noValidate>
        <Campo etiqueta="Usuario o correo" autoComplete="username" error={formState.errors.identifier?.message} {...register('identifier')} />
        <Campo etiqueta="Contraseña" type="password" autoComplete="current-password" error={formState.errors.password?.message} {...register('password')} />
        {errorGeneral && <Aviso tono="error">{errorGeneral}</Aviso>}
        <Boton type="submit" cargando={formState.isSubmitting} className="w-full">Ingresar</Boton>
      </form>
      <p className="text-sm">
        ¿No tienes cuenta? <Link to={RUTAS.registro} className="font-semibold text-marca-700 hover:underline dark:text-marca-500">Regístrate</Link>
      </p>
    </Tarjeta>
  )
}
