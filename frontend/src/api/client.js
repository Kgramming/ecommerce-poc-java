/* Thin fetch wrapper around the FastAPI backend (served via the Vite /api proxy). */

async function req(method, path, body) {
  let res
  try {
    res = await fetch('/api' + path, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  } catch {
    throw new Error('Cannot reach the API — is the backend running?')
  }
  let data = null
  try { data = await res.json() } catch { /* non-JSON */ }
  if (!res.ok) {
    const detail = data && data.detail ? data.detail : `Request failed (HTTP ${res.status})`
    throw new Error(detail)
  }
  return data
}

const qs = (params = {}) => {
  const s = new URLSearchParams()
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== null && v !== '') s.append(k, v)
  }
  const str = s.toString()
  return str ? '?' + str : ''
}

export const api = {
  products: (params) => req('GET', '/products' + qs(params)),
  product: (id) => req('GET', `/products/${id}`),
  createProduct: (data) => req('POST', '/products', data),
  updateProduct: (id, data) => req('PUT', `/products/${id}`, data),

  placeOrder: (data) => req('POST', '/orders', data),
  order: (id) => req('GET', `/orders/${id}`),
  setStatus: (id, status) => req('PATCH', `/orders/${id}/status`, { status }),

  searchOrders: (data) => req('POST', '/search/orders', data),
}

export function formatMoney(n) {
  return '₹' + Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function formatDate(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}

export function formatDateTime(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) +
    ', ' + d.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' })
}
