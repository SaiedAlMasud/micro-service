import type { ProductResponse } from '../../catalog/types/product'

export interface ProductRequest {
  name: string
  description?: string
  categoryId: string
  brand?: string
  price: number
  currency: string
  stockQuantity: number
  imageUrl?: string
}

export interface ProductAdminForm {
  name: string
  description: string
  categoryId: string
  brand: string
  price: string
  currency: string
  stockQuantity: string
  imageUrl: string
}

export type EditableProduct = ProductResponse
