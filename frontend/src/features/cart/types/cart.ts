export interface CartItemResponse {
  id: string
  productId: string
  productName: string
  unitPrice: number
  quantity: number
  subtotal: number
  addedAt: string
  priceChanged: boolean | null
  currentPrice: number | null
  available: boolean | null
}

export interface CartResponse {
  id: string | null
  userId: string
  status: 'ACTIVE' | 'CHECKED_OUT'
  items: CartItemResponse[]
  total: number
  createdAt: string | null
  updatedAt: string | null
}
