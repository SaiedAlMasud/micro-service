import { useEffect, useState, type PropsWithChildren } from 'react'
import { useAuth } from '../../auth/hooks/useAuth'
import { fetchCart } from '../api/cartApi'
import type { CartResponse } from '../types/cart'
import { CartContext } from './CartContext'

export function CartProvider({ children }: PropsWithChildren) {
  const { isAuthenticated } = useAuth()
  const [cart, setCart] = useState<CartResponse | null>(null)
  const [cartRevision, setCartRevision] = useState(0)

  useEffect(() => {
    if (isAuthenticated) {
      fetchCart().then((value) => {
        setCart(value)
        setCartRevision((value) => value + 1)
      }).catch(() => setCart(null))
    }
    else {
      queueMicrotask(() => setCart(null))
    }
  }, [isAuthenticated])

  const refreshCart = async () => {
    if (!isAuthenticated) return null
    const value = await fetchCart()
    setCart(value)
    setCartRevision((revision) => revision + 1)
    return value
  }

  const clearLocalCart = () => {
    setCart(null)
    setCartRevision((revision) => revision + 1)
  }

  const quantityForProduct = (productId: string) =>
    (Array.isArray(cart?.items)
      ? cart.items.find((item) => item.productId === productId)?.quantity
      : 0) ?? 0

  return (
    <CartContext.Provider value={{
      cart,
      refreshCart,
      quantityForProduct,
      clearLocalCart,
      cartRevision,
    }}>
      {children}
    </CartContext.Provider>
  )
}
