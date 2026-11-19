import { useState, type CSSProperties } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../auth/hooks/useAuth'
import { Link as RouterLink } from 'react-router-dom'
import { addCartItem } from '../../cart/api/cartApi'
import { useCart } from '../../cart/hooks/useCart'
import type { ProductResponse } from '../types/product'

interface ProductCardProps {
  product: ProductResponse
  categoryName?: string
  onCartChanged: () => void
}

export function ProductCard({ product, categoryName, onCartChanged }: ProductCardProps) {
  const { isAuthenticated } = useAuth()
  const { quantityForProduct, refreshCart } = useCart()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const tones = ['#b7b09c', '#d4d0c5', '#8d8f83', '#bbb4a6', '#c9c1ae', '#9fa18f']
  const tone = tones[product.name.length % tones.length]
  const visualStyle = { '--product-tone': tone } as CSSProperties
  const quantityInCart = quantityForProduct(product.id)

  const addToBag = async () => {
    if (!isAuthenticated) return
    setBusy(true)
    setError('')
    try {
      await addCartItem(product.id, 1)
      await refreshCart()
      onCartChanged()
    }
    catch (reason) {
      setError((reason as { message?: string }).message ?? 'Unable to add item')
    }
    finally {
      setBusy(false)
    }
  }

  return (
    <article className="product-card">
      <div className="product-visual" style={visualStyle} aria-label={`${product.name} preview`}>
        <span className="product-index">NF—{product.id.slice(0, 8)}</span>
        {product.imageUrl ? (
          <img src={product.imageUrl} alt={product.name} />
        ) : (
          <div className="product-silhouette" aria-hidden="true" />
        )}
      </div>
      <div className="product-meta">
        <div>
          <p className="product-category">{categoryName ?? 'Product'}</p>
          <h3><Link to={`/products/${product.id}`}>{product.name}</Link></h3>
          {product.brand && <p className="product-brand">{product.brand}</p>}
        </div>
        <p className="product-price">
          {product.price.toLocaleString()} <span>{product.currency}</span>
        </p>
      </div>
      {product.stockQuantity === 0 && (
        <p className="stock-note">Currently unavailable</p>
      )}
      {error && <p className="stock-note" role="alert">{error}</p>}
      {isAuthenticated && product.status === 'ACTIVE' && product.stockQuantity > 0 && (
        <button
          className={quantityInCart > 0 ? 'button button-success card-action is-added' : 'button button-primary card-action'}
          type="button"
          onClick={addToBag}
          disabled={busy}>
          {busy ? 'Adding...' : quantityInCart > 0 ? `Added (${quantityInCart})` : 'Add to bag'}
        </button>
      )}
      {!isAuthenticated && product.status === 'ACTIVE' && product.stockQuantity > 0 && (
        <RouterLink className="button button-secondary card-action card-action-link" to="/login">Sign in to add</RouterLink>
      )}
    </article>
  )
}
