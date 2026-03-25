<script setup lang="ts"></script>

<template>
  <footer class="footer">
    <p>
      © 2026 Open MMO. Design by
      <a href="" target="_blank">Enzo</a>
    </p>
  </footer>
</template>

<style scoped>
.footer {
  margin-top: auto;
  padding: 30px 0;
  text-align: center;
  opacity: 0;
  animation: fadeUp 0.5s ease-out 1.7s forwards;
}

.footer p {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  letter-spacing: 1px;
}

.footer a {
  color: var(--primary);
  text-decoration: none;
  transition: color 0.3s ease;
}

.footer a:hover {
  color: var(--secondary);
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
</style>

