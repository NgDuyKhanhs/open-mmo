<script setup lang="ts">
  import { ref } from 'vue'

  const emit = defineEmits<{
    navigate: [section: string]
  }>()

  const menuItems = ref([
    { id: 1, badge: 'IN', title: 'Introduction', section: 'introduction' },
    { id: 2, badge: 'SV', title: 'Services', section: 'services' },
    { id: 3, badge: 'GL', title: 'Gallery', section: 'gallery' },
    { id: 4, badge: 'TM', title: 'Testimonials', section: 'testimonials' },
    { id: 5, badge: 'AB', title: 'About', section: 'about' },
    { id: 6, badge: 'CT', title: 'Contact', section: 'contact' },
  ])

  const handleMenuClick = (section: string) => {
    emit('navigate', section)
  }
</script>

<template>
  <div class="menu-grid">
    <div
      v-for="item in menuItems"
      :key="item.id"
      class="menu-item initial-load"
      @click="handleMenuClick(item.section)"
    >
      <div class="menu-badge">{{ item.badge }}</div>
      <div class="menu-title">{{ item.title }}</div>
    </div>
  </div>
</template>

<style scoped>
  .menu-grid {
    display: grid;
    grid-template-columns: repeat(3, 204px);
    gap: 25px;
    margin: 0 auto 40px;
    justify-content: center;
  }

  .menu-item {
    height: 170px;
    background: var(--glass-bg);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border: 1px solid var(--glass-border);
    border-radius: 24px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    opacity: 0;
    transform: translateY(30px) scale(0.9);
    transition: all 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  }

  .menu-item::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 50%;
    background: linear-gradient(
      180deg,
      rgba(255, 255, 255, 0.12) 0%,
      rgba(255, 255, 255, 0.05) 40%,
      transparent 100%
    );
    border-radius: 24px 24px 0 0;
    pointer-events: none;
  }

  .menu-item:hover {
    transform: translateY(-10px) scale(1.02);
    background: rgba(0, 240, 255, 0.08);
    border-color: var(--primary);
    box-shadow:
      0 20px 40px rgba(0, 0, 0, 0.4),
      0 0 30px rgba(0, 200, 220, 0.2);
  }

  .menu-item.initial-load {
    animation: menuAppear 0.5s ease-out forwards;
  }

  .menu-item.initial-load:nth-child(1) {
    animation-delay: 1.1s;
  }

  .menu-item.initial-load:nth-child(2) {
    animation-delay: 1.2s;
  }

  .menu-item.initial-load:nth-child(3) {
    animation-delay: 1.3s;
  }

  .menu-item.initial-load:nth-child(4) {
    animation-delay: 1.4s;
  }

  .menu-item.initial-load:nth-child(5) {
    animation-delay: 1.5s;
  }

  .menu-item.initial-load:nth-child(6) {
    animation-delay: 1.6s;
  }

  @keyframes menuAppear {
    to {
      opacity: 1;
      transform: translateY(0) scale(1);
    }
  }

  .menu-badge {
    width: 60px;
    height: 60px;
    border-radius: 50%;
    display: flex;
    justify-content: center;
    align-items: center;
    font-family: 'Comfortaa', sans-serif;
    font-size: 22px;
    font-weight: 700;
    margin-bottom: 15px;
    position: relative;
    background: linear-gradient(135deg, rgba(0, 240, 255, 0.15) 0%, rgba(255, 0, 212, 0.15) 100%);
    border: 1px solid rgba(255, 255, 255, 0.1);
    color: var(--primary);
    text-shadow: 0 0 20px var(--glow-cyan);
    transition: all 0.4s ease;
    overflow: hidden;
  }

  .menu-badge::before {
    content: '';
    position: absolute;
    top: 2px;
    left: 10%;
    right: 10%;
    height: 45%;
    background: linear-gradient(180deg, rgba(255, 255, 255, 0.3) 0%, transparent 100%);
    border-radius: 50%;
    pointer-events: none;
  }

  .menu-badge::after {
    content: '';
    position: absolute;
    inset: -3px;
    border-radius: 50%;
    border: 2px solid transparent;
    border-top-color: var(--primary);
    border-right-color: rgba(0, 200, 220, 0.5);
    opacity: 0;
    transition: opacity 0.4s ease;
  }

  .menu-item:hover .menu-badge {
    background: linear-gradient(135deg, rgba(0, 240, 255, 0.2) 0%, rgba(0, 200, 220, 0.15) 100%);
    transform: scale(1.1);
    box-shadow: 0 0 25px rgba(0, 200, 220, 0.25);
  }

  .menu-item:hover .menu-badge::after {
    opacity: 1;
    animation: badgeSpin 2s linear infinite;
  }

  @keyframes badgeSpin {
    to {
      transform: rotate(360deg);
    }
  }

  .menu-title {
    font-size: 12px;
    font-weight: 400;
    letter-spacing: 3px;
    text-transform: uppercase;
    color: rgba(255, 255, 255, 0.7);
    transition: all 0.3s ease;
  }

  .menu-item:hover .menu-title {
    color: #ffffff;
    letter-spacing: 4px;
  }

  @media (max-width: 992px) {
    .menu-grid {
      grid-template-columns: repeat(3, 170px);
    }

    .menu-item {
      height: 145px;
    }
  }

  @media (max-width: 768px) {
    .menu-grid {
      grid-template-columns: repeat(2, 155px);
      gap: 15px;
    }

    .menu-item {
      height: 135px;
    }

    .menu-badge {
      width: 50px;
      height: 50px;
      font-size: 18px;
    }

    .menu-title {
      font-size: 10px;
      letter-spacing: 2px;
    }
  }

  @media (max-width: 480px) {
    .menu-grid {
      grid-template-columns: repeat(2, 130px);
      gap: 12px;
    }

    .menu-item {
      height: 120px;
    }

    .menu-badge {
      width: 45px;
      height: 45px;
      font-size: 16px;
    }

    .menu-title {
      font-size: 10px;
      letter-spacing: 2px;
    }
  }
</style>
