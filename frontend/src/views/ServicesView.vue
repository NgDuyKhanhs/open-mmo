<script setup lang="ts">
import { ref } from 'vue'
import { SERVICES_CONFIG } from '@/config'

const activeTab = ref('earning')

const tabs = [
  { id: 'earning', name: 'Monetization' },
  { id: 'skills', name: 'Skills' },
  { id: 'tools', name: 'Tools' },
  { id: 'support', name: 'Support' },
]

const services = SERVICES_CONFIG

const switchTab = (tabId: string) => {
  activeTab.value = tabId
}
</script>

<template>
  <div class="services-section">
    <div class="tabs-container">
      <div class="tab-buttons">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          :class="['tab-btn', { active: activeTab === tab.id }]"
          @click="switchTab(tab.id)"
        >
          {{ tab.name }}
        </button>
      </div>

      <div class="tab-content">
        <!-- Monetization Tab -->
        <div v-if="activeTab === 'earning'" class="tab-pane active service-pane">
          <div class="services-list">
            <div v-for="(service, idx) in services.earning" :key="idx" class="service-card">
              <div class="service-icon">{{ service.icon }}</div>
              <div class="service-body">
                <h4 class="service-title">{{ service.title }}</h4>
                <p class="service-desc">{{ service.desc }}</p>
                <div class="service-meta">
                  <span class="meta-label">Revenue:</span>
                  <span class="meta-value">{{ service.revenue }}</span>
                </div>
              </div>
              <div class="service-arrow">→</div>
            </div>
          </div>
        </div>

        <!-- Skills Tab -->
        <div v-if="activeTab === 'skills'" class="tab-pane active service-pane">
          <div class="services-list">
            <div v-for="(service, idx) in services.skills" :key="idx" class="service-card">
              <div class="service-icon">{{ service.icon }}</div>
              <div class="service-body">
                <h4 class="service-title">{{ service.title }}</h4>
                <p class="service-desc">{{ service.desc }}</p>
                <div class="service-meta">
                  <span class="meta-label">Level:</span>
                  <span class="meta-value">{{ service.level }}</span>
                </div>
              </div>
              <div class="service-arrow">→</div>
            </div>
          </div>
        </div>

        <!-- Tools Tab -->
        <div v-if="activeTab === 'tools'" class="tab-pane active service-pane">
          <div class="services-list">
            <div v-for="(service, idx) in services.tools" :key="idx" class="service-card">
              <div class="service-icon">{{ service.icon }}</div>
              <div class="service-body">
                <h4 class="service-title">{{ service.title }}</h4>
                <p class="service-desc">{{ service.desc }}</p>
                <div class="service-meta">
                  <span class="meta-label">Best for:</span>
                  <span class="meta-value">{{ service.use }}</span>
                </div>
              </div>
              <div class="service-arrow">→</div>
            </div>
          </div>
        </div>

        <!-- Support Tab -->
        <div v-if="activeTab === 'support'" class="tab-pane active service-pane">
          <div class="services-list">
            <div v-for="(service, idx) in services.support" :key="idx" class="service-card">
              <div class="service-icon">{{ service.icon }}</div>
              <div class="service-body">
                <h4 class="service-title">{{ service.title }}</h4>
                <p class="service-desc">{{ service.desc }}</p>
              </div>
              <div class="service-arrow">→</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.services-section {
  display: block;
  opacity: 0;
  padding: 0px 20px;
  position: relative;
  animation: sectionIn 0.6s ease-out forwards;
  max-width: 1200px;
  margin: 0 auto;
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

.services-header {
  text-align: center;
  margin-bottom: 70px;
}

.section-title {
  font-family: 'Comfortaa', sans-serif;
  font-size: 42px;
  font-weight: 700;
  margin-bottom: 15px;
  background: linear-gradient(135deg, var(--primary) 0%, #ffffff 50%, var(--secondary) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 0.3px;
}

.section-subtitle {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.55);
  letter-spacing: 0.5px;
  font-weight: 400;
}

.tabs-container {
  margin-bottom: 30px;
}

.tab-buttons {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 60px;
}

.tab-btn {
  padding: 11px 28px;
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--glass-border);
  border-radius: 50px;
  color: rgba(255, 255, 255, 0.55);
  font-family: 'Comfortaa', sans-serif;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.4px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.tab-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.05), transparent);
  opacity: 0;
  transition: opacity 0.3s ease;
  border-radius: 50px;
}

.tab-btn:hover {
  background: rgba(255, 255, 255, 0.04);
  color: rgba(255, 255, 255, 0.8);
  border-color: rgba(0, 240, 255, 0.25);
  transform: translateY(-2px);
}

.tab-btn.active {
  background: rgba(0, 240, 255, 0.06);
  border-color: var(--primary);
  color: var(--primary);
  box-shadow: 0 0 15px rgba(0, 240, 255, 0.2);
}

.tab-btn.active::before {
  opacity: 1;
}

.service-pane {
  animation: tabFade 0.4s ease-out;
}

@keyframes tabFade {
  from {
    opacity: 0;
    transform: translateY(15px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.services-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
  margin-top: 0;
}

.service-card {
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--glass-border);
  border-radius: 20px;
  padding: 25px;
  display: flex;
  gap: 18px;
  position: relative;
  overflow: hidden;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
}

.service-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0, 240, 255, 0.2), transparent);
  opacity: 0;
  transition: opacity 0.35s ease;
}

.service-card:hover {
  transform: translateY(-6px);
  background: rgba(255, 255, 255, 0.04);
  box-shadow: 0 15px 40px rgba(0, 240, 255, 0.1);
}

.service-card:hover::before {
  opacity: 1;
}

.service-icon {
  font-size: 44px;
  min-width: 65px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.service-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
}

.service-title {
  font-family: 'Comfortaa', sans-serif;
  font-size: 17px;
  font-weight: 600;
  color: var(--primary);
  letter-spacing: 0.2px;
  margin: 0;
}

.service-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.58);
  line-height: 1.6;
  margin: 0;
}

.service-meta {
  display: flex;
  gap: 8px;
  font-size: 11px;
  padding: 8px 11px;
  background: rgba(0, 240, 255, 0.04);
  border: 1px solid rgba(0, 240, 255, 0.08);
  border-radius: 8px;
  width: fit-content;
}

.meta-label {
  color: rgba(255, 255, 255, 0.45);
  font-weight: 500;
}

.meta-value {
  color: var(--primary);
  font-weight: 600;
}

.service-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: rgba(0, 240, 255, 0.3);
  transition: all 0.35s ease;
  min-width: 35px;
  flex-shrink: 0;
}

.service-card:hover .service-arrow {
  color: var(--primary);
  transform: translateX(4px);
}

@media (max-width: 1024px) {
  .services-list {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .services-section {
    padding: 70px 20px;
  }

  .section-title {
    font-size: 36px;
  }

  .services-header {
    margin-bottom: 50px;
  }
}

@media (max-width: 768px) {
  .services-section {
    padding: 60px 15px;
  }

  .services-header {
    margin-bottom: 45px;
  }

  .section-title {
    font-size: 28px;
    margin-bottom: 12px;
  }

  .section-subtitle {
    font-size: 14px;
  }

  .tab-buttons {
    gap: 10px;
    margin-bottom: 40px;
  }

  .tab-btn {
    padding: 10px 24px;
    font-size: 12px;
  }

  .services-list {
    gap: 18px;
  }

  .service-card {
    padding: 20px;
    gap: 15px;
  }

  .service-icon {
    font-size: 40px;
    min-width: 55px;
  }

  .service-title {
    font-size: 16px;
  }

  .service-desc {
    font-size: 12px;
  }

  .service-meta {
    font-size: 10px;
    padding: 7px 9px;
  }
}

@media (max-width: 480px) {
  .services-section {
    padding: 50px 12px;
  }

  .services-header {
    margin-bottom: 35px;
  }

  .section-title {
    font-size: 24px;
    margin-bottom: 10px;
  }

  .section-subtitle {
    font-size: 13px;
  }

  .tab-buttons {
    flex-direction: column;
    margin-bottom: 30px;
    gap: 8px;
  }

  .tab-btn {
    width: 100%;
    padding: 11px 20px;
    font-size: 12px;
  }

  .services-list {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .service-card {
    flex-direction: column;
    padding: 18px;
  }

  .service-icon {
    font-size: 36px;
    min-width: auto;
  }

  .service-title {
    font-size: 15px;
  }

  .service-desc {
    font-size: 12px;
  }

  .service-arrow {
    display: none;
  }
}
</style>
