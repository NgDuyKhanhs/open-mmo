<script setup lang="ts">
  import SectionHeader from '../components/SectionHeader.vue'
</script>

<template>
  <div class="legal-section">
    <SectionHeader />

    <div class="legal-content">
      <h1>Privacy Policy</h1>
      <p class="last-updated">Last Updated: April 4, 2026</p>

      <section class="policy-section">
        <h2>1. Introduction</h2>
        <p>
          Welcome to OpenMMO AI ("we," "us," "our," or "Company"). We are committed to protecting your privacy and
          ensuring you have a positive experience on our platform. This Privacy Policy explains how we collect, use,
          disclose, and safeguard your information when you visit our website, applications, and services.
        </p>
      </section>

      <section class="policy-section">
        <h2>2. Information We Collect</h2>
        <p>We may collect information about you in a variety of ways. The information we may collect on our Site includes:</p>
        <ul>
          <li><strong>Personal Data:</strong> Name, email address, phone number, profile information, and authentication credentials</li>
          <li><strong>Gmail Data:</strong> Email address, email contents, labels, and message metadata when you connect your Gmail account</li>
          <li><strong>Usage Data:</strong> Log files, browser type, IP address, access times, and pages viewed</li>
          <li><strong>Device Information:</strong> Device type, operating system, and unique device identifiers</li>
          <li><strong>Cookies and Tracking:</strong> Information collected through cookies, web beacons, and similar technologies</li>
        </ul>
      </section>

      <section class="policy-section">
        <h2>3. Use of Your Information</h2>
        <p>Having accurate information about you permits us to provide you with a smooth, efficient, and customized experience. Specifically, we may use information collected about you via our Site to:</p>
        <ul>
          <li>Create and manage your account</li>
          <li>Process transactions and send transaction confirmations</li>
          <li>Deliver AI-powered email automation services</li>
          <li>Improve and optimize our services</li>
          <li>Communicate with you about updates, security alerts, and support</li>
          <li>Comply with legal obligations</li>
          <li>Prevent fraudulent transactions and other illegal activities</li>
          <li>Personalize and improve your user experience</li>
        </ul>
      </section>

      <section class="policy-section">
        <h2>4. Gmail Data Privacy</h2>
        <p>When you connect your Gmail account, we access specific email data to provide our AI-powered email automation service. Specifically:</p>
        <ul>
          <li>We access emails only to identify messages matching your configured criteria (e.g., subject containing specific keywords)</li>
          <li>We read email content solely to generate appropriate AI-powered responses</li>
          <li>We send replies on your behalf according to your configured preferences</li>
          <li>We securely store only metadata and processed information; we do not permanently store full email contents</li>
          <li>You can revoke access at any time through your Google Account settings</li>
          <li>Your Gmail account credentials and tokens are encrypted using industry-standard AES-GCM encryption</li>
        </ul>
      </section>

      <section class="policy-section">
        <h2>5. Disclosure of Your Information</h2>
        <p>We may share your information in the following situations:</p>
        <ul>
          <li><strong>Service Providers:</strong> With vendors, consultants, and service providers who assist us in operating our website, conducting our business, or servicing you</li>
          <li><strong>Google Services:</strong> Gmail data is processed through Google's APIs in accordance with Google's terms of service</li>
          <li><strong>AI Services:</strong> Text content may be processed through Gemini API for generating email responses</li>
          <li><strong>Legal Requirement:</strong> If required by law or if we have good faith belief that disclosure is necessary</li>
          <li><strong>Business Transfer:</strong> In connection with a merger, acquisition, bankruptcy, or sale of assets</li>
        </ul>
      </section>

      <section class="policy-section">
        <h2>6. Security of Your Information</h2>
        <p>
          We use administrative, technical, and physical security measures to protect your personal information. While we strive to use
          commercially acceptable means to protect your Personal Data, we cannot guarantee its absolute security. Your use of our Service
          and any information you provide is at your own risk.
        </p>
      </section>

      <section class="policy-section">
        <h2>7. Contact Us</h2>
        <p>If you have questions or comments about this Privacy Policy, please contact us at:</p>
        <div class="contact-info">
          <p><strong>Email:</strong> openmmo@gmail.com</p>
          <p><strong>Address:</strong> TC3 Vinhomes Smart City</p>
          <p><strong>Phone:</strong> +84 000000000</p>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
  .legal-section {
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

  .legal-content {
    width: 100%;
  }

  .legal-content h1 {
    font-family: 'Comfortaa', sans-serif;
    font-size: 42px;
    font-weight: 600;
    margin-bottom: 10px;
    background: linear-gradient(135deg, #00f0ff 0%, #ffffff 50%, #ff00ff 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .last-updated {
    color: rgba(255, 255, 255, 0.5);
    font-size: 13px;
    margin-bottom: 40px;
    font-style: italic;
  }

  .policy-section {
    margin-bottom: 40px;
  }

  .policy-section h2 {
    font-family: 'Comfortaa', sans-serif;
    font-size: 24px;
    color: #00f0ff;
    margin-bottom: 15px;
    font-weight: 600;
  }

  .policy-section p {
    color: rgba(255, 255, 255, 0.8);
    line-height: 1.8;
    margin-bottom: 15px;
    font-size: 14px;
  }

  .policy-section ul {
    list-style: none;
    padding-left: 0;
    margin: 15px 0;
  }

  .policy-section li {
    color: rgba(255, 255, 255, 0.8);
    line-height: 1.8;
    margin-bottom: 12px;
    padding-left: 25px;
    position: relative;
    font-size: 14px;
  }

  .policy-section li:before {
    content: '→';
    position: absolute;
    left: 0;
    color: #00f0ff;
    font-weight: bold;
  }

  .policy-section strong {
    color: #00f0ff;
  }

  .contact-info {
    background: rgba(0, 240, 255, 0.05);
    border: 1px solid rgba(0, 240, 255, 0.2);
    border-radius: 10px;
    padding: 20px;
    margin-top: 15px;
  }

  .contact-info p {
    margin-bottom: 10px;
  }

  .contact-info p:last-child {
    margin-bottom: 0;
  }

  @media (max-width: 768px) {
    .legal-section {
      padding: 60px 15px;
    }

    .legal-content h1 {
      font-size: 28px;
    }

    .policy-section h2 {
      font-size: 18px;
    }

    .policy-section p,
    .policy-section li {
      font-size: 13px;
    }
  }
</style>

