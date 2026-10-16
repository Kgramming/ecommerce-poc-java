<template>
  <div class="skeleton" aria-hidden="true">
    <div v-for="i in rows" :key="i" class="sk-row"></div>
  </div>
</template>

<script setup>
defineProps({ rows: { type: Number, default: 5 } })
</script>

<style scoped>
.sk-row {
  height: 56px;
  border-radius: var(--radius-sm);
  background: linear-gradient(90deg, var(--paper-2) 25%, #e9e6da 50%, var(--paper-2) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
  margin-bottom: 10px;
}
@keyframes shimmer {
  to { background-position: -200% 0; }
}
</style>
