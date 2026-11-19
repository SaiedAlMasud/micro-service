import axios from 'axios'
import { env } from '../env'
import { toApiError } from '../error'
import { clearAuthSession, getAccessToken, refreshAccessToken } from '../../features/auth/storage/authStorage'

function createApiClient(baseURL: string) {
  const client = axios.create({
    baseURL,
    timeout: 10_000,
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
  })

  client.interceptors.request.use((config) => {
    const accessToken = getAccessToken()
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`
    }
    return config
  })

  client.interceptors.response.use(
    (response) => response,
    async (error: unknown) => {
      if (!axios.isAxiosError(error) || error.response?.status !== 401) {
        return Promise.reject(toApiError(error))
      }

      const config = error.config as (typeof error.config & { _authRetry?: boolean })
      const isAuthRequest = config?.url?.startsWith('/api/v1/auth/')
        || config?.url?.includes('/api/v1/auth/')

      if (!config || isAuthRequest || config._authRetry) {
        return Promise.reject(toApiError(error))
      }

      config._authRetry = true
      try {
        await refreshAccessToken()
        config.headers = config.headers ?? {}
        config.headers.Authorization = `Bearer ${getAccessToken()}`
        return client(config)
      }
      catch {
        clearAuthSession()
        if (window.location.pathname !== '/login') {
          window.location.assign('/login')
        }
        return Promise.reject(toApiError(error))
      }
    },
  )

  return client
}

export const userApi = createApiClient(env.userApiUrl)
export const productApi = createApiClient(env.productApiUrl)
export const cartApi = createApiClient(env.cartApiUrl)
