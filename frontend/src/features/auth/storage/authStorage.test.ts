import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const axiosPost = vi.hoisted(() => vi.fn())

vi.mock('axios', () => ({
  default: {
    post: axiosPost,
  },
}))

import {
  clearAuthSession,
  getAuthSession,
  refreshAccessToken,
  setAuthSession,
} from './authStorage'

describe('authentication session storage', () => {
  beforeEach(() => {
    const values = new Map<string, string>()
    vi.stubGlobal('window', {
      sessionStorage: {
        getItem: (key: string) => values.get(key) ?? null,
        setItem: (key: string, value: string) => values.set(key, value),
        removeItem: (key: string) => values.delete(key),
      },
      location: { pathname: '/catalog' },
    })
    clearAuthSession()
    axiosPost.mockReset()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('keeps the access token in memory and the refresh token in sessionStorage', () => {
    setAuthSession({
      accessToken: 'access-one',
      refreshToken: 'refresh-one',
      tokenType: 'Bearer',
      accessTokenExpiresIn: 3600,
      refreshTokenExpiresIn: 604800,
      user: { id: '00000000-0000-0000-0000-000000000001', fullName: 'Test User', role: 'USER' },
    })

    expect(getAuthSession()).toMatchObject({
      accessToken: 'access-one',
      refreshToken: 'refresh-one',
      user: { role: 'USER' },
    })
  })

  it('rotates refresh and access tokens from a successful refresh', async () => {
    setAuthSession({
      accessToken: 'access-one',
      refreshToken: 'refresh-one',
      tokenType: 'Bearer',
      accessTokenExpiresIn: 3600,
      refreshTokenExpiresIn: 604800,
      user: { id: '00000000-0000-0000-0000-000000000001', fullName: 'Test User', role: 'USER' },
    })

    axiosPost.mockResolvedValue({
      data: {
        accessToken: 'access-two',
        refreshToken: 'refresh-two',
        tokenType: 'Bearer',
        accessTokenExpiresIn: 3600,
        refreshTokenExpiresIn: 604800,
        user: { id: '00000000-0000-0000-0000-000000000001', fullName: 'Test User', role: 'USER' },
      },
    })

    await refreshAccessToken()

    expect(getAuthSession()).toMatchObject({
      accessToken: 'access-two',
      refreshToken: 'refresh-two',
    })
  })

  it('uses one refresh promise for simultaneous refresh callers', async () => {
    setAuthSession({
      accessToken: 'access-one',
      refreshToken: 'refresh-one',
      tokenType: 'Bearer',
      accessTokenExpiresIn: 3600,
      refreshTokenExpiresIn: 604800,
      user: { id: '00000000-0000-0000-0000-000000000001', fullName: 'Test User', role: 'USER' },
    })

    let resolveRefresh: ((value: unknown) => void) | undefined
    axiosPost.mockReturnValue(new Promise((resolve) => {
      resolveRefresh = resolve
    }))

    const first = refreshAccessToken()
    const second = refreshAccessToken()

    expect(first).toBe(second)
    resolveRefresh?.({
      data: {
        accessToken: 'access-two',
        refreshToken: 'refresh-two',
        tokenType: 'Bearer',
        accessTokenExpiresIn: 3600,
        refreshTokenExpiresIn: 604800,
        user: { id: '00000000-0000-0000-0000-000000000001', fullName: 'Test User', role: 'USER' },
      },
    })
    await first
    expect(axiosPost).toHaveBeenCalledTimes(1)
  })

  it('clears access, refresh, and user state on logout or failed refresh', () => {
    setAuthSession({
      accessToken: 'access-one',
      refreshToken: 'refresh-one',
      tokenType: 'Bearer',
      accessTokenExpiresIn: 3600,
      refreshTokenExpiresIn: 604800,
      user: { id: '00000000-0000-0000-0000-000000000001', fullName: 'Test User', role: 'USER' },
    })

    clearAuthSession()

    expect(getAuthSession()).toEqual({
      accessToken: null,
      refreshToken: null,
      user: null,
    })
  })
})
