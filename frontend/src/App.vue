<template>
  <AppHeader @open-cart="cartOpen = true" />
  <main>
    <RouterView />
  </main>
  <CartDrawer
    :open="cartOpen"
    @close="cartOpen = false"
    @checkout="goCheckout"
  />
  <ToastHost />
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from './components/AppHeader.vue'
import CartDrawer from './components/CartDrawer.vue'
import ToastHost from './components/ToastHost.vue'
import { useTheme } from './composables/useTheme.js'

useTheme().init()

const router = useRouter()
const cartOpen = ref(false)

function goCheckout() {
  cartOpen.value = false
  router.push('/checkout')
}
</script>

<style>
main { min-height: calc(100vh - 64px); padding-bottom: 72px; }
</style>
