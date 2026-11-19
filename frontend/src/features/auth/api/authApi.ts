import { userApi } from '../../../lib/api/clients'
import type {
  AuthResponse,
  LoginRequest,
  RefreshTokenRequest,
  RegisterRequest,
  UserResponse,
} from '../types/auth'

export async function registerUser(request: RegisterRequest) {
  const response = await userApi.post<UserResponse>('/api/v1/auth/register', request)
  return response.data
}

export async function loginUser(request: LoginRequest) {
  const response = await userApi.post<AuthResponse>('/api/v1/auth/login', request)
  return response.data
}

export async function refreshUser(request: RefreshTokenRequest) {
  const response = await userApi.post<AuthResponse>('/api/v1/auth/refresh', request)
  return response.data
}

export async function logoutUser() {
  await userApi.post('/api/v1/auth/logout')
}
