<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/introduction')
}
</script>

<template>
  <div class="not-found-container">
    <div class="not-found-content">
      <h1 class="error-code">404</h1>
      <h2 class="error-title">Page Not Found</h2>
      <p class="error-description">
        Sorry, the page you are looking for does not exist or has been removed.
      </p>
      <button class="back-button" @click="goHome">Back to Home</button>
    </div>
  </div>
</template>

<style scoped>
.not-found-container {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(10, 10, 18, 0.95) 0%, rgba(10, 10, 18, 0.8) 100%);
}

.not-found-content {
  text-align: center;
  animation: fadeInUp 0.6s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.error-code {
  font-family: 'Comfortaa', sans-serif;
  font-size: 120px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--primary) 0%, var(--secondary) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0;
  line-height: 1;
}

.error-title {
  font-family: 'Comfortaa', sans-serif;
  font-size: 32px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 20px 0 10px;
}

.error-description {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 40px;
  max-width: 400px;
  line-height: 1.6;
}

.back-button {
  padding: 12px 32px;
  background: linear-gradient(135deg, var(--primary), var(--secondary));
  border: none;
  border-radius: 50px;
  color: var(--dark-1);
  font-family: 'Comfortaa', sans-serif;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 8px 32px rgba(0, 240, 255, 0.3);
}

.back-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 48px rgba(0, 240, 255, 0.5);
}

.back-button:active {
  transform: translateY(0);
}

@media (max-width: 768px) {
  .error-code {
    font-size: 80px;
  }

  .error-title {
    font-size: 24px;
  }

  .error-description {
    font-size: 13px;
  }
}
</style>
