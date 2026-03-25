<script setup lang="ts">
import { ref } from 'vue'
import SectionHeader from '../components/SectionHeader.vue'

interface Props {
  onBack: () => void
}

defineProps<Props>()

const formData = ref({
  name: '',
  email: '',
  subject: '',
  message: '',
})

const submitted = ref(false)

const handleSubmit = () => {
  console.log('Form submitted:', formData.value)
  submitted.value = true
  setTimeout(() => {
    submitted.value = false
    formData.value = { name: '', email: '', subject: '', message: '' }
  }, 3000)
}

const contactInfo = [
  {
    id: 1,
    icon: '📍',
    title: 'Địa Chỉ',
    content: 'TC3 Vinhomes Smart City',
  },
  {
    id: 2,
    icon: '✉️',
    title: 'Email',
    content: 'openmmo@gmail.com',
  },
  {
    id: 3,
    icon: '📞',
    title: 'Điện Thoại',
    content: '+84 000000000',
  },
]
</script>

<template>
  <div class="contact-section">
    <SectionHeader :on-back="onBack" />

    <div class="contact-grid">
      <div class="contact-info">
        <div v-for="info in contactInfo" :key="info.id" class="contact-item">
          <div class="item-icon">{{ info.icon }}</div>
          <div class="item-content">
            <h4>{{ info.title }}</h4>
            <p>{{ info.content }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.contact-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  opacity: 0;
  padding: 80px 20px;
  position: relative;
  animation: sectionIn 0.6s ease-out forwards;
  max-width: 900px;
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

.section-header {
  text-align: center;
  margin-bottom: 50px;
}

.section-title {
  font-family: 'Comfortaa', sans-serif;
  font-size: 42px;
  font-weight: 600;
  margin-bottom: 15px;
  background: linear-gradient(135deg, var(--primary) 0%, #ffffff 50%, var(--secondary) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.section-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 2px;
}

.contact-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 60px;
  width: 100%;
  max-width: 600px;
  margin: 0 auto;
}

.contact-form {
  display: flex;
  flex-direction: column;
}

form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  font-weight: 500;
}

.form-group input,
.form-group textarea {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(0, 240, 255, 0.2);
  border-radius: 10px;
  padding: 12px 15px;
  color: #ffffff;
  font-family: inherit;
  font-size: 13px;
  transition: all 0.3s ease;
}

.form-group input::placeholder,
.form-group textarea::placeholder {
  color: rgba(255, 255, 255, 0.3);
}

.form-group input:focus,
.form-group textarea:focus {
  outline: none;
  background: rgba(255, 255, 255, 0.08);
  border-color: var(--primary);
  box-shadow: 0 0 20px rgba(0, 240, 255, 0.2);
}

.form-group textarea {
  resize: vertical;
  font-family: 'Courier New', monospace;
}

.submit-btn {
  background: linear-gradient(135deg, var(--primary) 0%, var(--secondary) 100%);
  border: none;
  border-radius: 10px;
  padding: 13px 30px;
  color: #000;
  font-family: 'Comfortaa', sans-serif;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s ease;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-top: 10px;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px rgba(255, 255, 255, 0.1);
}

.submit-btn:active {
  transform: translateY(0);
}

.success-message {
  color: var(--primary);
  font-size: 13px;
  text-align: center;
  animation: fadeUp 0.5s ease-out;
}

.contact-info {
  display: flex;
  flex-direction: column;
  gap: 25px;
}

.contact-item {
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--glass-border);
  border-radius: 20px;
  padding: 25px;
  display: flex;
  gap: 20px;
  align-items: flex-start;
  transition: all 0.3s ease;
}

.contact-item:hover {
  transform: translateY(-5px);
  border-color: var(--primary);
}

.item-icon {
  font-size: 32px;
  min-width: 50px;
  text-align: center;
}

.item-content h4 {
  font-family: 'Comfortaa', sans-serif;
  font-size: 16px;
  color: var(--primary);
  margin-bottom: 5px;
}

.item-content p {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.6;
}

@media (max-width: 992px) {
  .contact-grid {
    grid-template-columns: 1fr;
    gap: 40px;
  }

  .contact-info {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .contact-item {
    flex: 1;
    min-width: 200px;
  }
}

@media (max-width: 768px) {
  .section-title {
    font-size: 28px;
  }

  .contact-info {
    flex-direction: column;
  }

  .contact-item {
    flex: unset;
  }
}
</style>
