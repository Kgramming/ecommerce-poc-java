import { computed, reactive } from 'vue'

/* Tiny cart store shared by the storefront and checkout.
   Keyed by product id; each entry holds the product snapshot + quantity.
   Persisted to localStorage so a refresh doesn't empty the bag. */

const KEY = 'meridian-cart-v1'

function load() {
  try {
    const raw = localStorage.getItem(KEY)
    return raw ? JSON.parse(raw) : {}
  } catch { return {} }
}

const items = reactive(load())

function save() {
  try { localStorage.setItem(KEY, JSON.stringify(items)) } catch { /* ignore */ }
}

export function useCart() {
  const lines = computed(() =>
    Object.values(items).sort((a, b) => a.product.title.localeCompare(b.product.title))
  )
  const count = computed(() =>
    Object.values(items).reduce((n, l) => n + l.quantity, 0)
  )
  const total = computed(() =>
    Object.values(items).reduce((n, l) => n + l.quantity * l.product.price, 0)
  )

  function add(product, qty = 1) {
    const id = product.id
    if (items[id]) {
      items[id].quantity += qty
    } else {
      items[id] = { product: { ...product }, quantity: qty }
    }
    save()
  }

  function setQty(id, qty) {
    if (!items[id]) return
    if (qty <= 0) delete items[id]
    else items[id].quantity = qty
    save()
  }

  function remove(id) {
    delete items[id]
    save()
  }

  function clear() {
    for (const k of Object.keys(items)) delete items[k]
    save()
  }

  return { lines, count, total, add, setQty, remove, clear }
}
