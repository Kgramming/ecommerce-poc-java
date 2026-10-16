<template>
  <div class="card product fade-in">
    <div class="product-img">
      <img
        v-if="imgOk"
        :src="imgSrc"
        :alt="product.title"
        loading="lazy"
        @error="imgOk = false"
      />
      <div v-else class="img-fallback display" aria-hidden="true">
        {{ initials }}
      </div>
      <span class="img-cat">{{ product.category }}</span>
      <span v-if="!product.active" class="img-inactive">Inactive</span>
    </div>
    <div class="product-body">
      <h3 class="display product-title">{{ product.title }}</h3>
      <div class="product-foot">
        <span class="num product-price">{{ formatMoney(product.price) }}</span>
        <button
          v-if="product.active"
          class="btn btn-primary btn-sm"
          @click="$emit('add', product)"
        >Add to bag</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { formatMoney } from '../api/client.js'

const props = defineProps({ product: { type: Object, required: true } })
defineEmits(['add'])

const imgOk = ref(true)
const imgSrc = computed(() =>
  `/images/products/${String(props.product.sku || '').toLowerCase()}.jpg`)
const initials = computed(() =>
  props.product.title.split(/\s+/).slice(0, 2).map(w => w[0]).join('').toUpperCase())
</script>

<style scoped>
.product {
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 0;
  overflow: hidden;
  transition: transform 0.18s var(--ease), box-shadow 0.18s var(--ease);
}
.product:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
}
.product-img {
  position: relative;
  aspect-ratio: 4 / 3;
  border-radius: calc(var(--radius) - 10px);
  overflow: hidden;
  background: var(--img-well);
}
.product-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s var(--ease);
}
.product:hover .product-img img { transform: scale(1.04); }
.img-fallback {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  font-size: 44px; color: var(--ink-faint);
}
.img-cat, .img-inactive {
  position: absolute;
  top: 10px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  padding: 6px 12px;
  border-radius: 999px;
  backdrop-filter: blur(6px);
}
.img-cat {
  left: 10px;
  background: rgba(255, 255, 255, 0.85);
  color: #1c1a16;
}
.img-inactive {
  right: 10px;
  background: rgba(28, 26, 22, 0.75);
  color: #fff;
}
.product-body { padding: 14px 8px 8px; display: flex; flex-direction: column; gap: 10px; }
.product-title { margin: 0; font-size: 18px; }
.product-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.product-price { font-size: 17px; font-weight: 600; }
</style>
