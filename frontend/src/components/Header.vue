<script setup lang="ts"></script>

<template>
  <div class="header">
    <div class="logo">
      <svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <linearGradient id="logoGradient" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" style="stop-color: #00f0ff" />
            <stop offset="100%" style="stop-color: #ff00d4" />
          </linearGradient>
        </defs>
        <polygon
          points="50,5 95,30 95,70 50,95 5,70 5,30"
          fill="none"
          stroke="url(#logoGradient)"
          stroke-width="2"
        />
        <polygon
          points="50,20 80,37 80,63 50,80 20,63 20,37"
          fill="none"
          stroke="url(#logoGradient)"
          stroke-width="1.5"
          opacity="0.6"
        />
        <circle cx="50" cy="50" r="12" fill="url(#logoGradient)" opacity="0.3" />
        <circle cx="50" cy="50" r="6" fill="url(#logoGradient)" />
      </svg>
    </div>
    <h1 class="brand-name">NexaVerse</h1>
    <p class="tagline">Digital Excellence Redefined</p>
  </div>
</template>

<style scoped>
  .header {
    text-align: center;
    margin-bottom: 60px;
    opacity: 0;
    animation: fadeUp 0.8s ease-out 1s forwards;
  }

  .logo {
    width: 90px;
    height: 90px;
    margin: 0 auto 25px;
    position: relative;
  }

  .logo svg {
    width: 100%;
    height: 100%;
    filter: drop-shadow(0 0 20px var(--glow-cyan));
  }

  .brand-name {
    font-family: 'Comfortaa', sans-serif;
    font-size: 52px;
    font-weight: 600;
    letter-spacing: 6px;
    margin-bottom: 12px;
    background: linear-gradient(135deg, var(--primary) 0%, #ffffff 50%, var(--secondary) 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    text-transform: uppercase;
  }

  .tagline {
    font-size: 13px;
    letter-spacing: 5px;
    color: rgba(255, 255, 255, 0.5);
    text-transform: uppercase;
    font-weight: 300;
  }

  @keyframes fadeUp {
    from {
      opacity: 0;
      transform: translateY(30px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  @media (max-width: 768px) {
    .brand-name {
      font-size: 36px;
      letter-spacing: 4px;
    }

    .tagline {
      font-size: 10px;
      letter-spacing: 3px;
    }
  }

  @media (max-width: 480px) {
    .brand-name {
      font-size: 28px;
      letter-spacing: 3px;
    }

    .tagline {
      font-size: 10px;
      letter-spacing: 3px;
    }
  }
</style>
