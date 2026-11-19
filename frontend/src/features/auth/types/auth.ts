export type UserRole = 'USER' | 'ADMIN'

export interface UserSummary {
  id: string
  fullName: string
  role: UserRole
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: 'Bearer'
  accessTokenExpiresIn: number
  refreshTokenExpiresIn: number
  user: UserSummary
}

export interface RegisterRequest {
  fullName: string
  email: string
  password: string
  phone?: string
}

export interface UserResponse {
  id: string
  fullName: string
  email: string
  phone: string | null
  role: UserRole
  status: 'ACTIVE' | 'BLOCKED'
  createdAt: string
  updatedAt: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RefreshTokenRequest {
  refreshToken: string
}
