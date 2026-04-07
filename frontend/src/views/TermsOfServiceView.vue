<script setup lang="ts">
  import SectionHeader from '../components/SectionHeader.vue'
</script>

<template>
  <div class="legal-section">
    <SectionHeader />

    <div class="legal-content">
      <h1>Terms of Service</h1>
      <p class="last-updated">Last Updated: April 4, 2026</p>

      <section class="policy-section">
        <h2>1. Agreement to Terms</h2>
        <p>
          By accessing and using OpenMMO AI ("Service"), you accept and agree to be bound by and comply with the terms and provision of this agreement.
          If you do not agree to abide by the above, please do not use this service.
        </p>
      </section>

      <section class="policy-section">
        <h2>2. Use License</h2>
        <p>
          Permission is granted to temporarily download one copy of the materials (information or software) on OpenMMO AI for personal,
          non-commercial transitory viewing only. This is the grant of a license, not a transfer of title, and under this license you may not:
        </p>
        <ul>
          <li>Modify or copying the materials</li>
          <li>Using the materials for any commercial purpose or for any public display</li>
          <li>Attempting to decompile or reverse engineer any software contained on OpenMMO AI</li>
          <li>Removing any copyright or other proprietary notations from the materials</li>
          <li>Transferring the materials to another person or "mirror" the materials on any other server</li>
        </ul>
      </section>

      <section class="policy-section">
        <h2>3. Disclaimer</h2>
        <p>
          The materials on OpenMMO AI are provided on an 'as is' basis. OpenMMO AI makes no warranties, expressed or implied, and hereby disclaims
          and negates all other warranties including, without limitation, implied warranties or conditions of merchantability, fitness for a
          particular purpose, or non-infringement of intellectual property or other violation of rights.
        </p>
      </section>

      <section class="policy-section">
        <h2>4. Limitations</h2>
        <p>
          In no event shall OpenMMO AI or its suppliers be liable for any damages (including, without limitation, damages for loss of data or profit,
          or due to business interruption) arising out of the use or inability to use the materials on OpenMMO AI, even if OpenMMO AI or an authorized
          representative has been notified orally or in writing of the possibility of such damage.
        </p>
      </section>

      <section class="policy-section">
        <h2>5. Accuracy of Materials</h2>
        <p>
          The materials appearing on OpenMMO AI could include technical, typographical, or photographic errors. OpenMMO AI does not warrant that any
          of the materials on OpenMMO AI are accurate, complete, or current. OpenMMO AI may make changes to the materials contained on OpenMMO AI at any
          time without notice.
        </p>
      </section>

      <section class="policy-section">
        <h2>6. Materials and Services</h2>
        <p>
          The materials on OpenMMO AI and certain Services offered in connection with OpenMMO AI (the "Services") are provided "as-is." OpenMMO AI does not
          warrant the accuracy, completeness or usefulness of this information. Any reliance you place on such information is strictly at your own risk.
          OpenMMO AI disclaims all liability and responsibility arising from any reliance placed on such materials by you or any visitor to OpenMMO AI,
          or by anyone who may be informed of any of its contents.
        </p>
      </section>

      <section class="policy-section">
        <h2>7. Gmail API Usage</h2>
        <p>
          When using OpenMMO AI's email automation features, you grant OpenMMO AI permission to access your Gmail account for the following purposes:
        </p>
        <ul>
          <li>Reading emails matching your configured criteria</li>
          <li>Sending replies on your behalf based on AI-generated responses</li>
          <li>Organizing emails with labels as specified in your preferences</li>
          <li>You can revoke this access at any time through Google's security settings</li>
        </ul>
        <p>
          You are responsible for maintaining the confidentiality of your account credentials. OpenMMO AI will never store your Gmail password.
          All access is conducted through secure OAuth 2.0 authentication.
        </p>
      </section>

      <section class="policy-section">
        <h2>8. AI-Generated Content</h2>
        <p>
          OpenMMO AI uses artificial intelligence to generate email responses. While we strive for quality and accuracy, AI-generated content may not
          be perfect and you are responsible for reviewing and approving all responses before they are sent. OpenMMO AI is not liable for any issues,
          errors, or negative consequences resulting from AI-generated email responses.
        </p>
      </section>

      <section class="policy-section">
        <h2>9. User Responsibilities</h2>
        <p>You agree to:</p>
        <ul>
          <li>Use OpenMMO AI only for lawful purposes and in a way that does not infringe upon the rights of others or restrict their use and enjoyment of OpenMMO AI</li>
          <li>Not transmit obscene or offensive content or disrupt the normal flow of dialogue within OpenMMO AI</li>
          <li>Not attempt to gain unauthorized access to OpenMMO AI's computer systems or networks</li>
          <li>Not transmit any malware or viruses that could damage or degrade the performance of OpenMMO AI</li>
          <li>Comply with all laws and regulations in your jurisdiction</li>
        </ul>
      </section>

      <section class="policy-section">
        <h2>10. Limitation of Liability</h2>
        <p>
          In no event will OpenMMO AI, its affiliates, officers, directors, employees, agents, suppliers, or licensors be liable to you or any third
          party for any indirect, incidental, special, consequential, or punitive damages, including for any lost profits, lost revenue, lost data,
          or other damages arising from your use of OpenMMO AI.
        </p>
      </section>

      <section class="policy-section">
        <h2>11. Indemnification</h2>
        <p>
          You agree to defend, indemnify, and hold harmless OpenMMO AI and its affiliates, officers, directors, employees, agents, suppliers, and
          licensors from any claim, demand, action, fine, or other loss arising from or relating to your breach of these Terms of Service or your use of OpenMMO AI.
        </p>
      </section>

      <section class="policy-section">
        <h2>12. Termination</h2>
        <p>
          OpenMMO AI reserves the right to terminate or suspend your account and access to OpenMMO AI immediately, without prior notice or liability,
          if you breach any of the Terms of Service or if OpenMMO AI determines, in its sole discretion, that you have violated any applicable laws or
          regulations.
        </p>
      </section>

      <section class="policy-section">
        <h2>13. Governing Law</h2>
        <p>
          These terms and conditions are governed by and construed in accordance with the laws of Vietnam, and you irrevocably submit to the exclusive
          jurisdiction of the courts located in Ho Chi Minh City, Vietnam.
        </p>
      </section>

      <section class="policy-section">
        <h2>14. Contact Information</h2>
        <p>If you have any questions about these Terms of Service, please contact us at:</p>
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

