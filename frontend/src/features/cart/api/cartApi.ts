import { cartApi } from '../../../lib/api/clients'
import type { CartResponse } from '../types/cart'

export async function fetchCart() {
  const response = await cartApi.get<CartResponse>('/api/v1/cart')
  return response.data
}

export async function addCartItem(productId: string, quantity: number) {
  const response = await cartApi.post<CartResponse>('/api/v1/cart/items', { productId, quantity })
  return response.data
}

export async function updateCartItem(itemId: string, quantity: number) {
  const response = await cartApi.put<CartResponse>(`/api/v1/cart/items/${itemId}`, { quantity })
  return response.data
}

export async function removeCartItem(itemId: string) {
  await cartApi.delete(`/api/v1/cart/items/${itemId}`)
}

export async function clearCart() {
  await cartApi.delete('/api/v1/cart')
}
