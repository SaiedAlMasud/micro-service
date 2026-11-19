import axios from 'axios'

export interface ApiErrorResponse {
  timestamp: string
  status: number
  code: string
  message: string
  path: string
}

export interface ApiError {
  status: number
  code: string
  message: string
  path?: string
}

export function toApiError(error: unknown): ApiError {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    const response = error.response?.data
    return {
      status: error.response?.status ?? 0,
      code: response?.code ?? 'NETWORK_ERROR',
      message: response?.message ?? error.message,
      path: response?.path,
    }
  }

  return {
    status: 0,
    code: 'UNKNOWN_ERROR',
    message: error instanceof Error ? error.message : 'Unexpected error',
  }
}
