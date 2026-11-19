import type { PropsWithChildren } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../features/auth/hooks/useAuth'

export function ProtectedRoute({ children }: PropsWithChildren) {
  const { isAuthenticated, isBootstrapping } = useAuth()
  const location = useLocation()

  if (isBootstrapping) {
    return null
  }
  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return children
}

export function RoleRoute({
  children,
  role,
}: PropsWithChildren<{ role: 'USER' | 'ADMIN' }>) {
  const { isAuthenticated, hasRole, isBootstrapping } = useAuth()
  const location = useLocation()

  if (isBootstrapping) {
    return null
  }
  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  if (!hasRole(role)) {
    return <Navigate to="/catalog" replace />
  }
  return children
}
