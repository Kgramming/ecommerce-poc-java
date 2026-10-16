<template>
  <div class="card kpi fade-in">
    <div class="eyebrow">{{ label }}</div>
    <div class="kpi-value num">{{ value }}</div>
    <div v-if="sub" class="kpi-sub muted">{{ sub }}</div>
  </div>
</template>

<script setup>
defineProps({
  label: String,
  value: [String, Number],
  sub: String,
})
</script>

<style scoped>
.kpi { padding: 18px 20px; }
.kpi-value {
  font-size: 30px;
  font-weight: 500;
  margin-top: 6px;
  letter-spacing: -0.02em;
}
.kpi-sub { font-size: 12.5px; margin-top: 4px; }
</style>
