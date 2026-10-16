<template>
  <Teleport to="body">
    <div v-if="open" class="modal-backdrop fade-in" @click.self="$emit('close')">
      <div class="modal card fade-in" role="dialog" aria-modal="true">
        <div class="modal-head">
          <h3 class="display">{{ title }}</h3>
          <button class="modal-x" @click="$emit('close')" aria-label="Close">✕</button>
        </div>
        <div class="modal-body">
          <slot></slot>
        </div>
        <div v-if="$slots.footer" class="modal-foot">
          <slot name="footer"></slot>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
defineProps({ open: Boolean, title: { type: String, default: '' } })
defineEmits(['close'])
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(28, 27, 23, 0.45);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 8vh 16px 16px;
  z-index: 150;
  overflow-y: auto;
}
.modal { width: 560px; max-width: 100%; }
.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 0;
}
.modal-head h3 { margin: 0; font-size: 22px; }
.modal-x {
  border: none;
  background: none;
  font-size: 15px;
  color: var(--ink-faint);
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
}
.modal-x:hover { color: var(--ink); background: var(--paper-2); }
.modal-body { padding: 18px 24px 24px; }
.modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 24px 22px;
}
</style>
