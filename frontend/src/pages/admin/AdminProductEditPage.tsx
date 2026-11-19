import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { PageContainer } from '../../components/layout/PageContainer'
import {
  fetchAdminCategories,
  fetchAdminProduct,
  updateAdminProduct,
} from '../../features/admin/api/productAdminApi'
import { ProductForm } from '../../features/admin/components/ProductForm'
import type { CategoryResponse, ProductResponse } from '../../features/catalog/types/product'
import type { ProductRequest } from '../../features/admin/types/productAdmin'

export function AdminProductEditPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [categories, setCategories] = useState<CategoryResponse[]>([])
  const [product, setProduct] = useState<ProductResponse | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!id) return
    Promise.all([fetchAdminCategories(), fetchAdminProduct(id)])
      .then(([categoryList, productValue]) => {
        setCategories(categoryList)
        setProduct(productValue)
      })
      .catch((reason: { message?: string }) => {
        setError(reason.message ?? 'Unable to load product')
      })
  }, [id])

  const handleSubmit = async (request: ProductRequest) => {
    if (!id) return
    setSubmitting(true)
    setError('')
    try {
      await updateAdminProduct(id, request)
      navigate('/admin/products', { replace: true })
    }
    catch (reason) {
      setError((reason as { message?: string }).message ?? 'Unable to update product')
    }
    finally {
      setSubmitting(false)
    }
  }

  return (
    <PageContainer>
      <section className="admin-page">
        <div className="admin-page-header">
          <div>
            <p className="eyebrow">Administration</p>
            <h1>Edit product</h1>
          </div>
        </div>
        {error && <div className="form-alert" role="alert">{error}</div>}
        {product && (
          <ProductForm categories={categories} product={product} submitting={submitting}
            error="" onSubmit={handleSubmit} onCancel={() => navigate('/admin/products')} />
        )}
      </section>
    </PageContainer>
  )
}
