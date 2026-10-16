import { reactive } from 'vue'

/* Minimal toast bus: components call toast.success()/toast.error(),
   ToastHost renders them. */

const state = reactive({ items: [] })
let nextId = 1

function push(kind, message) {
  const id = nextId++
  state.items.push({ id, kind, message })
  setTimeout(() => dismiss(id), 4200)
}

function dismiss(id) {
  const i = state.items.findIndex(t => t.id === id)
  if (i >= 0) state.items.splice(i, 1)
}

export const toast = {
  state,
  success: (m) => push('success', m),
  error: (m) => push('error', m),
  dismiss,
}
