import { useEffect, useState } from 'react'
import { useAuth } from '../../auth/hooks/useAuth'
import { clearCart, fetchCart, removeCartItem, updateCartItem } from '../api/cartApi'
import type { CartResponse } from '../types/cart'
import { useCart } from '../hooks/useCart'

interface CartDrawerProps {
  open: boolean
  onClose: () => void
  refreshKey: number
}

export function CartDrawer({ open, onClose, refreshKey }: CartDrawerProps) {
  const { isAuthenticated } = useAuth()
  const { refreshCart } = useCart()
  const [cart, setCart] = useState<CartResponse | null>(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!open || !isAuthenticated) return
    let active = true
    fetchCart().then((value) => {
      if (active) setCart(value)
    }).catch((reason: { message?: string }) => {
      if (active) setError(reason.message ?? 'Unable to load the cart')
    })
    return () => { active = false }
  }, [open, isAuthenticated, refreshKey])

  const update = async (itemId: string, quantity: number) => {
    setBusy(true)
    setError('')
    try {
      setCart(await updateCartItem(itemId, quantity))
      await refreshCart()
    }
    catch (reason) {
      setError((reason as { message?: string }).message ?? 'Unable to update quantity')
    }
    finally {
      setBusy(false)
    }
  }

  const remove = async (itemId: string) => {
    setBusy(true)
    try {
      await removeCartItem(itemId)
      const updated = await fetchCart()
      setCart(updated)
      await refreshCart()
    }
    catch (reason) {
      setError((reason as { message?: string }).message ?? 'Unable to remove item')
    }
    finally {
      setBusy(false)
    }
  }

  if (!open) return null

  return (
    <aside className="cart-drawer" aria-label="Shopping bag">
      <div className="cart-drawer-header">
        <h2>Your bag</h2>
        <button type="button" className="button button-ghost button-small" onClick={onClose}>Close</button>
      </div>
      {!isAuthenticated ? (
        <p className="cart-message">Sign in to use your bag.</p>
      ) : error ? (
        <p className="cart-message" role="alert">{error}</p>
      ) : !cart || cart.items.length === 0 ? (
        <p className="cart-message">Your bag is empty.</p>
      ) : (
        <>
          <div className="cart-items">
            {cart.items.map((item) => (
              <article className="cart-item" key={item.id}>
                <div>
                  <p className="product-category">Item</p>
                  <h3>{item.productName}</h3>
                  <p className="cart-item-price">{item.unitPrice.toLocaleString()} BDT</p>
                  {item.priceChanged && <p className="stock-note">Price changed</p>}
                  {item.available === false && <p className="stock-note">Currently unavailable</p>}
                </div>
                <div className="quantity-control">
                  <button type="button" className="icon-button" disabled={busy || item.quantity <= 1}
                    onClick={() => update(item.id, item.quantity - 1)} aria-label="Decrease quantity">−</button>
                  <span>{item.quantity}</span>
                  <button type="button" className="icon-button" disabled={busy}
                    onClick={() => update(item.id, item.quantity + 1)} aria-label="Increase quantity">+</button>
                </div>
                <button type="button" className="button button-danger button-small cart-remove" disabled={busy}
                  onClick={() => remove(item.id)}>Remove</button>
              </article>
            ))}
          </div>
          <div className="cart-summary">
            <div><span>Total</span><strong>{cart.total.toLocaleString()} BDT</strong></div>
            <button type="button" className="button button-outline button-small" disabled={busy}
              onClick={async () => {
                setBusy(true)
                try {
                  await clearCart()
                  const updated = await fetchCart()
                  setCart(updated)
                  await refreshCart()
                }
                finally {
                  setBusy(false)
                }
              }}>Clear bag</button>
          </div>
        </>
      )}
    </aside>
  )
}
