<template>
  <div class="wrap narrow">
    <RouterLink to="/admin" class="back-link">← All orders</RouterLink>

    <div v-if="loading"><Skeleton :rows="6" /></div>
    <div v-else-if="error" class="card state-box">
      <h3 class="display">Order not found</h3>
      <p class="muted">{{ error }}</p>
      <RouterLink to="/admin" class="btn btn-ghost">Back to search</RouterLink>
    </div>

    <template v-else-if="order">
      <div class="order-head">
        <div>
          <div class="eyebrow">Order</div>
          <h1 class="display num">#{{ order.id }}</h1>
          <p class="muted">{{ formatDateTime(order.order_date) }}</p>
        </div>
        <div class="head-badges">
          <StatusPill :status="order.status" />
          <span class="pill pill-neutral sync-badge" :class="{ syncing: syncing }">
            {{ syncing ? 'Syncing…' : 'In sync' }}
          </span>
        </div>
      </div>

      <div class="detail-grid">
        <!-- Receipt -->
        <section class="card receipt">
          <h3>Receipt <span class="faint note-inline">prices snapshotted at order time</span></h3>
          <table class="table">
            <thead>
              <tr>
                <th>Item</th>
                <th style="text-align:right">Unit</th>
                <th style="text-align:right">Qty</th>
                <th style="text-align:right">Line total</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="it in order.items" :key="it.product_id">
                <td>
                  <div class="item-title">{{ it.title }}</div>
                  <div class="faint item-id num">{{ it.product_id }}</div>
                </td>
                <td class="num" style="text-align:right">{{ formatMoney(it.unit_price) }}</td>
                <td class="num" style="text-align:right">{{ it.quantity }}</td>
                <td class="num" style="text-align:right">{{ formatMoney(it.unit_price * it.quantity) }}</td>
              </tr>
            </tbody>
          </table>
          <div class="receipt-total">
            <span class="muted">Total charged</span>
            <span class="num total-amt">{{ formatMoney(order.total_amount) }}</span>
          </div>
        </section>

        <!-- Side panel -->
        <aside class="side">
          <section class="card side-card">
            <h3>Customer</h3>
            <div class="cust">
              <div class="buyer-avatar">{{ initials }}</div>
              <div>
                <div class="cust-name">{{ order.user.name }}</div>
                <div class="faint cust-mail">{{ order.user.email }}</div>
              </div>
            </div>
          </section>

          <section class="card side-card">
            <h3>Order status</h3>
            <p class="faint status-note">
              Postgres is the source of truth; the change is written there
              first, then mirrored to Elasticsearch.
            </p>
            <div class="field">
              <label for="status-sel">Status</label>
              <select
                id="status-sel"
                v-model="newStatus"
                class="select"
                :disabled="updating"
              >
                <option v-for="s in statuses" :key="s" :value="s">{{ s }}</option>
              </select>
            </div>
            <button
              class="btn btn-primary btn-block"
              :disabled="updating || newStatus === order.status"
              @click="updateStatus"
              style="margin-top: 12px;"
            >
              {{ updating ? 'Updating…' : 'Update status' }}
            </button>
          </section>
        </aside>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { api, formatMoney, formatDateTime } from '../api/client.js'
import { toast } from '../stores/toast.js'
import StatusPill from '../components/StatusPill.vue'
import Skeleton from '../components/Skeleton.vue'

const props = defineProps({ id: { type: String, required: true } })
const statuses = ['PENDING', 'PROCESSING', 'SHIPPED']

const order = ref(null)
const loading = ref(true)
const error = ref('')
const newStatus = ref('')
const updating = ref(false)
const syncing = ref(false)

const initials = computed(() =>
  (order.value?.user.name || '?').split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase()
)

async function fetchOrder() {
  loading.value = true
  error.value = ''
  try {
    order.value = await api.order(props.id)
    newStatus.value = order.value.status
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function updateStatus() {
  updating.value = true
  syncing.value = true
  try {
    order.value = await api.setStatus(props.id, newStatus.value)
    newStatus.value = order.value.status
    toast.success(`Order #${props.id} moved to ${order.value.status}`)
  } catch (e) {
    toast.error('Status update failed: ' + e.message)
  } finally {
    updating.value = false
    // dual-write mirrors to ES inside the same request; the badge settles
    // once the round-trip is done.
    setTimeout(() => { syncing.value = false }, 900)
  }
}

onMounted(fetchOrder)
</script>

<style scoped>
.narrow { max-width: 960px; padding-top: 32px; }
.back-link {
  display: inline-block;
  text-decoration: none;
  font-size: 13.5px; font-weight: 600;
  color: var(--ink-soft);
  margin-bottom: 18px;
}
.back-link:hover { color: var(--ink); }

.order-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 22px;
}
.order-head h1 { font-size: 40px; margin: 6px 0 4px; }
.order-head p { margin: 0; }
.head-badges { display: flex; gap: 8px; padding-top: 10px; }
.sync-badge.syncing { color: var(--status-processing); background: var(--status-processing-wash); }
.sync-badge:not(.syncing) { color: var(--status-shipped); background: var(--status-shipped-wash); }

.detail-grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 18px;
  align-items: start;
}
@media (max-width: 820px) { .detail-grid { grid-template-columns: 1fr; } }

.receipt { padding: 22px; }
.receipt h3 { margin: 0 0 8px; font-size: 15px; }
.note-inline { font-size: 12px; font-weight: 400; }
.receipt-total {
  display: flex; justify-content: space-between; align-items: baseline;
  padding-top: 14px; margin-top: 6px;
  border-top: 1px solid var(--line);
}
.total-amt { font-size: 20px; font-weight: 600; }
.item-title { font-weight: 600; font-size: 14px; }
.item-id { font-size: 11px; }

.side { display: flex; flex-direction: column; gap: 18px; }
.side-card { padding: 20px; }
.side-card h3 { margin: 0 0 12px; font-size: 15px; }
.cust { display: flex; gap: 12px; align-items: center; }
.buyer-avatar {
  width: 42px; height: 42px; border-radius: 50%;
  background: var(--ink); color: var(--paper);
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 14px; flex: none;
}
.cust-name { font-weight: 600; font-size: 14.5px; }
.cust-mail { font-size: 12.5px; }
.status-note { font-size: 12.5px; line-height: 1.6; margin: 0 0 14px; }

.state-box { padding: 48px; text-align: center; }
.state-box h3 { margin: 0 0 8px; font-size: 22px; }
.state-box p { margin: 0 0 18px; }
</style>
