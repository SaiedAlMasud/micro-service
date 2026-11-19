import { useState, type FormEvent } from 'react'
import { FormField } from '../../auth/components/FormField'
import type { CategoryResponse, ProductResponse } from '../../catalog/types/product'
import type { ProductAdminForm, ProductRequest } from '../types/productAdmin'

interface ProductFormProps {
  categories: CategoryResponse[]
  product?: ProductResponse
  submitting: boolean
  error: string
  onSubmit: (request: ProductRequest) => Promise<void>
  onCancel: () => void
}

function initialForm(product?: ProductResponse): ProductAdminForm {
  return {
    name: product?.name ?? '',
    description: product?.description ?? '',
    categoryId: product?.categoryId ?? '',
    brand: product?.brand ?? '',
    price: product ? String(product.price) : '',
    currency: product?.currency ?? 'BDT',
    stockQuantity: product ? String(product.stockQuantity) : '0',
    imageUrl: product?.imageUrl ?? '',
  }
}

export function ProductForm({
  categories,
  product,
  submitting,
  error,
  onSubmit,
  onCancel,
}: ProductFormProps) {
  const [form, setForm] = useState<ProductAdminForm>(() => initialForm(product))
  const [fieldErrors, setFieldErrors] = useState<Partial<Record<keyof ProductAdminForm, string>>>({})

  const validate = () => {
    const errors: Partial<Record<keyof ProductAdminForm, string>> = {}
    if (!form.name.trim()) errors.name = 'Product name is required'
    else if (form.name.trim().length > 200) errors.name = 'Maximum 200 characters'
    if (!form.categoryId) errors.categoryId = 'Choose a category'
    if (form.brand.length > 100) errors.brand = 'Maximum 100 characters'
    if (!form.price || Number(form.price) <= 0) errors.price = 'Price must be greater than 0'
    if (!/^[A-Za-z]{3}$/.test(form.currency)) errors.currency = 'Currency must be 3 letters'
    if (!/^\d+$/.test(form.stockQuantity) || Number(form.stockQuantity) < 0) {
      errors.stockQuantity = 'Stock must be 0 or greater'
    }
    if (form.imageUrl.length > 500) errors.imageUrl = 'Maximum 500 characters'
    setFieldErrors(errors)
    return Object.keys(errors).length === 0
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!validate()) return
    await onSubmit({
      name: form.name.trim(),
      description: form.description.trim() || undefined,
      categoryId: form.categoryId,
      brand: form.brand.trim() || undefined,
      price: Number(form.price),
      currency: form.currency.toUpperCase(),
      stockQuantity: Number(form.stockQuantity),
      imageUrl: form.imageUrl.trim() || undefined,
    })
  }

  return (
    <form className="admin-form" onSubmit={handleSubmit} noValidate>
      {error && <div className="form-alert" role="alert">{error}</div>}
      <FormField id="name" label="Product name" value={form.name}
        onChange={(event) => setForm({ ...form, name: event.target.value })} error={fieldErrors.name} />
      <div className="form-field">
        <label htmlFor="description">Description</label>
        <textarea id="description" rows={4} value={form.description}
          onChange={(event) => setForm({ ...form, description: event.target.value })} />
      </div>
      <div className="form-field">
        <label htmlFor="categoryId">Category</label>
        <select id="categoryId" value={form.categoryId}
          onChange={(event) => setForm({ ...form, categoryId: event.target.value })}
          aria-invalid={Boolean(fieldErrors.categoryId)}>
          <option value="">Choose category</option>
          {categories.map((category) => (
            <option value={category.id} key={category.id}>{category.name}</option>
          ))}
        </select>
        {fieldErrors.categoryId && <p className="field-error" id="categoryId-error">{fieldErrors.categoryId}</p>}
      </div>
      <div className="admin-form-grid">
        <FormField id="brand" label="Brand" value={form.brand}
          onChange={(event) => setForm({ ...form, brand: event.target.value })} error={fieldErrors.brand} />
        <FormField id="price" label="Price (BDT)" type="number" min="0.01" step="0.01" value={form.price}
          onChange={(event) => setForm({ ...form, price: event.target.value })} error={fieldErrors.price} />
        <FormField id="currency" label="Currency" value={form.currency} maxLength={3}
          onChange={(event) => setForm({ ...form, currency: event.target.value.toUpperCase() })} error={fieldErrors.currency} />
        <FormField id="stockQuantity" label="Stock quantity" type="number" min="0" value={form.stockQuantity}
          onChange={(event) => setForm({ ...form, stockQuantity: event.target.value })} error={fieldErrors.stockQuantity} />
      </div>
      <FormField id="imageUrl" label="Image URL" type="url" value={form.imageUrl}
        onChange={(event) => setForm({ ...form, imageUrl: event.target.value })} error={fieldErrors.imageUrl} />
      <div className="admin-form-actions">
        <button type="button" className="button button-secondary" onClick={onCancel} disabled={submitting}>Cancel</button>
        <button type="submit" className="button button-primary" disabled={submitting}>
          {submitting ? 'Saving...' : product ? 'Save changes' : 'Create product'}
        </button>
      </div>
    </form>
  )
}
