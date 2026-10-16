<template>
  <header class="site-head">
    <div class="wrap head-inner">
      <RouterLink to="/" class="brand">
        <span class="brand-mark" aria-hidden="true">
          <svg viewBox="0 0 32 32" width="20" height="20">
            <g fill="none" stroke="var(--paper)" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round">
              <path d="M16 7.5 L23.5 11.75 V20.25 L16 24.5 L8.5 20.25 V11.75 Z"/>
              <path d="M8.5 11.75 L16 16 L23.5 11.75"/>
              <path d="M16 16 V24.5"/>
            </g>
          </svg>
        </span>
        <span class="brand-name display">Y3L <em>Supply Co.</em></span>
      </RouterLink>
      <nav class="nav">
        <RouterLink to="/" class="nav-link" active-class="active">Shop</RouterLink>
        <RouterLink to="/admin" class="nav-link" active-class="active">Orders</RouterLink>
        <RouterLink to="/admin/catalog" class="nav-link" active-class="active">Catalog</RouterLink>
      </nav>
      <div class="head-right">
        <span class="acting-as faint" title="Demo user for placing orders">
          {{ userLabel }}
        </span>
        <button class="icon-btn" @click="toggleTheme" :aria-label="theme === 'light' ? 'Switch to dark mode' : 'Switch to light mode'" :title="theme === 'light' ? 'Dark mode' : 'Light mode'">
          <svg v-if="theme === 'light'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/>
          </svg>
          <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <circle cx="12" cy="12" r="4.2"/><path d="M12 2.5v2.4M12 19.1v2.4M2.5 12h2.4M19.1 12h2.4M5 5l1.7 1.7M17.3 17.3L19 19M19 5l-1.7 1.7M6.7 17.3L5 19"/>
          </svg>
        </button>
        <button class="icon-btn bag-btn" @click="$emit('open-cart')" aria-label="Open bag">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <path d="M6 8h15l-1.5 9h-12z"/><path d="M6 8l-1-4H2"/><circle cx="9" cy="20" r="1.6"/><circle cx="18" cy="20" r="1.6"/>
          </svg>
          <span v-if="count > 0" class="bag-count num">{{ count }}</span>
        </button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useCart } from '../stores/cart.js'
import { useTheme } from '../composables/useTheme.js'

defineEmits(['open-cart'])
const { count } = useCart()
const { theme, toggle: toggleTheme } = useTheme()
/* Fixed demo user for order placement (matches seed user #2). */
const userLabel = computed(() => 'Signed in as Priya Patel')
</script>

<style scoped>
.site-head {
  position: sticky;
  top: 0;
  z-index: 100;
  background: color-mix(in srgb, var(--paper) 90%, transparent);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--line);
}
.head-inner {
  display: flex;
  align-items: center;
  gap: 32px;
  height: 64px;
}
.brand { display: flex; align-items: center; gap: 10px; text-decoration: none; }
.brand-mark {
  width: 32px; height: 32px;
  background: var(--ink);
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
}
.brand-name { font-size: 18px; }
.brand-name em { font-style: normal; color: var(--ink-faint); font-weight: 500; }
.nav { display: flex; gap: 4px; }
.nav-link {
  text-decoration: none;
  font-size: 14px; font-weight: 500;
  color: var(--ink-soft);
  padding: 8px 14px;
  border-radius: 999px;
}
.nav-link:hover { background: var(--paper-2); color: var(--ink); }
.nav-link.active { background: var(--ink); color: var(--paper); }
.head-right { margin-left: auto; display: flex; align-items: center; gap: 10px; }
.acting-as { font-size: 12.5px; margin-right: 6px; }
.icon-btn {
  position: relative;
  border: 1px solid var(--line);
  background: var(--card);
  border-radius: 999px;
  width: 40px; height: 40px;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; color: var(--ink);
  transition: border-color 0.12s, transform 0.12s var(--ease);
}
.icon-btn:hover { border-color: var(--ink-soft); }
.icon-btn:active { transform: scale(0.94); }
.bag-count {
  position: absolute;
  top: -6px; right: -6px;
  background: var(--accent); color: #fff;
  font-size: 11px; font-weight: 600;
  min-width: 20px; height: 20px;
  border-radius: 999px;
  display: flex; align-items: center; justify-content: center;
  padding: 0 5px;
}
@media (max-width: 760px) {
  .acting-as { display: none; }
  .head-inner { gap: 16px; }
}
</style>
