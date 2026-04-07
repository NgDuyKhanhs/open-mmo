import './styles/global.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Vue3Toastify from 'vue3-toastify'
import 'vue3-toastify/dist/index.css'

import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/useAuthStore'

const app = createApp(App)

const pinia = createPinia()
app.use(pinia)
app.use(router)
app.use(Vue3Toastify, {
  autoClose: 3000,
  position: 'top-right',
  closeButton: true,
})

// Initialize auth store to load tokens from localStorage
const authStore = useAuthStore()
authStore.initializeAuth()

app.mount('#app')
