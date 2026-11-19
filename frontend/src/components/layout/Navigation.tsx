import { NavLink } from 'react-router-dom'

export function Navigation() {
  return (
    <nav className="primary-navigation" aria-label="Primary navigation">
      <NavLink to="/" end>New arrivals</NavLink>
      <NavLink to="/catalog">Shop all</NavLink>
      <a href="#categories">Categories</a>
    </nav>
  )
}
