<script setup lang="ts">
import { ref } from 'vue';
import { Input, Modal } from 'animal-island-vue';
import { useSession } from '@/composables/useSession';

const props = defineProps<{
  serverId: string;
  open: boolean;
}>();

const emit = defineEmits<{
  close: [];
  success: [];
  'update:open': [value: boolean];
}>();

const { login, loading } = useSession();

const username = ref('admin');
const password = ref('');
const totpCode = ref('');
const errorMessage = ref('');

async function submit() {
  errorMessage.value = '';

  try {
    await login({
      username: username.value,
      password: password.value,
      totpCode: totpCode.value,
      serverId: props.serverId,
    });
    password.value = '';
    totpCode.value = '';
    emit('success');
    emit('update:open', false);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败，请检查账号、密码和验证码。';
  }
}

function close() {
  emit('close');
  emit('update:open', false);
}
</script>

<template>
  <Modal
    :open="props.open"
    title="管理员登录"
    :mask-closable="true"
    :show-footer="false"
    :typewriter="true"
    width="520"
    @close="close"
    @update:open="emit('update:open', $event)"
  >
    <p class="modal-copy">输入账号、密码和动态验证码后继续管理服务器。</p>

    <form class="login-form" @submit.prevent="submit">
      <label>
        <span>账号</span>
        <Input v-model="username" placeholder="admin" />
      </label>

      <label>
        <span>密码</span>
        <Input v-model="password" type="password" placeholder="请输入密码" />
      </label>

      <label>
        <span>动态验证码</span>
        <Input v-model="totpCode" :maxlength="6" placeholder="6 位验证码" />
      </label>

      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

      <footer class="login-actions">
        <button type="button" class="action-button action-button--secondary" @click="close">取消</button>
        <button type="submit" class="action-button action-button--primary" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </footer>
    </form>
  </Modal>
</template>

<style scoped>
.modal-copy {
  margin: 0 0 18px;
  color: var(--animal-text-color-secondary);
}

.login-form {
  display: grid;
  gap: 16px;
}

.login-form label {
  display: grid;
  gap: 8px;
  color: var(--animal-warm-color-soft);
  font-weight: 700;
}

.error-message {
  margin: 0;
  border-radius: var(--animal-border-radius-base);
  padding: 10px 12px;
  background: rgba(224, 90, 90, 0.12);
  color: var(--animal-error-color);
}

.login-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}

.action-button {
  border: 0;
  border-radius: 999px;
  padding: 10px 18px;
  min-width: 96px;
  font: inherit;
  font-weight: 800;
  cursor: pointer;
}

.action-button--secondary {
  background: rgba(91, 143, 90, 0.14);
  color: #3f683f;
}

.action-button--primary {
  background: var(--animal-primary-color);
  color: #fff;
}

.action-button:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}
</style>
