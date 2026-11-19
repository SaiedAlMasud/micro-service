import axios from 'axios'
import { env } from '../../../lib/env'
import type { AuthResponse } from '../types/auth'
import type { UserSummary } from '../types/auth'

const REFRESH_TOKEN_KEY = 'north-and-field.refreshToken'
const listeners = new Set<(session: AuthSession) => void>()

let accessToken: string | null = null
let authenticatedUser: UserSummary | null = null
let refreshPromise: Promise<AuthResponse> | null = null

export interface AuthSession {
  accessToken: string | null
  refreshToken: string | null
  user: UserSummary | null
}

function readRefreshToken() {
  return window.sessionStorage.getItem(REFRESH_TOKEN_KEY)
}

function notify() {
  const session = getAuthSession()
  listeners.forEach((listener) => listener(session))
}

export function getAuthSession(): AuthSession {
  return {
    accessToken,
    refreshToken: readRefreshToken(),
    user: authenticatedUser,
  }
}

export function getAccessToken() {
  return accessToken
}

export function setAuthSession(response: AuthResponse) {
  accessToken = response.accessToken
  authenticatedUser = response.user
  window.sessionStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken)
  notify()
}

export function clearAuthSession() {
  accessToken = null
  authenticatedUser = null
  window.sessionStorage.removeItem(REFRESH_TOKEN_KEY)
  refreshPromise = null
  notify()
}

export function subscribeToAuth(listener: (session: AuthSession) => void) {
  listeners.add(listener)
  listener(getAuthSession())
  return () => {
    listeners.delete(listener)
  }
}

export function refreshAccessToken(): Promise<AuthResponse> {
  if (refreshPromise) {
    return refreshPromise
  }

  const refreshToken = readRefreshToken()
  if (!refreshToken) {
    return Promise.reject(new Error('No refresh token available'))
  }

  refreshPromise = axios.post<AuthResponse>(
    `${env.userApiUrl}/api/v1/auth/refresh`,
    { refreshToken },
    { headers: { 'Content-Type': 'application/json' } },
  ).then((response) => {
    setAuthSession(response.data)
    return response.data
  }).finally(() => {
    refreshPromise = null
  })

  return refreshPromise
}
