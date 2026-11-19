import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { AuthLayout } from '../../features/auth/components/AuthLayout'
import { FormField } from '../../features/auth/components/FormField'
import { useAuth } from '../../features/auth/hooks/useAuth'
import type { ApiError } from '../../lib/error'
import type { RegisterRequest } from '../../features/auth/types/auth'

export function RegisterPage() {
  const { register } = useAuth()
  const [form, setForm] = useState<RegisterRequest>({
    fullName: '',
    email: '',
    password: '',
    phone: '',
  })
  const [fieldErrors, setFieldErrors] = useState<Partial<Record<keyof RegisterRequest, string>>>({})
  const [serverError, setServerError] = useState('')
  const [successName, setSuccessName] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const validate = () => {
    const errors: Partial<Record<keyof RegisterRequest, string>> = {}
    if (!form.fullName.trim()) errors.fullName = 'Full name is required'
    else if (form.fullName.trim().length > 120) errors.fullName = 'Maximum 120 characters'

    if (!form.email.trim()) errors.email = 'Email is required'
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) errors.email = 'Enter a valid email address'
    else if (form.email.length > 160) errors.email = 'Maximum 160 characters'

    if (!form.password) errors.password = 'Password is required'
    else if (form.password.length < 6 || form.password.length > 100) {
      errors.password = 'Password must be 6–100 characters'
    }

    if (form.phone && form.phone.length > 30) errors.phone = 'Maximum 30 characters'
    setFieldErrors(errors)
    return Object.keys(errors).length === 0
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setServerError('')
    setSuccessName('')
    if (!validate()) return

    setIsSubmitting(true)
    try {
      const phone = form.phone?.trim()
      const user = await register({
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        password: form.password,
        phone: phone || undefined,
      })
      setSuccessName(user.fullName)
    }
    catch (error) {
      const apiError = error as ApiError
      setServerError(apiError.message || 'Unable to create your account')
    }
    finally {
      setIsSubmitting(false)
    }
  }

  if (successName) {
    return (
      <main className="auth-layout">
        <section className="auth-panel" aria-labelledby="registration-success">
          <p className="eyebrow">Registration complete</p>
          <h1 id="registration-success">Welcome, {successName}.</h1>
          <p className="auth-description">
            Your account is ready. Sign in to continue.
          </p>
          <Link className="button button-primary auth-button-link" to="/login">Continue to sign in</Link>
        </section>
        <aside className="auth-aside" aria-label="Store information">
          <p className="eyebrow">North &amp; Field</p>
          <p className="auth-aside-copy">A considered edit of objects for work and home.</p>
        </aside>
      </main>
    )
  }

  return (
    <AuthLayout
      eyebrow="New account"
      title="Create an account."
      description="Registration takes a moment. You will sign in separately after your account is created."
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
          id="fullName"
          label="Full name"
          type="text"
          autoComplete="name"
          value={form.fullName}
          onChange={(event) => setForm({ ...form, fullName: event.target.value })}
          error={fieldErrors.fullName}
        />
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
          autoComplete="new-password"
          value={form.password}
          onChange={(event) => setForm({ ...form, password: event.target.value })}
          error={fieldErrors.password}
          hint="Use 6–100 characters."
        />
        <FormField
          id="phone"
          label="Phone (optional)"
          type="tel"
          autoComplete="tel"
          value={form.phone}
          onChange={(event) => setForm({ ...form, phone: event.target.value })}
          error={fieldErrors.phone}
        />
        <button className="button button-primary" type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Creating account...' : 'Create account'}
        </button>
        <p className="auth-switch">
          Already registered? <Link to="/login">Sign in</Link>
        </p>
      </form>
    </AuthLayout>
  )
}
