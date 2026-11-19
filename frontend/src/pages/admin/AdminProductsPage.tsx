import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { PageContainer } from '../../components/layout/PageContainer'
import { deleteAdminProduct, fetchAdminCategories, fetchAdminProducts } from '../../features/admin/api/productAdminApi'
import type { CategoryResponse, ProductPage } from '../../features/catalog/types/product'

export function AdminProductsPage() {
  const [page, setPage] = useState(0)
  const [products, setProducts] = useState<ProductPage | null>(null)
  const [categories, setCategories] = useState<CategoryResponse[]>([])
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState('')

  useEffect(() => {
    fetchAdminCategories().then(setCategories).catch((reason: { message?: string }) => {
      setError(reason.message ?? 'Unable to load categories')
    })
  }, [])

  useEffect(() => {
    fetchAdminProducts(page, 20).then(setProducts).catch((reason: { message?: string }) => {
      setError(reason.message ?? 'Unable to load products')
    })
  }, [page])

  const categoryName = (id: string) => categories.find((category) => category.id === id)?.name ?? 'Unknown'

  const handleDelete = async (id: string, name: string) => {
    if (!window.confirm(`Deactivate "${name}"? It will remain visible in direct product details.`)) return
    setBusyId(id)
    setError('')
    try {
      await deleteAdminProduct(id)
      setProducts(await fetchAdminProducts(page, 20))
    }
    catch (reason) {
      setError((reason as { message?: string }).message ?? 'Unable to deactivate product')
    }
    finally {
      setBusyId('')
    }
  }

  return (
    <PageContainer>
      <section className="admin-page">
        <div className="admin-page-header">
          <div>
            <p className="eyebrow">Administration</p>
            <h1>Products</h1>
          </div>
          <Link className="primary-button admin-header-button" to="/admin/products/new">Add product</Link>
        </div>
        {error && <div className="form-alert" role="alert">{error}</div>}
        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Product</th>
                <th>Category</th>
                <th>Price</th>
                <th>Stock</th>
                <th>Status</th>
                <th><span className="sr-only">Actions</span></th>
              </tr>
            </thead>
            <tbody>
              {products?.content.map((product) => (
                <tr key={product.id}>
                  <td>
                    <strong>{product.name}</strong>
                    {product.brand && <span>{product.brand}</span>}
                  </td>
                  <td>{categoryName(product.categoryId)}</td>
                  <td>{product.price.toLocaleString()} {product.currency}</td>
                  <td>{product.stockQuantity}</td>
                  <td><span className={`status-label status-${product.status.toLowerCase()}`}>{product.status}</span></td>
                  <td className="admin-row-actions">
                    <Link className="button button-outline button-small" to={`/admin/products/${product.id}/edit`}>Edit</Link>
                    <button type="button" className="button button-danger button-small" disabled={busyId === product.id}
                      onClick={() => handleDelete(product.id, product.name)}>
                      {busyId === product.id ? 'Saving...' : 'Deactivate'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {products?.content.length === 0 && (
          <section className="simple-state admin-empty">
            <p className="eyebrow">Catalog</p>
            <h2>No products yet</h2>
            <Link className="button button-primary" to="/admin/products/new">Add the first product</Link>
          </section>
        )}
        {products && products.totalPages > 1 && (
          <nav className="catalog-pagination" aria-label="Admin product pages">
            <button type="button" className="button button-secondary button-small" disabled={page === 0}
              onClick={() => setPage((value) => value - 1)}>Previous</button>
            <span>Page {page + 1} of {products.totalPages}</span>
            <button type="button" className="button button-secondary button-small" disabled={page + 1 >= products.totalPages}
              onClick={() => setPage((value) => value + 1)}>Next</button>
          </nav>
        )}
      </section>
    </PageContainer>
  )
}
