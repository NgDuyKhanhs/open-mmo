<script setup lang="ts">
interface Props {
  visible: boolean
}

withDefaults(defineProps<Props>(), {
  visible: true,
})
</script>

<template>
  <div v-if="visible" class="loading-screen">
    <div class="loader-ring"></div>
    <div class="loading-text">Loading</div>
  </div>
</template>

<style scoped>
.loading-screen {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: var(--dark-1);
  z-index: 10000;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  animation: fadeOut 0.8s ease 1.5s forwards;
}

@keyframes fadeOut {
  to {
    opacity: 0;
    visibility: hidden;
  }
}

.loader-ring {
  width: 80px;
  height: 80px;
  position: relative;
}

.loader-ring::before,
.loader-ring::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  border: 2px solid transparent;
}

.loader-ring::before {
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-top-color: var(--primary);
  border-right-color: var(--primary);
  animation: spin 1.2s linear infinite;
}

.loader-ring::after {
  top: 10px;
  left: 10px;
  right: 10px;
  bottom: 10px;
  border-bottom-color: var(--secondary);
  border-left-color: var(--secondary);
  animation: spin 0.8s linear infinite reverse;
}

.loading-text {
  margin-top: 30px;
  font-family: 'Comfortaa', sans-serif;
  font-size: 12px;
  letter-spacing: 4px;
  color: var(--primary);
  text-transform: uppercase;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
