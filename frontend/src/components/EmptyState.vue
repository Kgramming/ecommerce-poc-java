<template>
  <div class="empty">
    <div class="empty-mark">{{ mark }}</div>
    <h3 class="display">{{ title }}</h3>
    <p class="muted">{{ body }}</p>
    <slot></slot>
  </div>
</template>

<script setup>
defineProps({
  mark: { type: String, default: '◦' },
  title: { type: String, default: 'Nothing here yet' },
  body: { type: String, default: '' },
})
</script>

<style scoped>
.empty {
  text-align: center;
  padding: 56px 24px;
  border: 1px dashed var(--line);
  border-radius: var(--radius);
  background: var(--card);
}
.empty-mark {
  font-size: 30px;
  color: var(--ink-faint);
  margin-bottom: 8px;
}
.empty h3 { margin: 0 0 6px; font-size: 20px; }
.empty p { margin: 0 0 18px; font-size: 14px; }
</style>
