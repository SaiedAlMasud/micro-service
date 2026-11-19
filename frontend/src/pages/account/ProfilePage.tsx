import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { PageContainer } from '../../components/layout/PageContainer'
import { fetchCurrentUser } from '../../features/auth/api/userApi'
import { useAuth } from '../../features/auth/hooks/useAuth'
import type { ApiError } from '../../lib/error'
import type { UserResponse } from '../../features/auth/types/auth'

function formatDate(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

export function ProfilePage() {
  const { logout, hasRole } = useAuth()
  const [profile, setProfile] = useState<UserResponse | null>(null)
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [isLoggingOut, setIsLoggingOut] = useState(false)

  useEffect(() => {
    let active = true
    fetchCurrentUser()
      .then((value) => {
        if (active) setProfile(value)
      })
      .catch((reason) => {
        if (active) setError((reason as ApiError).message || 'Unable to load profile')
      })
      .finally(() => {
        if (active) setIsLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  const handleLogout = async () => {
    setIsLoggingOut(true)
    try {
      await logout()
      window.location.assign('/login')
    }
    finally {
      setIsLoggingOut(false)
    }
  }

  return (
    <PageContainer>
      <section className="profile-page" aria-labelledby="profile-heading">
        <div className="profile-header">
          <div>
            <p className="eyebrow">Account</p>
            <h1 id="profile-heading">Profile</h1>
          </div>
          {profile && (
            <span className={`status-label status-${profile.status.toLowerCase()}`}>
              {profile.status}
            </span>
          )}
        </div>

        {isLoading && (
          <p className="profile-state">Loading account details...</p>
        )}

        {error && (
          <div className="form-alert" role="alert">
            {error}
            <button className="button button-secondary button-small profile-retry"
              type="button" onClick={() => window.location.reload()}>
              Retry
            </button>
          </div>
        )}

        {profile && (
          <div className="profile-layout">
            <aside className="profile-summary" aria-label="Profile summary">
              <div className="profile-avatar" aria-hidden="true">
                {profile.fullName
                  .split(/\s+/)
                  .slice(0, 2)
                  .map((part) => part[0]?.toUpperCase())
                  .join('')}
              </div>
              <h2>{profile.fullName}</h2>
              <p>{profile.role}</p>
            </aside>

            <dl className="profile-details">
              <div>
                <dt>Full name</dt>
                <dd>{profile.fullName}</dd>
              </div>
              <div>
                <dt>Email</dt>
                <dd>{profile.email}</dd>
              </div>
              <div>
                <dt>Phone</dt>
                <dd>{profile.phone || 'Not provided'}</dd>
              </div>
              <div>
                <dt>User ID</dt>
                <dd className="profile-id">{profile.id}</dd>
              </div>
              <div>
                <dt>Role</dt>
                <dd>{profile.role}</dd>
              </div>
              <div>
                <dt>Status</dt>
                <dd>{profile.status}</dd>
              </div>
              <div>
                <dt>Member since</dt>
                <dd>{formatDate(profile.createdAt)}</dd>
              </div>
              <div>
                <dt>Last updated</dt>
                <dd>{formatDate(profile.updatedAt)}</dd>
              </div>
            </dl>

            <div className="profile-actions">
              {hasRole('ADMIN') && (
                <Link className="button button-secondary" to="/admin/products">
                  Manage products
                </Link>
              )}
              <button className="button button-danger"
                type="button" onClick={handleLogout} disabled={isLoggingOut}>
                {isLoggingOut ? 'Signing out...' : 'Sign out'}
              </button>
            </div>
          </div>
        )}
      </section>
    </PageContainer>
  )
}
