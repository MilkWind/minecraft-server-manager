<script setup lang="ts">
import { computed, ref } from 'vue';
import { Button, Card, Input, Title, Divider, Typewriter, Icon } from 'animal-island-vue';
import { RouterLink, useRoute } from 'vue-router';
import AppShell from '@/components/AppShell.vue';
import { apiRequest } from '@/lib/api';
import type {
  ManagerRegistrationConfirmRequest,
  ManagerRegistrationQrPayload,
  ManagerRegistrationResult,
} from '@/types/api';

const route = useRoute();
const username = computed(() => String(route.params.username ?? '').trim());

const totpCode = ref('');
const qrPayload = ref<ManagerRegistrationQrPayload | null>(null);
const successMessage = ref('');
const errorMessage = ref('');
const generatingQr = ref(false);
const confirmingRegistration = ref(false);

const canGenerateQr = computed(() => Boolean(username.value));
const canConfirm = computed(() => Boolean(qrPayload.value) && /^\d{6}$/.test(totpCode.value.trim()));

async function generateQr() {
  if (!canGenerateQr.value) {
    errorMessage.value = '注册链接缺少管理员用户名。';
    return;
  }

  generatingQr.value = true;
  errorMessage.value = '';
  successMessage.value = '';

  try {
    qrPayload.value = await apiRequest<ManagerRegistrationQrPayload>(
      `/api/public/manager-registration/${encodeURIComponent(username.value)}/qr`,
      { method: 'POST' },
    );
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
    const response = await apiRequest<ManagerRegistrationResult>(
      `/api/public/manager-registration/${encodeURIComponent(username.value)}/confirm`,
      {
        method: 'POST',
        body: JSON.stringify({
          totpCode: totpCode.value.trim(),
        } satisfies ManagerRegistrationConfirmRequest),
      },
    );
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
    icon="icon-miles"
    title="管理员 2FA 注册"
    subtitle="此公开注册链接绑定了一个后端配置中的管理员用户名。完成绑定后，登录只需要输入验证器动态码。"
    :manager-mode="true"
  >
    <section class="registration-grid">
      <Card class="panel">
        <div class="panel-header">
          <p class="eyebrow">步骤 1</p>
          <Title size="small" color="app-teal">生成验证器 QR 码</Title>
        </div>

        <p class="panel-copy">
          当前注册链接对应管理员账号：<strong>{{ username || '未识别' }}</strong>。
          后端会检查 <code>application.yaml</code> 中该账号的注册链接开关。
        </p>

        <p v-if="errorMessage" class="error-banner">{{ errorMessage }}</p>
        <div v-if="successMessage" class="success-typewriter">
          <Typewriter
            :text="successMessage"
            :auto-play="true"
            :speed="30"
          />
        </div>

        <div class="panel-actions">
          <Button type="default" :disabled="!qrPayload" @click="resetRegistration">重置</Button>
          <Button type="primary" :disabled="!canGenerateQr" :loading="generatingQr" @click="generateQr">
            {{ generatingQr ? '正在生成 QR 码...' : '生成 QR 码' }}
          </Button>
        </div>
      </Card>

      <Divider type="line-teal" class="panel-divider" />

      <Card class="panel qr-panel">
        <div class="panel-header">
          <p class="eyebrow">步骤 2</p>
          <Title size="small" color="app-teal">绑定并确认验证器</Title>
        </div>

        <template v-if="qrPayload">
          <img class="qr-image" :src="qrPayload.qrCodeImage" alt="管理员验证器 QR 码" />

          <div class="secret-box">
            <p>手动设置密钥</p>
            <code>{{ qrPayload.manualEntryKey }}</code>
          </div>

          <p class="panel-copy">
            请使用验证器应用扫描 QR 码，然后输入首次生成的 6 位动态码以激活
            <strong>{{ qrPayload.displayName }}</strong>。
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
            点击生成 QR 码后，管理员专用验证器绑定信息会显示在这里。无需设置密码。
          </p>
        </template>

        <RouterLink to="/servers" class="directory-link">
          <Icon name="icon-helicopter" :size="14" />
          返回服务器目录
        </RouterLink>
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

.error-banner {
  margin: 0;
  border-radius: var(--animal-border-radius-base);
  padding: 12px 14px;
  font-weight: 700;
  background: rgba(224, 90, 90, 0.14);
  color: var(--animal-error-color);
}

.success-typewriter {
  color: #2f7c4b;
  font-weight: 700;
  font-size: 15px;
}

.panel-divider {
  display: none;
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
  display: flex;
  align-items: center;
  gap: 6px;
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

  .panel-divider {
    display: block;
  }
}
</style>
