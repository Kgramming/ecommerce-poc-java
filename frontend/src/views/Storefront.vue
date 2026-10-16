<template>
  <div class="wrap">
    <!-- Hero -->
    <section class="hero">
      <div class="eyebrow">Workspace essentials</div>
      <h1 class="display">Tools for focused work.</h1>
      <p class="muted hero-sub">
        Peripherals, audio and office gear — picked for people who spend
        their days at a desk. Everything ships from one warehouse.
      </p>
    </section>

    <!-- Toolbar: search + categories -->
    <section class="toolbar">
      <div class="search-box">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="7"/><path d="M20 20l-3.5-3.5"/>
        </svg>
        <input
          v-model="q"
          class="input search-input"
          type="search"
          placeholder="Search products, tags…"
          @input="onSearchInput"
        />
      </div>
      <div class="cats">
        <button
          v-for="c in categories"
          :key="c"
          class="cat-pill"
          :class="{ active: category === c }"
          @click="category = c"
        >{{ c === '' ? 'All' : c }}</button>
      </div>
    </section>

    <div class="result-meta faint">
      <span v-if="!loading && !error">{{ products.length }} product{{ products.length === 1 ? '' : 's' }}</span>
    </div>

    <!-- States -->
    <Skeleton v-if="loading" :rows="6" />
    <div v-else-if="error" class="card error-box">
      <h3 class="display">Couldn't load products</h3>
      <p class="muted">{{ error }}</p>
      <button class="btn btn-ghost" @click="fetchProducts">Try again</button>
    </div>
    <EmptyState
      v-else-if="products.length === 0"
      mark="◌"
      title="No products match"
      body="Try a different search term or category."
    >
      <button class="btn btn-ghost" @click="clearFilters">Clear filters</button>
    </EmptyState>

    <!-- Grid -->
    <section v-else class="grid">
      <ProductCard
        v-for="p in products"
        :key="p.id"
        :product="p"
        @add="addToBag"
      />
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api/client.js'
import { useCart } from '../stores/cart.js'
import { toast } from '../stores/toast.js'
import ProductCard from '../components/ProductCard.vue'
import EmptyState from '../components/EmptyState.vue'
import Skeleton from '../components/Skeleton.vue'

const categories = ['', 'Peripherals', 'Audio', 'Cables', 'Office']
const q = ref('')
const category = ref('')
const products = ref([])
const loading = ref(true)
const error = ref('')
const { add } = useCart()

let debounce = null

async function fetchProducts() {
  loading.value = true
  error.value = ''
  try {
    const params = { limit: 100 }
    if (q.value.trim()) params.q = q.value.trim()
    if (category.value) params.category = category.value
    const res = await api.products(params)
    products.value = res.products
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function onSearchInput() {
  clearTimeout(debounce)
  debounce = setTimeout(fetchProducts, 280)
}

function clearFilters() {
  q.value = ''
  category.value = ''
  fetchProducts()
}

function addToBag(product) {
  add(product)
  toast.success(`Added “${product.title}” to your bag`)
}

onMounted(fetchProducts)

// re-fetch when the category pill changes
import { watch } from 'vue'
watch(category, fetchProducts)
</script>

<style scoped>
.hero { padding: 44px 0 24px; max-width: 660px; }
.hero h1 { font-size: 46px; margin: 10px 0 12px; letter-spacing: -0.02em; }
.hero-sub { font-size: 16px; margin: 0; }

.toolbar {
  display: flex;
  gap: 14px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 10px;
}
.search-box {
  position: relative;
  flex: 1;
  min-width: 220px;
  max-width: 360px;
}
.search-box svg {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--ink-faint);
}
.search-input { padding-left: 36px; }
.cats { display: flex; gap: 8px; flex-wrap: wrap; }
.cat-pill {
  border: 1px solid var(--line);
  background: var(--card);
  border-radius: 999px;
  padding: 8px 15px;
  font: 500 13px var(--font-body);
  color: var(--ink-soft);
  cursor: pointer;
  transition: all 0.12s;
}
.cat-pill:hover { border-color: var(--ink-soft); color: var(--ink); }
.cat-pill.active { background: var(--ink); border-color: var(--ink); color: var(--paper); }

.result-meta { font-size: 13px; margin: 14px 0; }

.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
}
@media (max-width: 960px) { .grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 620px) { .grid { grid-template-columns: 1fr; } .hero h1 { font-size: 34px; } }

.error-box { padding: 40px; text-align: center; }
.error-box h3 { margin: 0 0 8px; font-size: 22px; }
.error-box p { margin: 0 0 18px; }
</style>
