import { createContext } from 'react'
import type { CartResponse } from '../types/cart'

export interface CartContextValue {
  cart: CartResponse | null
  refreshCart: () => Promise<CartResponse | null>
  quantityForProduct: (productId: string) => number
  clearLocalCart: () => void
  cartRevision: number
}

export const CartContext = createContext<CartContextValue | null>(null)
