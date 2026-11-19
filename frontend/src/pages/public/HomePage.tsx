import { useEffect, useState } from 'react'
import { PageContainer } from '../../components/layout/PageContainer'
import { fetchCategories, fetchProducts } from '../../features/catalog/api/catalogApi'
import { ProductCard } from '../../features/catalog/components/ProductCard'
import type { CategoryResponse, ProductPage } from '../../features/catalog/types/product'

export function HomePage() {
  const [categories, setCategories] = useState<CategoryResponse[]>([])
  const [categoryId, setCategoryId] = useState('')
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const [products, setProducts] = useState<ProductPage | null>(null)
  const [error, setError] = useState('')
  const [cartRefresh, setCartRefresh] = useState(0)

  useEffect(() => {
    fetchCategories().then(setCategories).catch((reason: { message?: string }) => {
      setError(reason.message ?? 'Unable to load categories')
    })
  }, [])

  useEffect(() => {
    fetchProducts({
      categoryId: categoryId || undefined,
      q: query.trim() || undefined,
      page,
      size: 20,
    }).then(setProducts).catch((reason: { message?: string }) => {
      setError(reason.message ?? 'Unable to load products')
    })
  }, [categoryId, query, page])

  const categoryName = (id: string) => categories.find((category) => category.id === id)?.name

  return (
    <PageContainer>
      <section className="catalog-intro" aria-labelledby="catalog-heading">
        <p className="eyebrow">Catalog / Active collection</p>
        <div className="catalog-heading-row">
          <h1 id="catalog-heading">Objects for daily<br />work and living.</h1>
          <p>A compact collection of practical tools and considered accessories.</p>
        </div>
      </section>

      <section className="catalog-controls" id="categories" aria-label="Product controls">
        <label className="catalog-search">
          <span>Search</span>
          <input value={query} onChange={(event) => {
            setQuery(event.target.value)
            setPage(0)
          }} placeholder="Name or brand" />
        </label>
        <div className="category-bar" aria-label="Product categories">
          <button type="button" className={!categoryId ? 'button button-filter is-active' : 'button button-filter'}
            onClick={() => { setCategoryId(''); setPage(0) }}>All</button>
          {categories.map((category) => (
            <button type="button" key={category.id}
              className={categoryId === category.id ? 'button button-filter is-active' : 'button button-filter'}
              onClick={() => { setCategoryId(category.id); setPage(0) }}>{category.name}</button>
          ))}
          <span className="catalog-count">{products?.totalElements ?? 0} objects</span>
        </div>
      </section>

      {error && <p className="form-alert catalog-error" role="alert">{error}</p>}
      {products && products.content.length === 0 && !error && (
        <section className="simple-state">
          <p className="eyebrow">Catalog</p>
          <h2>No matching products</h2>
          <p>Try a different search or category.</p>
        </section>
      )}
      {products && products.content.length > 0 && (
        <section className="product-grid" aria-label="Product catalog">
          {products.content.map((product) => (
            <ProductCard key={product.id} product={product}
              categoryName={categoryName(product.categoryId)}
              onCartChanged={() => setCartRefresh((value) => value + 1)} />
          ))}
        </section>
      )}

      {products && products.totalPages > 1 && (
        <nav className="catalog-pagination" aria-label="Catalog pages">
          <button type="button" className="button button-secondary button-small" disabled={page === 0}
            onClick={() => setPage((value) => value - 1)}>Previous</button>
          <span>Page {page + 1} of {products.totalPages}</span>
          <button type="button" className="button button-secondary button-small" disabled={page + 1 >= products.totalPages}
            onClick={() => setPage((value) => value + 1)}>Next</button>
        </nav>
      )}
      {cartRefresh >= 0 && <span hidden aria-hidden="true">{cartRefresh}</span>}
    </PageContainer>
  )
}
