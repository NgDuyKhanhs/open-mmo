<script setup lang="ts">
  import { ref, onMounted } from 'vue'
  import { useRouter } from 'vue-router'
  import Navbar from './components/Navbar.vue'
  import LoadingScreen from './components/LoadingScreen.vue'
  import AmbientBg from './components/AmbientBg.vue'
  import Footer from './components/Footer.vue'

  const router = useRouter()
  const showLoading = ref(true)

  onMounted(() => {
    setTimeout(() => {
      showLoading.value = false
    }, 1500)
  })

  const backToHome = () => {
    router.push('/')
  }
</script>

<template>
  <div class="app-container">
    <!-- Ambient Background -->
    <AmbientBg />

    <!-- Loading Screen -->
    <LoadingScreen :visible="showLoading" />

    <!-- Main Container -->
    <div class="layout-wrapper">
      <!-- Navbar (Always Visible) -->
      <Navbar />

      <!-- Content -->
      <div class="container">
        <router-view :key="$route.path" :on-back="backToHome" />
        <Footer />
      </div>
    </div>
  </div>
</template>

<style scoped>
  .app-container {
    position: relative;
    overflow-x: hidden;
    min-height: 100vh;
    font-family: 'Comfortaa', sans-serif;
  }

  .layout-wrapper {
    position: relative;
    z-index: 10;
    display: flex;
    flex-direction: column;
    min-height: 100vh;
  }

  .container {
    flex: 1;
    padding: 120px 20px 40px;
    max-width: 1200px;
    margin: 0 auto;
    width: 100%;
    display: flex;
    flex-direction: column;
  }

  @media (max-width: 768px) {
    .container {
      padding: 120px 15px 20px;
    }
  }
</style>
