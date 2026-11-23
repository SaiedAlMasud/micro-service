import { productApi } from '../../../lib/api/clients'
import type { CategoryResponse, ProductPage, ProductResponse } from '../../catalog/types/product'
import type { ProductRequest } from '../types/productAdmin'

export async function fetchAdminProducts(page = 0, size = 20) {
  const response = await productApi.get<ProductPage>('/api/v1/products', {
    params: { page, size },
  })
  const data = response.data as Partial<ProductPage>
  if (!data || !Array.isArray(data.content)) {
    throw new Error('Unexpected product response from the server')
  }
  return {
    content: data.content,
    totalElements: typeof data.totalElements === 'number' ? data.totalElements : data.content.length,
    totalPages: typeof data.totalPages === 'number' ? data.totalPages : 1,
    number: typeof data.number === 'number' ? data.number : page,
    size: typeof data.size === 'number' ? data.size : size,
  }
}

export async function fetchAdminCategories() {
  const response = await productApi.get<CategoryResponse[]>('/api/v1/categories')
  if (!Array.isArray(response.data)) {
    throw new Error('Unexpected category response from the server')
  }
  return response.data
}

export async function fetchAdminProduct(id: string) {
  const response = await productApi.get<ProductResponse>(`/api/v1/products/${id}`)
  return response.data
}

export async function createAdminProduct(request: ProductRequest) {
  const response = await productApi.post<ProductResponse>('/api/v1/products', request)
  return response.data
}

export async function updateAdminProduct(id: string, request: ProductRequest) {
  const response = await productApi.put<ProductResponse>(`/api/v1/products/${id}`, request)
  return response.data
}

export async function deleteAdminProduct(id: string) {
  await productApi.delete(`/api/v1/products/${id}`)
}
