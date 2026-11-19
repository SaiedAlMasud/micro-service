import { useEffect, useState, type PropsWithChildren } from 'react'
import { loginUser, logoutUser, registerUser } from '../api/authApi'
import {
  clearAuthSession,
  getAuthSession,
  refreshAccessToken,
  setAuthSession,
  subscribeToAuth,
  type AuthSession,
} from '../storage/authStorage'
import type { LoginRequest, RegisterRequest } from '../types/auth'
import { AuthContext } from './authContext'

export function AuthProvider({ children }: PropsWithChildren) {
  const [session, setSession] = useState<AuthSession>(getAuthSession)
  const initialSession = getAuthSession()
  const [isBootstrapping, setIsBootstrapping] = useState(
    Boolean(initialSession.refreshToken && !initialSession.accessToken),
  )

  useEffect(() => {
    return subscribeToAuth(setSession)
  }, [])

  useEffect(() => {
    let active = true
    if (!getAuthSession().accessToken && getAuthSession().refreshToken) {
      refreshAccessToken()
        .catch(() => clearAuthSession())
        .finally(() => {
          if (active) setIsBootstrapping(false)
        })
    }
    return () => {
      active = false
    }
  }, [])

  const login = async (request: LoginRequest) => {
    const response = await loginUser(request)
    setAuthSession(response)
    return response
  }

  const register = async (request: RegisterRequest) => registerUser(request)

  const logout = async () => {
    try {
      await logoutUser()
    }
    finally {
      clearAuthSession()
    }
  }

  const hasRole = (role: 'USER' | 'ADMIN') => session.user?.role === role

  return (
    <AuthContext.Provider value={{
      ...session,
      isAuthenticated: Boolean(session.accessToken && session.user),
      isBootstrapping,
      login,
      register,
      logout,
      hasRole,
      clearSession: clearAuthSession,
    }}>
      {children}
    </AuthContext.Provider>
  )
}
