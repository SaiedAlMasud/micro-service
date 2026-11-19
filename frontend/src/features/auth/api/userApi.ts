import { userApi } from '../../../lib/api/clients'
import type { UserResponse } from '../types/auth'

export async function fetchCurrentUser() {
  const response = await userApi.get<UserResponse>('/api/v1/users/me')
  return response.data
}
