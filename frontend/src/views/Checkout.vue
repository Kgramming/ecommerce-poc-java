<template>
  <div class="wrap narrow">
    <div class="eyebrow" style="margin-top: 40px;">Checkout</div>
    <h1 class="display page-title">Review your order</h1>

    <!-- Success state -->
    <div v-if="placed" class="card success-box fade-in">
      <div class="success-mark">✓</div>
      <h2 class="display">Order placed</h2>
      <p class="muted">
        Order <RouterLink :to="`/admin/orders/${placed.id}`" class="order-link num">#{{ placed.id }}</RouterLink>
        is confirmed for <strong class="num">{{ formatMoney(placed.total_amount) }}</strong>.
        It's already indexed in the admin dashboard.
      </p>
      <div class="success-actions">
        <RouterLink to="/admin" class="btn btn-primary">View in dashboard</RouterLink>
        <RouterLink to="/" class="btn btn-ghost">Back to shop</RouterLink>
      </div>
    </div>

    <template v-else>
      <EmptyState
        v-if="lines.length === 0"
        mark="◌"
        title="Your bag is empty"
        body="Add a few products before checking out."
      >
        <RouterLink to="/" class="btn btn-ghost">Browse products</RouterLink>
      </EmptyState>

      <div v-else class="checkout-grid">
        <!-- Line items -->
        <section class="card items-card">
          <h3>Items</h3>
          <div v-for="l in lines" :key="l.product.id" class="line">
            <div>
              <div class="line-title">{{ l.product.title }}</div>
              <div class="faint line-sub num">{{ formatMoney(l.product.price) }} × {{ l.quantity }}</div>
            </div>
            <div class="num line-total">{{ formatMoney(l.product.price * l.quantity) }}</div>
          </div>
          <hr class="divider" />
          <div class="line total-line">
            <div class="muted">Total</div>
            <div class="num total-amt">{{ formatMoney(total) }}</div>
          </div>
        </section>

        <!-- Buyer + place -->
        <section class="card buyer-card">
          <h3>Buyer</h3>
          <div class="buyer">
            <div class="buyer-avatar">PP</div>
            <div>
              <div class="buyer-name">Priya Patel</div>
              <div class="faint buyer-sub">priya.patel@example.com · demo account</div>
            </div>
          </div>
          <p class="faint note">
            Prices are snapshotted at order time — later catalog changes
            never rewrite history.
          </p>
          <button class="btn btn-accent btn-block" :disabled="placing" @click="place">
            {{ placing ? 'Placing order…' : `Place order · ${formatMoney(total)}` }}
          </button>
          <p v-if="error" class="error-msg">{{ error }}</p>
        </section>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { api, formatMoney } from '../api/client.js'
import { useCart } from '../stores/cart.js'
import { toast } from '../stores/toast.js'
import EmptyState from '../components/EmptyState.vue'

const { lines, total, clear } = useCart()
const placing = ref(false)
const placed = ref(null)
const error = ref('')

async function place() {
  placing.value = true
  error.value = ''
  try {
    const order = await api.placeOrder({
      user_id: 2, // Priya Patel, demo account
      items: lines.value.map(l => ({ product_id: l.product.id, quantity: l.quantity })),
    })
    placed.value = order
    clear()
  } catch (e) {
    error.value = e.message
    toast.error('Order failed: ' + e.message)
  } finally {
    placing.value = false
  }
}
</script>

<style scoped>
.narrow { max-width: 880px; }
.page-title { font-size: 34px; margin: 10px 0 26px; }

.checkout-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 18px;
  align-items: start;
}
@media (max-width: 760px) { .checkout-grid { grid-template-columns: 1fr; } }

.items-card, .buyer-card { padding: 22px; }
.items-card h3, .buyer-card h3 { margin: 0 0 14px; font-size: 15px; font-weight: 700; }

.line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
}
.line-title { font-weight: 600; font-size: 14px; }
.line-sub { font-size: 12.5px; }
.line-total { font-weight: 600; }
.divider { margin: 8px 0; }
.total-line { padding-top: 12px; }
.total-amt { font-size: 19px; font-weight: 600; }

.buyer { display: flex; gap: 12px; align-items: center; margin-bottom: 14px; }
.buyer-avatar {
  width: 44px; height: 44px; border-radius: 50%;
  background: var(--ink); color: var(--paper);
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 15px; flex: none;
}
.buyer-name { font-weight: 600; font-size: 14.5px; }
.buyer-sub { font-size: 12.5px; }
.note { font-size: 12.5px; line-height: 1.6; margin: 0 0 16px; }
.error-msg { color: var(--danger); font-size: 13.5px; margin: 12px 0 0; }

.success-box { padding: 48px; text-align: center; }
.success-mark {
  width: 56px; height: 56px; border-radius: 50%;
  background: var(--status-shipped-wash); color: var(--status-shipped);
  font-size: 26px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 16px;
}
.success-box h2 { margin: 0 0 10px; font-size: 26px; }
.success-box p { margin: 0 0 24px; }
.order-link { color: var(--accent-deep); font-weight: 600; }
.success-actions { display: flex; gap: 10px; justify-content: center; }
</style>
