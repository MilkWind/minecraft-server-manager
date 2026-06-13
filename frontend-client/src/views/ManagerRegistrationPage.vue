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
    errorMessage.value = '请先完整填写管理员账号表单，再生成 QR 码。';
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
    errorMessage.value = error instanceof Error ? error.message : '生成管理员 QR 码失败。';
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
    errorMessage.value = error instanceof Error ? error.message : '确认管理员注册失败。';
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
    title="管理员注册"
    subtitle="此页面仅在通过私有管理员注册链接打开时可用。后端会在签发 QR 码前校验隐藏验证片段。"
    :manager-mode="true"
  >
    <section class="registration-grid">
      <Card class="panel">
        <div class="panel-header">
          <p class="eyebrow">步骤 1</p>
          <h2>创建管理员账号</h2>
        </div>

        <p class="panel-copy">
          请先提交管理员用户名、显示名称和密码。只有当此私有路线仍在 <code>application.yaml</code> 中启用时，
          后端才会创建专用 TOTP 密钥并返回 QR 码。
        </p>

        <div class="form-grid">
          <label>
            <span>显示名称</span>
            <Input v-model="displayName" placeholder="管理员显示名称" />
          </label>

          <label>
            <span>用户名</span>
            <Input v-model="username" placeholder="管理员用户名" />
          </label>

          <label>
            <span>密码</span>
            <Input v-model="password" type="password" placeholder="至少 8 个字符" />
          </label>

          <label>
            <span>确认密码</span>
            <Input v-model="confirmPassword" type="password" placeholder="再次输入密码" />
          </label>
        </div>

        <p v-if="!passwordsMatch && confirmPassword" class="warning-banner">两次输入的密码不一致。</p>
        <p v-if="errorMessage" class="error-banner">{{ errorMessage }}</p>
        <p v-if="successMessage" class="success-banner">{{ successMessage }}</p>

        <div class="panel-actions">
          <Button type="default" :disabled="!qrPayload" @click="resetRegistration">重置</Button>
          <Button type="primary" :disabled="!canGenerateQr" :loading="generatingQr" @click="generateQr">
            {{ generatingQr ? '正在生成 QR 码...' : '生成 QR 码' }}
          </Button>
        </div>
      </Card>

      <Card class="panel qr-panel">
        <div class="panel-header">
          <p class="eyebrow">步骤 2</p>
          <h2>绑定并确认验证器</h2>
        </div>

        <template v-if="qrPayload">
          <img class="qr-image" :src="qrPayload.qrCodeImage" alt="管理员验证器 QR 码" />

          <div class="secret-box">
            <p>手动设置密钥</p>
            <code>{{ qrPayload.manualEntryKey }}</code>
          </div>

          <p class="panel-copy">
            请使用验证器应用扫描 QR 码，然后在此输入首次生成的 6 位动态码以激活管理员账号。
          </p>

          <label class="confirm-label">
            <span>验证器动态码</span>
            <Input v-model="totpCode" :maxlength="6" placeholder="6 位动态码" />
          </label>

          <div class="panel-actions">
            <Button type="primary" :disabled="!canConfirm" :loading="confirmingRegistration" @click="confirmManagerRegistration">
              {{ confirmingRegistration ? '确认中...' : '确认注册' }}
            </Button>
          </div>
        </template>

        <template v-else>
          <p class="panel-copy">
            私有路线通过后端校验且账号信息被接受后，管理员专用 QR 码会显示在这里。
          </p>
        </template>

        <RouterLink to="/servers" class="directory-link">返回服务器目录</RouterLink>
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
