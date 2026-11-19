import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { AuthLayout } from '../../features/auth/components/AuthLayout'
import { FormField } from '../../features/auth/components/FormField'
import { useAuth } from '../../features/auth/hooks/useAuth'
import type { ApiError } from '../../lib/error'
import type { LoginRequest } from '../../features/auth/types/auth'

interface LoginLocationState {
  from?: string
}

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const state = location.state as LoginLocationState | null
  const [form, setForm] = useState<LoginRequest>({ email: '', password: '' })
  const [fieldErrors, setFieldErrors] = useState<Partial<Record<keyof LoginRequest, string>>>({})
  const [serverError, setServerError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const validate = () => {
    const errors: Partial<Record<keyof LoginRequest, string>> = {}
    if (!form.email.trim()) errors.email = 'Email is required'
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) errors.email = 'Enter a valid email address'
    if (!form.password) errors.password = 'Password is required'
    setFieldErrors(errors)
    return Object.keys(errors).length === 0
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setServerError('')
    if (!validate()) return

    setIsSubmitting(true)
    try {
      await login(form)
      navigate(state?.from || '/catalog', { replace: true })
    }
    catch (error) {
      const apiError = error as ApiError
      setServerError(apiError.message || 'Unable to sign in')
    }
    finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthLayout
      eyebrow="Member access"
      title="Welcome back."
      description="Sign in to continue with your North & Field account."
      aside={
        <>
          <p className="eyebrow">North &amp; Field</p>
          <p className="auth-aside-copy">A considered edit of objects for work and home.</p>
        </>
      }
    >
      <form className="auth-form" onSubmit={handleSubmit} noValidate>
        {serverError && (
          <div className="form-alert" role="alert">{serverError}</div>
        )}
        <FormField
          id="email"
          label="Email"
          type="email"
          autoComplete="email"
          value={form.email}
          onChange={(event) => setForm({ ...form, email: event.target.value })}
          error={fieldErrors.email}
        />
        <FormField
          id="password"
          label="Password"
          type="password"
          autoComplete="current-password"
          value={form.password}
          onChange={(event) => setForm({ ...form, password: event.target.value })}
          error={fieldErrors.password}
        />
        <button className="button button-primary" type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Signing in...' : 'Sign in'}
        </button>
        <p className="auth-switch">
          New to North &amp; Field? <Link to="/register">Create an account</Link>
        </p>
      </form>
    </AuthLayout>
  )
}
