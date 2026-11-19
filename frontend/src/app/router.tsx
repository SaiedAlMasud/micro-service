import { createBrowserRouter } from 'react-router-dom'
import { AppLayout } from '../components/layout/AppLayout'
import { HomePage } from '../pages/public/HomePage'
import { LoginPage } from '../pages/public/LoginPage'
import { RegisterPage } from '../pages/public/RegisterPage'
import { ProductDetailPage } from '../pages/public/ProductDetailPage'
import { RoleRoute } from './routeGuards'
import { AdminProductsPage } from '../pages/admin/AdminProductsPage'
import { AdminProductNewPage } from '../pages/admin/AdminProductNewPage'
import { AdminProductEditPage } from '../pages/admin/AdminProductEditPage'
import { ProtectedRoute } from './routeGuards'
import { ProfilePage } from '../pages/account/ProfilePage'

export const router = createBrowserRouter([
  {
    element: <AppLayout />,
    children: [
      { path: '/', element: <HomePage /> },
      { path: '/catalog', element: <HomePage /> },
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
      { path: '/products/:id', element: <ProductDetailPage /> },
      {
        path: '/profile',
        element: <ProtectedRoute><ProfilePage /></ProtectedRoute>,
      },
      {
        path: '/admin/products',
        element: <RoleRoute role="ADMIN"><AdminProductsPage /></RoleRoute>,
      },
      {
        path: '/admin/products/new',
        element: <RoleRoute role="ADMIN"><AdminProductNewPage /></RoleRoute>,
      },
      {
        path: '/admin/products/:id/edit',
        element: <RoleRoute role="ADMIN"><AdminProductEditPage /></RoleRoute>,
      },
      {
        path: '*',
        element: (
          <main className="page-container">
            <section className="simple-state" aria-labelledby="not-found-title">
              <p className="eyebrow">404</p>
              <h1 id="not-found-title">Page not found</h1>
              <p>The requested page is outside the current storefront catalog.</p>
            </section>
          </main>
        ),
      },
    ],
  },
])
