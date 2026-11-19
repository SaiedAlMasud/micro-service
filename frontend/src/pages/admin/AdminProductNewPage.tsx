import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { PageContainer } from '../../components/layout/PageContainer'
import { createAdminProduct, fetchAdminCategories } from '../../features/admin/api/productAdminApi'
import { ProductForm } from '../../features/admin/components/ProductForm'
import type { CategoryResponse } from '../../features/catalog/types/product'
import type { ProductRequest } from '../../features/admin/types/productAdmin'

export function AdminProductNewPage() {
  const navigate = useNavigate()
  const [categories, setCategories] = useState<CategoryResponse[]>([])
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    fetchAdminCategories().then(setCategories).catch((reason: { message?: string }) => {
      setError(reason.message ?? 'Unable to load categories')
    })
  }, [])

  const handleSubmit = async (request: ProductRequest) => {
    setSubmitting(true)
    setError('')
    try {
      await createAdminProduct(request)
      navigate('/admin/products', { replace: true })
    }
    catch (reason) {
      setError((reason as { message?: string }).message ?? 'Unable to create product')
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
            <h1>Add product</h1>
          </div>
        </div>
        <ProductForm categories={categories} submitting={submitting} error={error}
          onSubmit={handleSubmit} onCancel={() => navigate('/admin/products')} />
      </section>
    </PageContainer>
  )
}
