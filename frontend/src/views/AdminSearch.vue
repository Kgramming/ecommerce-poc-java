<template>
  <div class="wrap admin">
    <div class="admin-head">
      <div>
        <div class="eyebrow">Back office</div>
        <h1 class="display">Order search</h1>
        <p class="muted admin-sub">
          Full-text search over customers and line items, with filters and live
          KPIs — served by Elasticsearch. PostgreSQL stays the source of truth.
        </p>
      </div>
    </div>

    <!-- Omni search -->
    <div class="search-hero card">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="11" cy="11" r="7"/><path d="M20 20l-3.5-3.5"/>
      </svg>
      <input
        v-model="query"
        class="omni"
        type="search"
        placeholder="Search orders — try “wireless”, “wendy”, “headphones”…"
        @input="onQueryInput"
      />
    </div>

    <div class="admin-grid">
      <!-- Filters -->
      <aside class="card filters">
        <div class="filters-head">
          <h3>Filters</h3>
          <button v-if="hasFilters" class="clear-btn" @click="clearFilters">Clear</button>
        </div>

        <div class="filter-group">
          <div class="filter-label">Status</div>
          <label v-for="s in statuses" :key="s" class="check">
            <input type="checkbox" :value="s" v-model="selectedStatuses" @change="search" />
            <StatusPill :status="s" />
          </label>
        </div>

        <div class="filter-group">
          <div class="filter-label">Order date</div>
          <div class="field-col">
            <input v-model="dateFrom" type="date" class="input" @change="search" />
            <input v-model="dateTo" type="date" class="input" @change="search" />
          </div>
        </div>

        <div class="filter-group">
          <div class="filter-label">Order total</div>
          <div class="field-row">
            <input v-model.number="minTotal" type="number" min="0" step="1" placeholder="Min $" class="input" @change="search" />
            <input v-model.number="maxTotal" type="number" min="0" step="1" placeholder="Max $" class="input" @change="search" />
          </div>
        </div>
      </aside>

      <!-- Results -->
      <section class="results">
        <div class="kpis">
          <KpiCard label="Revenue" :value="formatMoney(kpis.total_revenue || 0)" sub="across matching orders" />
          <KpiCard label="Orders" :value="total" sub="matching the filters" />
          <KpiCard label="Average order" :value="formatMoney(avgOrder)" sub="mean of matching orders" />
          <KpiCard
            label="By status"
            :value="statusSummary"
            sub="pending · processing · shipped"
          />
        </div>

        <div class="card table-card">
          <Skeleton v-if="loading" :rows="8" />
          <div v-else-if="error" class="state-box">
            <h3 class="display">Search failed</h3>
            <p class="muted">{{ error }}</p>
            <button class="btn btn-ghost" @click="search">Retry</button>
          </div>
          <EmptyState
            v-else-if="orders.length === 0"
            mark="◌"
            title="No orders found"
            body="No orders match this combination of search and filters."
          >
            <button class="btn btn-ghost" @click="clearFilters">Clear search & filters</button>
          </EmptyState>
          <table v-else class="table">
            <thead>
              <tr>
                <th>Order</th>
                <th>Date</th>
                <th>Customer</th>
                <th>Items</th>
                <th style="text-align:right">Total</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="o in orders"
                :key="o.order_id"
                class="clickable"
                @click="$router.push(`/admin/orders/${o.order_id}`)"
              >
                <td class="num order-id">#{{ o.order_id }}</td>
                <td class="muted">{{ formatDate(o.order_date) }}</td>
                <td>
                  <div class="cust-name">{{ o.customer.name }}</div>
                  <div class="faint cust-mail">{{ o.customer.email }}</div>
                </td>
                <td class="num">{{ (o.items || []).length }}</td>
                <td class="num" style="text-align:right">{{ formatMoney(o.total_amount) }}</td>
                <td><StatusPill :status="o.status" /></td>
              </tr>
            </tbody>
          </table>

          <div v-if="!loading && total > 0" class="pager">
            <span class="faint pager-info">
              Page <span class="num">{{ page }}</span> of <span class="num">{{ totalPages }}</span>
              · <span class="num">{{ total }}</span> orders
            </span>
            <div class="pager-btns">
              <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="page--, search()">← Prev</button>
              <button class="btn btn-ghost btn-sm" :disabled="page >= totalPages" @click="page++, search()">Next →</button>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { api, formatMoney, formatDate } from '../api/client.js'
import StatusPill from '../components/StatusPill.vue'
import KpiCard from '../components/KpiCard.vue'
import EmptyState from '../components/EmptyState.vue'
import Skeleton from '../components/Skeleton.vue'

const statuses = ['PENDING', 'PROCESSING', 'SHIPPED']
const query = ref('')
const selectedStatuses = ref([])
const dateFrom = ref('')
const dateTo = ref('')
const minTotal = ref(null)
const maxTotal = ref(null)
const page = ref(1)
const pageSize = 15

const orders = ref([])
const total = ref(0)
const kpis = ref({ total_revenue: 0, count_by_status: {} })
const loading = ref(true)
const error = ref('')

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))
const avgOrder = computed(() =>
  total.value ? (kpis.value.total_revenue || 0) / total.value : 0
)
const statusSummary = computed(() => {
  const c = kpis.value.count_by_status || {}
  return `${c.PENDING || 0} / ${c.PROCESSING || 0} / ${c.SHIPPED || 0}`
})
const hasFilters = computed(() =>
  query.value || selectedStatuses.value.length || dateFrom.value ||
  dateTo.value || minTotal.value != null || maxTotal.value != null
)

let debounce = null

function buildBody() {
  const body = { page: page.value, page_size: pageSize }
  if (query.value.trim()) body.query = query.value.trim()
  if (selectedStatuses.value.length) body.statuses = [...selectedStatuses.value]
  if (dateFrom.value) body.date_from = dateFrom.value
  if (dateTo.value) body.date_to = dateTo.value
  if (minTotal.value != null && minTotal.value !== '') body.min_total = minTotal.value
  if (maxTotal.value != null && maxTotal.value !== '') body.max_total = maxTotal.value
  return body
}

async function search() {
  loading.value = true
  error.value = ''
  try {
    const res = await api.searchOrders(buildBody())
    orders.value = res.orders
    total.value = res.total
    kpis.value = res.kpis
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function onQueryInput() {
  clearTimeout(debounce)
  debounce = setTimeout(() => { page.value = 1; search() }, 300)
}

function clearFilters() {
  query.value = ''
  selectedStatuses.value = []
  dateFrom.value = ''
  dateTo.value = ''
  minTotal.value = null
  maxTotal.value = null
  page.value = 1
  search()
}

watch([selectedStatuses, dateFrom, dateTo], () => { page.value = 1 })

onMounted(search)
</script>

<style scoped>
.admin { padding-top: 36px; }
.admin-head h1 { font-size: 34px; margin: 8px 0 6px; }
.admin-sub { margin: 0 0 22px; max-width: 620px; }

.search-hero {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 6px 6px 18px;
  margin-bottom: 20px;
  color: var(--ink-faint);
}
.search-hero:focus-within { border-color: var(--ink-soft); box-shadow: var(--shadow-sm); }
.omni {
  border: none; outline: none;
  font: 400 16px var(--font-body);
  flex: 1;
  padding: 12px 12px 12px 0;
  background: transparent;
  color: var(--ink);
}

.admin-grid {
  display: grid;
  grid-template-columns: 250px 1fr;
  gap: 20px;
  align-items: start;
}
@media (max-width: 900px) { .admin-grid { grid-template-columns: 1fr; } }

.filters { padding: 20px; position: sticky; top: 84px; }
.filters-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.filters-head h3 { margin: 0; font-size: 15px; }
.clear-btn {
  border: none; background: none; color: var(--accent-deep);
  font: 600 12.5px var(--font-body); cursor: pointer; padding: 4px;
}
.clear-btn:hover { text-decoration: underline; }
.filter-group { padding: 14px 0; border-top: 1px solid var(--line-soft); }
.filter-group:first-of-type { border-top: none; }
.filter-label {
  font-size: 11.5px; font-weight: 700; letter-spacing: 0.08em;
  text-transform: uppercase; color: var(--ink-faint); margin-bottom: 10px;
}
.check { display: flex; align-items: center; gap: 10px; padding: 5px 0; cursor: pointer; font-size: 14px; }
.check input { accent-color: var(--ink); width: 15px; height: 15px; }
.field-row { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.field-row .input { min-width: 0; }
/* native date inputs have a wide intrinsic size — stack them in the sidebar */
.field-col { display: flex; flex-direction: column; gap: 8px; }

.results { min-width: 0; }
.kpis {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}
@media (max-width: 1100px) { .kpis { grid-template-columns: repeat(2, 1fr); } }

.table-card { overflow: hidden; padding: 6px; }
.table-card .table th:first-child, .table-card .table td:first-child { padding-left: 18px; }
.state-box { padding: 44px; text-align: center; }
.state-box h3 { margin: 0 0 8px; font-size: 20px; }
.state-box p { margin: 0 0 16px; }

.order-id { font-weight: 600; color: var(--accent-deep); }
.cust-name { font-weight: 600; font-size: 13.5px; }
.cust-mail { font-size: 12px; }

.pager {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  border-top: 1px solid var(--line-soft);
}
.pager-info { font-size: 13px; }
.pager-btns { display: flex; gap: 8px; }
</style>
