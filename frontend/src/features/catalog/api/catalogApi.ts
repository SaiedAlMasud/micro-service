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
  return response.data
}

export async function fetchProducts(params: ProductListParams) {
  const response = await productApi.get<ProductPage>('/api/v1/products', { params })
  return response.data
}

export async function fetchProduct(id: string) {
  const response = await productApi.get<ProductResponse>(`/api/v1/products/${id}`)
  return response.data
}
