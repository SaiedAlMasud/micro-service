import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { PageContainer } from '../../components/layout/PageContainer'
import { useAuth } from '../../features/auth/hooks/useAuth'
import { addCartItem } from '../../features/cart/api/cartApi'
import { fetchProduct } from '../../features/catalog/api/catalogApi'
import type { ProductResponse } from '../../features/catalog/types/product'
import { useCart } from '../../features/cart/hooks/useCart'

export function ProductDetailPage() {
	const { id } = useParams()
	const { isAuthenticated } = useAuth()
	const { quantityForProduct, refreshCart } = useCart()
	const [product, setProduct] = useState<ProductResponse | null>(null)
	const [error, setError] = useState('')
	const [busy, setBusy] = useState(false)
	const quantityInCart = product ? quantityForProduct(product.id) : 0

	useEffect(() => {
		if (!id) return
		fetchProduct(id).then(setProduct).catch((reason: { message?: string }) => {
			setError(reason.message ?? 'Unable to load product')
		})
	}, [id])

	const addToBag = async () => {
		if (!product) return
		setBusy(true)
		setError('')
		try {
			await addCartItem(product.id, 1)
			await refreshCart()
		}
		catch (reason) {
			setError((reason as { message?: string }).message ?? 'Unable to add item')
		}
		finally {
			setBusy(false)
		}
	}

	return (
		<PageContainer>
			<section className="product-detail">
				{error && <p className="form-alert" role="alert">{error}</p>}
				{product && (
					<>
						<div className="product-detail-visual">
							{product.imageUrl ? <img src={product.imageUrl} alt={product.name} /> : <div className="product-silhouette" />}
						</div>
						<div className="product-detail-meta">
							<p className="eyebrow">{product.status}</p>
							<h1>{product.name}</h1>
							<p className="product-detail-brand">{product.brand ?? 'Unbranded'}</p>
							<p className="product-detail-description">{product.description ?? 'No additional description.'}</p>
							<div className="product-detail-buy">
								<p>{product.price.toLocaleString()} <span>{product.currency}</span></p>
								{product.status === 'ACTIVE' && product.stockQuantity > 0 && isAuthenticated && (
									<button
										className={quantityInCart > 0 ? 'button button-success is-added' : 'button button-primary'}
										type="button"
										onClick={addToBag}
										disabled={busy}>
										{busy ? 'Adding...' : quantityInCart > 0 ? `Added (${quantityInCart})` : 'Add to bag'}
									</button>
								)}
								{!isAuthenticated && (
									<Link className="button button-secondary auth-button-link" to="/login">Sign in to add</Link>
								)}
								{product.stockQuantity === 0 && <p className="stock-note">Currently unavailable</p>}
							</div>
						</div>
					</>
				)}
			</section>
		</PageContainer>
	)
}
