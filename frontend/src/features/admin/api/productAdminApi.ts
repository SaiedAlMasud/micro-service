import { productApi } from '../../../lib/api/clients'
import type { CategoryResponse, ProductPage, ProductResponse } from '../../catalog/types/product'
import type { ProductRequest } from '../types/productAdmin'

export async function fetchAdminProducts(page = 0, size = 20) {
  const response = await productApi.get<ProductPage>('/api/v1/products', {
    params: { page, size },
  })
  return response.data
}

export async function fetchAdminCategories() {
  const response = await productApi.get<CategoryResponse[]>('/api/v1/categories')
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
