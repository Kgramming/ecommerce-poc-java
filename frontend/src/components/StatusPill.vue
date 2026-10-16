<template>
  <span class="pill" :class="'pill-' + status.toLowerCase()">{{ status }}</span>
</template>

<script setup>
defineProps({ status: { type: String, required: true } })
</script>
