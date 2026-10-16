<template>
  <Teleport to="body">
    <div v-if="open" class="drawer-backdrop fade-in" @click.self="$emit('close')"></div>
    <aside v-if="open" class="drawer card" aria-label="Shopping bag">
      <div class="drawer-head">
        <h3 class="display">Your bag <span class="faint num">({{ count }})</span></h3>
        <button class="modal-x" @click="$emit('close')" aria-label="Close">✕</button>
      </div>
      <div class="drawer-body">
        <EmptyState
          v-if="lines.length === 0"
          mark="◌"
          title="Your bag is empty"
          body="Browse the shelves and add something useful."
        >
          <button class="btn btn-ghost" @click="$emit('close')">Continue shopping</button>
        </EmptyState>
        <div v-else class="lines">
          <div v-for="l in lines" :key="l.product.id" class="line">
            <div class="line-info">
              <div class="line-title">{{ l.product.title }}</div>
              <div class="line-price num muted">{{ formatMoney(l.product.price) }} each</div>
            </div>
            <div class="qty">
              <button @click="setQty(l.product.id, l.quantity - 1)" aria-label="Decrease">−</button>
              <span class="num">{{ l.quantity }}</span>
              <button @click="setQty(l.product.id, l.quantity + 1)" aria-label="Increase">+</button>
            </div>
            <div class="num line-total">{{ formatMoney(l.product.price * l.quantity) }}</div>
          </div>
        </div>
      </div>
      <div v-if="lines.length" class="drawer-foot">
        <div class="drawer-total">
          <span class="muted">Subtotal</span>
          <span class="num total-amt">{{ formatMoney(total) }}</span>
        </div>
        <button class="btn btn-accent btn-block" @click="$emit('checkout')">
          Checkout
        </button>
      </div>
    </aside>
  </Teleport>
</template>

<script setup>
import { formatMoney } from '../api/client.js'
import { useCart } from '../stores/cart.js'
import EmptyState from './EmptyState.vue'

defineProps({ open: Boolean })
defineEmits(['close', 'checkout'])
const { lines, count, total, setQty } = useCart()
</script>

<style scoped>
.drawer-backdrop {
  position: fixed; inset: 0;
  background: rgba(28, 27, 23, 0.4);
  z-index: 140;
}
.drawer {
  position: fixed;
  top: 0; right: 0; bottom: 0;
  width: 400px; max-width: 94vw;
  z-index: 141;
  border-radius: 0;
  border-top: none; border-right: none; border-bottom: none;
  display: flex; flex-direction: column;
  animation: slideIn 0.25s var(--ease);
}
@keyframes slideIn {
  from { transform: translateX(40px); opacity: 0; }
  to { transform: none; opacity: 1; }
}
.drawer-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 22px;
  border-bottom: 1px solid var(--line-soft);
}
.drawer-head h3 { margin: 0; font-size: 20px; }
.modal-x {
  border: none; background: none; font-size: 15px;
  color: var(--ink-faint); cursor: pointer; padding: 6px; border-radius: 6px;
}
.modal-x:hover { color: var(--ink); background: var(--paper-2); }
.drawer-body { flex: 1; overflow-y: auto; padding: 18px 22px; }
.lines { display: flex; flex-direction: column; gap: 16px; }
.line {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: 12px;
  align-items: center;
}
.line-title { font-weight: 600; font-size: 14px; }
.line-price { font-size: 12.5px; }
.qty {
  display: flex; align-items: center; gap: 10px;
  border: 1px solid var(--line); border-radius: 999px; padding: 4px 8px;
}
.qty button {
  border: none; background: none; font-size: 15px; cursor: pointer;
  color: var(--ink-soft); width: 22px; height: 22px; border-radius: 50%;
}
.qty button:hover { background: var(--paper-2); color: var(--ink); }
.line-total { font-weight: 600; min-width: 64px; text-align: right; }
.drawer-foot { padding: 18px 22px 22px; border-top: 1px solid var(--line-soft); }
.drawer-total {
  display: flex; justify-content: space-between; align-items: baseline;
  margin-bottom: 14px;
}
.total-amt { font-size: 20px; font-weight: 600; }
</style>
