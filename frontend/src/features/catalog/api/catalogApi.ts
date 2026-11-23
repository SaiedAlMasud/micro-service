import { productApi } from '../../../lib/api/clients'
import type { CategoryResponse, ProductPage, ProductResponse } from '../types/product'

export interface ProductListParams {
  categoryId?: string
  q?: string
  page: number
  size: number
}

export async function fetchCategories() {
  const response = await productApi.get<CategoryResponse[]>('/api/v1/categories')
  if (!Array.isArray(response.data)) {
    throw new Error('Unexpected category response from the server')
  }
  return response.data
}

export async function fetchProducts(params: ProductListParams) {
  const response = await productApi.get<ProductPage>('/api/v1/products', { params })
  const data = response.data as Partial<ProductPage>
  if (!data || !Array.isArray(data.content)) {
    throw new Error('Unexpected product response from the server')
  }
  return {
    content: data.content,
    totalElements: typeof data.totalElements === 'number' ? data.totalElements : data.content.length,
    totalPages: typeof data.totalPages === 'number' ? data.totalPages : 1,
    number: typeof data.number === 'number' ? data.number : params.page,
    size: typeof data.size === 'number' ? data.size : params.size,
  }
}

export async function fetchProduct(id: string) {
  const response = await productApi.get<ProductResponse>(`/api/v1/products/${id}`)
  const data = response.data as Partial<ProductResponse>
  if (!data || typeof data.id !== 'string') {
    throw new Error('Unexpected product response from the server')
  }
  return data as ProductResponse
}
