import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router'
import Aviso from '../componentes/Aviso'
import Boton from '../componentes/Boton'
import Campo from '../componentes/Campo'
import Tarjeta from '../componentes/Tarjeta'
import { useRegistrarUsuario } from '../funcionalidades/usuarios/hooks'
import { RegistroSchema, type RegistroForm } from '../funcionalidades/usuarios/tipos'
import { ApiError } from '../lib/http'
import { aplicarErroresServidor } from '../lib/erroresFormulario'
import { RUTAS } from '../lib/rutas'

const CAMPOS = ['username', 'email', 'firstName', 'lastName', 'phone', 'city', 'birthDate', 'password'] as const

export default function Registro() {
  const navigate = useNavigate()
  const registrar = useRegistrarUsuario()
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null)
  const { register, handleSubmit, setError, formState } = useForm<RegistroForm>({
    resolver: zodResolver(RegistroSchema),
    defaultValues: { phone: '', city: '' },
  })
  const e = formState.errors

  const enviar = (datos: RegistroForm) => {
    setErrorGeneral(null)
    registrar.mutate(datos, {
      onSuccess: () => void navigate(RUTAS.login, { state: { registrado: true } }),
      onError: (error) => {
        if (error instanceof ApiError && error.status === 409) {
          setErrorGeneral('El usuario o correo ya está registrado')
          return
        }
        setErrorGeneral(aplicarErroresServidor<RegistroForm>(error, setError, CAMPOS))
      },
    })
  }

  return (
    <Tarjeta className="mx-auto max-w-xl space-y-4">
      <h1 className="text-2xl font-bold">Crear cuenta</h1>
      <form onSubmit={handleSubmit(enviar)} className="grid gap-4 sm:grid-cols-2" noValidate>
        <Campo etiqueta="Usuario" autoComplete="username" error={e.username?.message} {...register('username')} />
        <Campo etiqueta="Correo" type="email" autoComplete="email" error={e.email?.message} {...register('email')} />
        <Campo etiqueta="Nombre" autoComplete="given-name" error={e.firstName?.message} {...register('firstName')} />
        <Campo etiqueta="Apellido" autoComplete="family-name" error={e.lastName?.message} {...register('lastName')} />
        <Campo etiqueta="Teléfono (opcional)" autoComplete="tel" error={e.phone?.message} {...register('phone')} />
        <Campo etiqueta="Ciudad (opcional)" error={e.city?.message} {...register('city')} />
        <Campo etiqueta="Fecha de nacimiento" type="date" error={e.birthDate?.message} {...register('birthDate')} />
        <Campo etiqueta="Contraseña (mínimo 12 caracteres)" type="password" autoComplete="new-password" error={e.password?.message} {...register('password')} />
        <div className="space-y-4 sm:col-span-2">
          {errorGeneral && <Aviso tono="error">{errorGeneral}</Aviso>}
          <Boton type="submit" cargando={registrar.isPending} className="w-full">Crear cuenta</Boton>
        </div>
      </form>
      <p className="text-sm">
        ¿Ya tienes cuenta? <Link to={RUTAS.login} className="font-semibold text-marca-700 hover:underline dark:text-marca-500">Ingresa</Link>
      </p>
    </Tarjeta>
  )
}
