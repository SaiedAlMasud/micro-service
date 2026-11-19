export const env = {
  userApiUrl: import.meta.env.VITE_USER_API_URL ?? 'http://localhost:8081',
  productApiUrl: import.meta.env.VITE_PRODUCT_API_URL ?? 'http://localhost:8082',
  cartApiUrl: import.meta.env.VITE_CART_API_URL ?? 'http://localhost:8083',
}
