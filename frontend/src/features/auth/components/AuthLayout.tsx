import type { PropsWithChildren, ReactNode } from 'react'

interface AuthLayoutProps extends PropsWithChildren {
  eyebrow: string
  title: string
  description: string
  aside: ReactNode
}

export function AuthLayout({
  eyebrow,
  title,
  description,
  aside,
  children,
}: AuthLayoutProps) {
  return (
    <main className="auth-layout">
      <section className="auth-panel" aria-labelledby="auth-title">
        <p className="eyebrow">{eyebrow}</p>
        <h1 id="auth-title">{title}</h1>
        <p className="auth-description">{description}</p>
        {children}
      </section>
      <aside className="auth-aside" aria-label="Store information">
        {aside}
      </aside>
    </main>
  )
}
