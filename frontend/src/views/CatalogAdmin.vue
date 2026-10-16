<template>
  <div class="wrap admin">
    <div class="admin-head">
      <div>
        <div class="eyebrow">Back office</div>
        <h1 class="display">Catalog</h1>
        <p class="muted admin-sub">
          Products live in MongoDB. Editing a title or price here never rewrites
          old orders — they keep their own snapshots.
        </p>
      </div>
      <button class="btn btn-accent" @click="openCreate">+ New product</button>
    </div>

    <div class="card table-card">
      <Skeleton v-if="loading" :rows="8" />
      <div v-else-if="error" class="state-box">
        <h3 class="display">Couldn't load the catalog</h3>
        <p class="muted">{{ error }}</p>
        <button class="btn btn-ghost" @click="fetchProducts">Retry</button>
      </div>
      <EmptyState
        v-else-if="products.length === 0"
        mark="◌"
        title="No products"
        body="The catalog is empty. Create the first product."
      >
        <button class="btn btn-accent" @click="openCreate">+ New product</button>
      </EmptyState>
      <table v-else class="table">
        <thead>
          <tr>
            <th>Product</th>
            <th>Category</th>
            <th style="text-align:right">Price</th>
            <th>Tags</th>
            <th>Variants</th>
            <th>Status</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in products" :key="p.id">
            <td>
              <div class="p-title">{{ p.title }}</div>
              <div class="faint p-id num">{{ p.id }}</div>
            </td>
            <td><span class="pill pill-neutral">{{ p.category }}</span></td>
            <td class="num" style="text-align:right">{{ formatMoney(p.price) }}</td>
            <td>
              <span v-for="t in (p.tags || []).slice(0, 3)" :key="t" class="tag">{{ t }}</span>
            </td>
            <td class="num muted">{{ (p.variants || []).length }}</td>
            <td>
              <span v-if="p.active" class="pill pill-shipped">Active</span>
              <span v-else class="pill pill-neutral">Inactive</span>
            </td>
            <td style="text-align:right">
              <button class="btn btn-ghost btn-sm" @click="openEdit(p)">Edit</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Create / edit modal -->
    <AppModal :open="modalOpen" :title="editing ? 'Edit product' : 'New product'" @close="closeModal">
      <div class="form-grid">
        <div class="field">
          <label>Title</label>
          <input v-model="form.title" class="input" placeholder="e.g. Wireless Mouse Pro" />
        </div>
        <div class="form-row">
          <div class="field">
            <label>Price ($)</label>
            <input v-model.number="form.price" type="number" min="0" step="0.01" class="input" />
          </div>
          <div class="field">
            <label>Category</label>
            <select v-model="form.category" class="select">
              <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
            </select>
          </div>
        </div>
        <div class="field">
          <label>Tags <span class="faint">(comma separated)</span></label>
          <input v-model="form.tagsText" class="input" placeholder="wireless, mouse, ergonomic" />
        </div>
        <div class="field">
          <label>Attributes <span class="faint">(JSON object)</span></label>
          <textarea v-model="form.attributesText" class="input" rows="3" spellcheck="false"></textarea>
        </div>
        <div class="field">
          <label>Variants <span class="faint">(JSON array)</span></label>
          <textarea v-model="form.variantsText" class="input" rows="3" spellcheck="false"></textarea>
        </div>
        <label class="check">
          <input type="checkbox" v-model="form.active" />
          <span>Active — visible in the storefront</span>
        </label>
        <p v-if="formError" class="form-error">{{ formError }}</p>
      </div>
      <template #footer>
        <button class="btn btn-ghost" @click="closeModal">Cancel</button>
        <button class="btn btn-accent" :disabled="saving" @click="save">
          {{ saving ? 'Saving…' : editing ? 'Save changes' : 'Create product' }}
        </button>
      </template>
    </AppModal>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api, formatMoney } from '../api/client.js'
import { toast } from '../stores/toast.js'
import AppModal from '../components/AppModal.vue'
import EmptyState from '../components/EmptyState.vue'
import Skeleton from '../components/Skeleton.vue'

const categories = ['Peripherals', 'Audio', 'Cables', 'Office']

const products = ref([])
const loading = ref(true)
const error = ref('')

const modalOpen = ref(false)
const editing = ref(null)   // product being edited, or null for create
const saving = ref(false)
const formError = ref('')
const form = ref(blankForm())

function blankForm() {
  return {
    title: '', price: 0, category: 'Peripherals',
    tagsText: '', attributesText: '{}', variantsText: '[]',
    active: true,
  }
}

async function fetchProducts() {
  loading.value = true
  error.value = ''
  try {
    const res = await api.products({ limit: 100, include_inactive: true })
    products.value = res.products
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.value = blankForm()
  formError.value = ''
  modalOpen.value = true
}

function openEdit(p) {
  editing.value = p
  // DB stores lowercase ("peripherals"); the dropdown shows labels ("Peripherals")
  const catLabel = categories.find(c => c.toLowerCase() === (p.category || '').toLowerCase())
  form.value = {
    title: p.title,
    price: p.price,
    category: catLabel || 'Peripherals',
    tagsText: (p.tags || []).join(', '),
    attributesText: JSON.stringify(p.attributes || {}, null, 2),
    variantsText: JSON.stringify(p.variants || [], null, 2),
    active: p.active,
  }
  formError.value = ''
  modalOpen.value = true
}

function closeModal() {
  modalOpen.value = false
  editing.value = null
}

function parseJson(text, label) {
  try {
    return JSON.parse(text || (label === 'attributes' ? '{}' : '[]'))
  } catch {
    throw new Error(`${label} is not valid JSON`)
  }
}

async function save() {
  formError.value = ''
  if (!form.value.title.trim()) {
    formError.value = 'Title is required.'
    return
  }
  if (!(form.value.price > 0)) {
    formError.value = 'Price must be greater than zero.'
    return
  }
  let attributes, variants
  try {
    attributes = parseJson(form.value.attributesText, 'attributes')
    variants = parseJson(form.value.variantsText, 'variants')
  } catch (e) {
    formError.value = e.message
    return
  }
  if (attributes && typeof attributes !== 'object') {
    formError.value = 'Attributes must be a JSON object.'
    return
  }
  if (!Array.isArray(variants)) {
    formError.value = 'Variants must be a JSON array.'
    return
  }

  const payload = {
    title: form.value.title.trim(),
    price: Number(form.value.price),
    category: form.value.category.toLowerCase(), // DB convention is lowercase
    tags: form.value.tagsText.split(',').map(t => t.trim()).filter(Boolean),
    attributes,
    variants,
    active: form.value.active,
  }

  saving.value = true
  try {
    if (editing.value) {
      await api.updateProduct(editing.value.id, payload)
      toast.success(`“${payload.title}” updated`)
    } else {
      await api.createProduct(payload)
      toast.success(`“${payload.title}” created`)
    }
    closeModal()
    await fetchProducts()
  } catch (e) {
    formError.value = e.message
  } finally {
    saving.value = false
  }
}

onMounted(fetchProducts)
</script>

<style scoped>
.admin { padding-top: 36px; }
.admin-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}
.admin-head h1 { font-size: 34px; margin: 8px 0 6px; }
.admin-sub { margin: 0; max-width: 560px; }

.table-card { overflow: hidden; padding: 6px; }
.table-card .table th:first-child, .table-card .table td:first-child { padding-left: 18px; }
.p-title { font-weight: 600; font-size: 14px; }
.p-id { font-size: 11px; }
.tag {
  display: inline-block;
  font-size: 11.5px;
  color: var(--ink-soft);
  background: var(--paper-2);
  border-radius: 999px;
  padding: 3px 9px;
  margin: 1px 4px 1px 0;
}
.state-box { padding: 44px; text-align: center; }
.state-box h3 { margin: 0 0 8px; font-size: 20px; }
.state-box p { margin: 0 0 16px; }

.form-grid { display: flex; flex-direction: column; gap: 14px; }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.check { display: flex; align-items: center; gap: 10px; cursor: pointer; font-size: 14px; }
.check input { accent-color: var(--ink); width: 16px; height: 16px; }
.form-error { color: var(--danger); font-size: 13.5px; margin: 0; }
</style>
