<script setup lang="ts">
  interface Props {
    onBack: () => void
  }

  defineProps<Props>()
</script>
<style scoped>
  .section-header-small {
    position: fixed;
    top: 20px;
    left: 20px;
    display: flex;
    align-items: center;
    gap: 12px;
    z-index: 100;
    background: var(--glass-bg);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    padding: 12px 20px;
    border-radius: 50px;
    border: 1px solid var(--glass-border);
  }

  .small-logo {
    width: 32px;
    height: 32px;
  }

  .small-logo svg {
    width: 100%;
    height: 100%;
    filter: drop-shadow(0 0 10px var(--glow-cyan));
  }

  .small-brand h3 {
    font-family: 'Comfortaa', sans-serif;
    font-size: 14px;
    font-weight: 600;
    letter-spacing: 2px;
    color: var(--primary);
  }

  .small-brand p {
    font-size: 9px;
    letter-spacing: 1px;
    color: rgba(255, 255, 255, 0.5);
    margin-top: 2px;
  }

  .back-btn {
    position: fixed;
    top: 20px;
    right: 20px;
    padding: 12px 24px;
    background: var(--glass-bg);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border: 1px solid var(--glass-border);
    border-radius: 50px;
    color: rgba(255, 255, 255, 0.8);
    font-family: 'Comfortaa', sans-serif;
    font-size: 13px;
    letter-spacing: 2px;
    cursor: pointer;
    transition: all 0.3s ease;
    z-index: 100;
  }

  .back-btn:hover {
    background: rgba(0, 240, 255, 0.1);
    border-color: var(--primary);
    color: var(--primary);
    box-shadow: 0 0 20px var(--glow-cyan);
  }

  @media (max-width: 768px) {
    .section-header-small {
      position: relative;
      top: auto;
      left: auto;
      margin-bottom: 20px;
      justify-content: center;
    }

    .back-btn {
      position: relative;
      top: auto;
      right: auto;
      display: block;
      width: fit-content;
      margin: 0 auto 30px;
    }
  }
</style>
