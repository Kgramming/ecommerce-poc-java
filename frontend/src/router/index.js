import { createRouter, createWebHistory } from 'vue-router'

import Storefront from '../views/Storefront.vue'
import Checkout from '../views/Checkout.vue'
import AdminSearch from '../views/AdminSearch.vue'
import OrderDetail from '../views/OrderDetail.vue'
import CatalogAdmin from '../views/CatalogAdmin.vue'

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'store', component: Storefront },
    { path: '/checkout', name: 'checkout', component: Checkout },
    { path: '/admin', name: 'admin', component: AdminSearch },
    { path: '/admin/orders/:id', name: 'order-detail', component: OrderDetail, props: true },
    { path: '/admin/catalog', name: 'catalog', component: CatalogAdmin },
  ],
  scrollBehavior() { return { top: 0 } },
})
