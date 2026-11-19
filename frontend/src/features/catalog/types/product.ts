export type ProductStatus = 'ACTIVE' | 'INACTIVE'

export interface ProductResponse {
  id: string
  name: string
  description: string | null
  categoryId: string
  brand: string | null
  price: number
  currency: string
  stockQuantity: number
  imageUrl: string | null
  status: ProductStatus
}

export interface CategoryResponse {
  id: string
  name: string
  description: string | null
}

export interface ProductPage {
  content: ProductResponse[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
