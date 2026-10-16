<template>
  <div class="toast-host" aria-live="polite">
    <div
      v-for="t in toast.state.items"
      :key="t.id"
      class="toast fade-in"
      :class="'toast-' + t.kind"
      @click="toast.dismiss(t.id)"
    >
      <span class="toast-dot"></span>
      <span>{{ t.message }}</span>
    </div>
  </div>
</template>

<script setup>
import { toast } from '../stores/toast.js'
</script>

<style scoped>
.toast-host {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  gap: 10px;
  z-index: 200;
  align-items: center;
}
.toast {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--ink);
  color: var(--paper);
  font-size: 13.5px;
  font-weight: 500;
  padding: 12px 18px;
  border-radius: 999px;
  box-shadow: var(--shadow-lg);
  cursor: pointer;
  max-width: min(520px, 90vw);
}
.toast-dot { width: 8px; height: 8px; border-radius: 50%; background: #7fb98a; flex: none; }
.toast-error .toast-dot { background: #e88; }
</style>
