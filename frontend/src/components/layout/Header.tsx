import { Link } from 'react-router-dom'
import { Navigation } from './Navigation'
import { useAuth } from '../../features/auth/hooks/useAuth'
import { useState } from 'react'
import { CartDrawer } from '../../features/cart/components/CartDrawer'

export function Header() {
  const { isAuthenticated, user, logout, hasRole } = useAuth()
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const [cartOpen, setCartOpen] = useState(false)

  const handleLogout = async () => {
    setIsLoggingOut(true)
    try {
      await logout()
    }
    finally {
      setIsLoggingOut(false)
    }
  }

  return (
    <header className="site-header">
      <div className="header-inner">
        <Link className="wordmark" to="/" aria-label="North and Field home">
          NORTH <span>&amp;</span> FIELD
        </Link>
        <Navigation />
        <div className="header-actions" aria-label="Account actions">
          {isAuthenticated ? (
            <>
              <Link className="header-user" to="/profile">{user?.fullName}</Link>
              {hasRole('ADMIN') && (
                <Link className="button button-ghost button-small" to="/admin/products">Admin</Link>
              )}
              <button className="button button-ghost button-small" type="button" onClick={handleLogout} disabled={isLoggingOut}>
                {isLoggingOut ? 'Signing out' : 'Sign out'}
              </button>
            </>
          ) : (
            <Link className="button button-secondary button-small" to="/login">Sign in</Link>
          )}
          <button className="button button-outline button-small" type="button" onClick={() => setCartOpen(true)}>Bag</button>
        </div>
      </div>
      {cartOpen && (
        <>
          <button className="drawer-backdrop" type="button" aria-label="Close shopping bag"
            onClick={() => setCartOpen(false)} />
          <CartDrawer open={cartOpen} onClose={() => setCartOpen(false)} refreshKey={cartOpen ? 1 : 0} />
        </>
      )}
    </header>
  )
}
