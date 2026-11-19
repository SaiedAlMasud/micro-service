import { createContext } from 'react'
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  UserResponse,
  UserSummary,
} from '../types/auth'

export interface AuthContextValue {
  accessToken: string | null
  refreshToken: string | null
  user: UserSummary | null
  isAuthenticated: boolean
  isBootstrapping: boolean
  login: (request: LoginRequest) => Promise<AuthResponse>
  register: (request: RegisterRequest) => Promise<UserResponse>
  logout: () => Promise<void>
  hasRole: (role: 'USER' | 'ADMIN') => boolean
  clearSession: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
