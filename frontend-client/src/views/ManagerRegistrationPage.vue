<script setup lang="ts">
import { computed, ref } from 'vue';
import { Button, Card, Input } from 'animal-island-vue';
import { RouterLink } from 'vue-router';
import AppShell from '@/components/AppShell.vue';
import { apiRequest } from '@/lib/api';
import type {
  ManagerRegistrationConfirmRequest,
  ManagerRegistrationQrPayload,
  ManagerRegistrationRequest,
  ManagerRegistrationResult,
} from '@/types/api';

const displayName = ref('');
const username = ref('');
const password = ref('');
const confirmPassword = ref('');
const totpCode = ref('');
const qrPayload = ref<ManagerRegistrationQrPayload | null>(null);
const successMessage = ref('');
const errorMessage = ref('');
const generatingQr = ref(false);
const confirmingRegistration = ref(false);

const passwordsMatch = computed(() => password.value === confirmPassword.value);
const canGenerateQr = computed(() => {
  return Boolean(
    displayName.value.trim()
      && username.value.trim()
      && password.value
      && confirmPassword.value
      && passwordsMatch.value,
  );
});
const canConfirm = computed(() => Boolean(qrPayload.value) && /^\d{6}$/.test(totpCode.value.trim()));

async function generateQr() {
  if (!canGenerateQr.value) {
    errorMessage.value = 'Complete the manager account form before generating the QR code.';
    return;
  }

  generatingQr.value = true;
  errorMessage.value = '';
  successMessage.value = '';

  try {
    qrPayload.value = await apiRequest<ManagerRegistrationQrPayload>('/api/public/manager-registration/qr', {
      method: 'POST',
      body: JSON.stringify({
        displayName: displayName.value.trim(),
        username: username.value.trim(),
        password: password.value,
      } satisfies ManagerRegistrationRequest),
    });
    totpCode.value = '';
  } catch (error) {
    qrPayload.value = null;
    errorMessage.value = error instanceof Error ? error.message : 'Generating the manager QR code failed.';
  } finally {
    generatingQr.value = false;
  }
}

async function confirmManagerRegistration() {
  if (!qrPayload.value) {
    return;
  }

  confirmingRegistration.value = true;
  errorMessage.value = '';
  successMessage.value = '';

  try {
    const response = await apiRequest<ManagerRegistrationResult>('/api/public/manager-registration/confirm', {
      method: 'POST',
      body: JSON.stringify({
        registrationId: qrPayload.value.registrationId,
        totpCode: totpCode.value.trim(),
      } satisfies ManagerRegistrationConfirmRequest),
    });
    successMessage.value = response.message;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Confirming the manager registration failed.';
  } finally {
    confirmingRegistration.value = false;
  }
}

function resetRegistration() {
  qrPayload.value = null;
  successMessage.value = '';
  errorMessage.value = '';
  totpCode.value = '';
}
</script>

<template>
  <AppShell
    title="Manager Registration"
    subtitle="This page works only when it is opened from the private manager-registration URL. The backend validates the hidden verification segment before issuing any QR code."
    :manager-mode="true"
  >
    <section class="registration-grid">
      <Card class="panel">
        <div class="panel-header">
          <p class="eyebrow">Step 1</p>
          <h2>Create the manager account</h2>
        </div>

        <p class="panel-copy">
          Submit the manager username, display name, and password first. The backend will create a dedicated TOTP secret and
          return a QR code only if this private route is still enabled in <code>application.yaml</code>.
        </p>

        <div class="form-grid">
          <label>
            <span>Display Name</span>
            <Input v-model="displayName" placeholder="Manager display name" />
          </label>

          <label>
            <span>Username</span>
            <Input v-model="username" placeholder="Manager username" />
          </label>

          <label>
            <span>Password</span>
            <Input v-model="password" type="password" placeholder="At least 8 characters" />
          </label>

          <label>
            <span>Confirm Password</span>
            <Input v-model="confirmPassword" type="password" placeholder="Repeat the password" />
          </label>
        </div>

        <p v-if="!passwordsMatch && confirmPassword" class="warning-banner">The password confirmation does not match.</p>
        <p v-if="errorMessage" class="error-banner">{{ errorMessage }}</p>
        <p v-if="successMessage" class="success-banner">{{ successMessage }}</p>

        <div class="panel-actions">
          <Button type="default" :disabled="!qrPayload" @click="resetRegistration">Reset</Button>
          <Button type="primary" :disabled="!canGenerateQr" :loading="generatingQr" @click="generateQr">
            {{ generatingQr ? 'Generating QR...' : 'Generate QR Code' }}
          </Button>
        </div>
      </Card>

      <Card class="panel qr-panel">
        <div class="panel-header">
          <p class="eyebrow">Step 2</p>
          <h2>Bind and confirm the authenticator</h2>
        </div>

        <template v-if="qrPayload">
          <img class="qr-image" :src="qrPayload.qrCodeImage" alt="Manager authenticator QR code" />

          <div class="secret-box">
            <p>Manual setup key</p>
            <code>{{ qrPayload.manualEntryKey }}</code>
          </div>

          <p class="panel-copy">
            Scan the QR code in your authenticator app. Then enter the first 6-digit code here to activate the manager
            account.
          </p>

          <label class="confirm-label">
            <span>Authenticator Code</span>
            <Input v-model="totpCode" :maxlength="6" placeholder="6-digit code" />
          </label>

          <div class="panel-actions">
            <Button type="primary" :disabled="!canConfirm" :loading="confirmingRegistration" @click="confirmManagerRegistration">
              {{ confirmingRegistration ? 'Confirming...' : 'Confirm Registration' }}
            </Button>
          </div>
        </template>

        <template v-else>
          <p class="panel-copy">
            The manager-specific QR code will appear here after the private route passes backend validation and the account
            details are accepted.
          </p>
        </template>

        <RouterLink to="/servers" class="directory-link">Back to server directory</RouterLink>
      </Card>
    </section>
  </AppShell>
</template>

<style scoped>
.registration-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  gap: 20px;
}

.panel {
  display: grid;
  gap: 18px;
}

.panel-header {
  display: grid;
  gap: 6px;
}

.panel-header h2,
.panel-copy,
.secret-box p {
  margin: 0;
}

.eyebrow {
  margin: 0;
  color: var(--animal-primary-color);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.panel-copy {
  color: var(--animal-text-color-secondary);
  line-height: 1.6;
}

.form-grid {
  display: grid;
  gap: 14px;
}

.form-grid label,
.confirm-label {
  display: grid;
  gap: 8px;
  color: var(--animal-warm-color-soft);
  font-weight: 700;
}

.panel-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.warning-banner,
.error-banner,
.success-banner {
  margin: 0;
  border-radius: var(--animal-border-radius-base);
  padding: 12px 14px;
  font-weight: 700;
}

.warning-banner {
  background: rgba(238, 191, 67, 0.16);
  color: #8a6500;
}

.error-banner {
  background: rgba(224, 90, 90, 0.14);
  color: var(--animal-error-color);
}

.success-banner {
  background: rgba(71, 166, 106, 0.16);
  color: #2f7c4b;
}

.qr-panel {
  align-content: start;
}

.qr-image {
  width: min(100%, 320px);
  justify-self: center;
  border-radius: 24px;
  border: 10px solid rgba(255, 255, 255, 0.8);
  background: #ffffff;
  box-shadow: 0 18px 48px rgba(38, 75, 62, 0.12);
}

.secret-box {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
}

.secret-box code {
  overflow-wrap: anywhere;
  font-size: 14px;
  color: var(--animal-warm-color-soft);
}

.directory-link {
  color: var(--animal-primary-color);
  font-weight: 700;
  text-decoration: none;
}

@media (max-width: 960px) {
  .registration-grid {
    grid-template-columns: 1fr;
  }

  .qr-image {
    width: min(100%, 280px);
  }
}
</style>
