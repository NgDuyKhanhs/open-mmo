<script setup lang="ts">
import { onMounted, ref } from 'vue'
import ChatAssistant from '../components/ChatAssistant.vue'
import { usePhoneTilt, useScrollReveal, useCounterAnimation } from '@/composables'

const phoneContainer = ref<HTMLElement | null>(null)

// Use composables for animations
const { rotateX, rotateY } = usePhoneTilt(phoneContainer, { maxRotate: 15, enabled: true })
const { observeElement } = useScrollReveal({ threshold: 0.15, rootMargin: '0px 0px -50px 0px' })

// Counter animation targets
const animatedValues = useCounterAnimation([500, 98, 12], {
  duration: 1200,
  stepDuration: 30,
  staggerDelay: 200,
})

onMounted(() => {
  // Scroll reveal is auto-setup by composable
  // Everything else is reactive and ready
})
</script>

<template>
  <div class="introduction-section">
    <!-- Hero Banner -->
    <div class="intro-hero">
      <div class="intro-hero-content">
        <h1 class="intro-headline"><br /><span>OpenMMO</span></h1>
        <p class="intro-subtext">Turn ideas into digital businesses with AI</p>
        <div class="intro-cta-group">
          <button class="intro-cta intro-cta-primary">
            <span class="cta-text">Selling Through Email with AI</span>
            <span class="cta-arrow" aria-hidden="true">→</span>
          </button>
          <button class="intro-cta intro-cta-secondary">
            <span class="cta-text">SEO with AI</span>
            <span class="cta-arrow" aria-hidden="true">→</span>
          </button>
          <button class="intro-cta intro-cta-secondary">
            <span class="cta-text">Create a Website with AI</span>
            <span class="cta-arrow" aria-hidden="true">→</span>
          </button>
        </div>
      </div>
      <div class="intro-hero-visual" ref="phoneContainer">
        <div
          class="phone-wrapper"
          :style="{
            transform: `perspective(1200px) rotateX(${rotateX}deg) rotateY(${rotateY}deg)`,
          }"
        >
          <div class="phone-mockup">
            <img src="@/assets/hero-phone.png" alt="OpenMMO Mobile" class="phone-image" />
          </div>
        </div>
      </div>
    </div>

    <!-- Metrics Strip -->
    <div class="intro-metrics scroll-reveal">
      <div class="metric-item">
        <span class="metric-value">{{ animatedValues[0] }}</span>
        <span class="metric-label">Completed Projects</span>
      </div>
      <div class="metric-divider"></div>
      <div class="metric-item">
        <span class="metric-value">{{ animatedValues[1] }}</span>
        <span class="metric-suffix">%</span>
        <span class="metric-label">Client Retention Rate</span>
      </div>
      <div class="metric-divider"></div>
      <div class="metric-item">
        <span class="metric-value">{{ animatedValues[2] }}</span>
        <span class="metric-label">Industry Awards</span>
      </div>
      <div class="metric-divider"></div>
      <div class="metric-item">
        <span class="metric-value">24/7</span>
        <span class="metric-label">Global Support</span>
      </div>
    </div>

    <!-- Chat Assistance Section -->
    <div class="intro-chat-section scroll-reveal">
      <ChatAssistant />
    </div>

    <!-- Core Values -->

    <!-- Tech Stack Preview -->
    <div class="intro-tech scroll-reveal">
      <p class="tech-label">Technologies We Use</p>
      <div class="tech-marquee">
        <div class="tech-track">
          <span class="tech-item">Vue.js</span>
          <span class="tech-item">Java</span>
          <span class="tech-item">Open Claw</span>
          <span class="tech-item">OpenAI</span>
          <span class="tech-item">Gemini</span>
          <span class="tech-item">Vue.js</span>
          <span class="tech-item">Java</span>
          <span class="tech-item">Open Claw</span>
          <span class="tech-item">OpenAI</span>
          <span class="tech-item">Gemini</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.introduction-section {
  display: block;
  opacity: 0;
  padding: 0px 20px;
  position: relative;
  animation: sectionIn 0.6s ease-out forwards;
}

@keyframes sectionIn {
  from {
    opacity: 0;
    transform: translateY(40px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Scroll Reveal Animation */
.scroll-reveal {
  opacity: 0;
  transform: translateY(50px);
  transition: all 0.8s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.scroll-reveal.revealed {
  opacity: 1;
  transform: translateY(0);
}

/* Hero Banner */
.intro-hero {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 80px;
  align-items: center;
  margin-bottom: 100px;
  min-height: 600px;
}

.intro-hero-content {
  max-width: 550px;
}

.intro-badge {
  display: inline-block;
  padding: 8px 18px;
  background: rgba(0, 240, 255, 0.1);
  border: 1px solid rgba(0, 240, 255, 0.3);
  border-radius: 50px;
  font-size: 12px;
  letter-spacing: 2px;
  color: var(--primary);
  margin-bottom: 25px;
}

.intro-headline {
  font-family: 'Comfortaa', sans-serif;
  font-size: 56px;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 30px;
  color: #ffffff;
}

.intro-headline span {
  background: linear-gradient(135deg, var(--primary), var(--accent));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.intro-subtext {
  font-size: 18px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.65);
  margin-bottom: 40px;
}

.intro-cta-group {
  display: grid;
  grid-template-columns: repeat(2, minmax(180px, 1fr));
  gap: 12px;
  max-width: 520px;
}

.intro-cta {
  appearance: none;
  border: none;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 54px;
  padding: 0 18px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 999px;
  font-family: 'Comfortaa', sans-serif;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.45px;
  text-align: left;
  white-space: nowrap;
  cursor: pointer;
  user-select: none;
  transition:
    transform 0.22s ease,
    background 0.22s ease,
    border-color 0.28s ease,
    box-shadow 0.22s ease;
  overflow: hidden;
}

.intro-cta::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.35), transparent);
}

.intro-cta:hover {
  transform: translateY(-2px);
}

.intro-cta:active {
  transform: translateY(0);
}

.intro-cta:focus-visible {
  outline: 2px solid rgba(0, 240, 255, 0.7);
  outline-offset: 2px;
}

.intro-cta-primary {
  grid-column: 1 / -1;
  border: 1px solid rgba(0, 255, 170, 0.72);
  background: linear-gradient(135deg, rgba(0, 255, 191, 0.24), rgba(0, 240, 255, 0.1));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.2),
    0 10px 26px rgba(0, 200, 220, 0.3);
}

.intro-cta-primary .cta-text {
  color: #ebffff;
}

.intro-cta-primary:hover {
  background: linear-gradient(135deg, rgba(0, 240, 255, 0.3), rgba(0, 240, 255, 0.12));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.24),
    0 14px 30px rgba(0, 200, 220, 0.35);
}

.intro-cta-secondary {
  border: 1px solid rgba(255, 255, 255, 0.22);
  color: rgba(255, 255, 255, 0.92);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    0 6px 16px rgba(0, 0, 0, 0.22);
}

.intro-cta-secondary:hover {
  border-color: rgba(0, 240, 255, 0.4);
  background: rgba(255, 255, 255, 0.08);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.12),
    0 10px 22px rgba(0, 0, 0, 0.28);
}

.cta-text {
  font-size: 13px;
  font-weight: 600;
  line-height: 1;
  color: rgba(255, 255, 255, 0.9);
}

.cta-arrow {
  font-size: 14px;
  line-height: 1;
  opacity: 0.8;
  transform: translateX(0);
  transition:
    transform 0.2s ease,
    opacity 0.2s ease;
}

.intro-cta:hover .cta-arrow {
  opacity: 1;
  transform: translateX(2px);
}

/* Hero Visual - Phone Mockup */
.intro-hero-visual {
  position: relative;
  min-height: 500px;
  display: flex;
  justify-content: center;
  align-items: center;
  perspective: 1200px;
}

.phone-wrapper {
  transition: transform 0.12s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  transform-style: preserve-3d;
}

.phone-mockup {
  position: relative;
  filter: drop-shadow(0 20px 80px rgba(0, 240, 255, 0.15));
}

.phone-image {
  width: 100%;
  max-width: 420px;
  height: auto;
  display: block;
  filter: drop-shadow(0 30px 60px rgba(0, 0, 0, 0.5));
}

/* Metrics Strip */
.intro-metrics {
  display: flex;
  justify-content: space-around;
  align-items: center;
  gap: 40px;
  padding: 50px 60px;
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--glass-border);
  border-radius: 20px;
  margin: 80px 0;
  position: relative;
  overflow: hidden;
  border-radius: 20px;
  margin-bottom: 60px;
  position: relative;
  overflow: hidden;
}

.intro-metrics::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.15), transparent);
}

.metric-item {
  text-align: center;
}

.metric-value {
  font-family: 'Comfortaa', sans-serif;
  font-size: 42px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--primary), var(--accent));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  display: inline;
}

.metric-suffix {
  font-family: 'Comfortaa', sans-serif;
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--primary), var(--accent));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.metric-label {
  display: block;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 8px;
  letter-spacing: 1px;
}

.metric-divider {
  width: 1px;
  height: 50px;
  background: linear-gradient(180deg, transparent, rgba(255, 255, 255, 0.15), transparent);
}

/* Chat Section */
.intro-chat-section {
  width: 100%;
  margin: 100px 0;
  padding: 0;
}

/* Core Values */
.intro-values {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 30px;
  margin: 80px 0;
}

.value-card {
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--glass-border);
  border-radius: 20px;
  padding: 40px 35px;
  position: relative;
  overflow: hidden;
  transition: all 0.4s ease;
}

.value-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.12), transparent);
}

.value-card:hover {
  transform: translateY(-12px);
  border-color: rgba(0, 240, 255, 0.2);
  box-shadow: 0 30px 60px rgba(0, 0, 0, 0.4);
}

.value-number {
  font-family: 'Comfortaa', sans-serif;
  font-size: 52px;
  font-weight: 700;
  color: var(--primary);
  margin-bottom: 18px;
  line-height: 1;
}

.value-card h3 {
  font-family: 'Comfortaa', sans-serif;
  font-size: 22px;
  font-weight: 600;
  color: #ffffff;
  margin-bottom: 15px;
}

.value-card p {
  font-size: 15px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.65);
}

/* Tech Stack */
.intro-tech {
  text-align: center;
  margin: 100px 0;
}

.tech-label {
  font-size: 13px;
  letter-spacing: 3px;
  color: var(--primary);
  text-transform: uppercase;
  margin-bottom: 32px;
  font-weight: 600;
}

.tech-marquee {
  overflow: hidden;
  position: relative;
  padding: 20px 0;
}

.tech-marquee::before,
.tech-marquee::after {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  width: 100px;
  z-index: 2;
  pointer-events: none;
}

.tech-marquee::before {
  left: 0;
  background: linear-gradient(90deg, var(--dark-1), transparent);
}

.tech-marquee::after {
  right: 0;
  background: linear-gradient(-90deg, var(--dark-1), transparent);
}

.tech-track {
  display: flex;
  gap: 40px;
  animation: marquee 20s linear infinite;
}

@keyframes marquee {
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(-50%);
  }
}

.tech-item {
  font-family: 'Comfortaa', sans-serif;
  font-size: 18px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.3);
  white-space: nowrap;
  transition: color 0.3s ease;
}

.tech-item:hover {
  color: var(--primary);
}

@media (max-width: 992px) {
  .intro-hero {
    grid-template-columns: 1fr;
    gap: 50px;
    min-height: auto;
  }

  .intro-hero-content {
    text-align: center;
    max-width: 100%;
  }

  .intro-cta-group {
    margin: 0 auto;
  }

  .intro-hero-visual {
    min-height: 400px;
  }

  .intro-metrics {
    flex-wrap: wrap;
    gap: 40px;
  }

  .metric-divider {
    display: none;
  }

  .intro-values {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .intro-hero {
    gap: 35px;
    min-height: auto;
  }

  .intro-headline {
    font-size: 40px;
    margin-bottom: 20px;
  }

  .intro-subtext {
    font-size: 16px;
    margin-bottom: 28px;
  }

  .intro-hero-visual {
    display: none;
  }

  .intro-metrics {
    padding: 35px 28px;
    gap: 30px;
  }

  .metric-value {
    font-size: 36px;
  }

  .metric-label {
    font-size: 12px;
  }
}

@media (max-width: 480px) {
  .intro-hero {
    padding: 30px 0;
    margin-bottom: 50px;
  }

  .intro-headline {
    font-size: 32px;
    line-height: 1.3;
  }

  .intro-subtext {
    font-size: 15px;
    line-height: 1.7;
  }

  .intro-cta-group {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .intro-cta-primary {
    grid-column: auto;
  }

  .intro-cta {
    width: 100%;
    min-height: 50px;
    padding: 0 16px;
  }

  .cta-text {
    font-size: 12px;
  }

  .intro-metrics {
    flex-direction: column;
    padding: 25px 20px;
    gap: 20px;
  }

  .metric-value {
    font-size: 32px;
  }

  .metric-label {
    font-size: 11px;
  }

  .intro-chat-section {
    margin: 50px 0;
  }

  .intro-values {
    gap: 20px;
  }

  .value-card {
    padding: 28px 22px;
  }

  .value-number {
    font-size: 42px;
  }

  .value-card h3 {
    font-size: 18px;
  }

  .value-card p {
    font-size: 14px;
  }

  .tech-item {
    font-size: 16px;
  }
}
</style>
