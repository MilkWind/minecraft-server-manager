<script setup lang="ts">
import { ref } from 'vue';
import { Button, Input, Modal } from 'animal-island-vue';
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

const username = ref('');
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
    errorMessage.value = error instanceof Error
      ? error.message
      : '登录失败，请检查管理员用户名、密码和 TOTP 动态码。';
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
    <p class="modal-copy">
      使用通过私有管理员注册链接注册的管理员账号登录。
    </p>

    <form class="login-form" @submit.prevent="submit">
      <label>
        <span>用户名</span>
        <Input v-model="username" placeholder="管理员用户名" />
      </label>

      <label>
        <span>密码</span>
        <Input v-model="password" type="password" placeholder="管理员密码" />
      </label>

      <label>
        <span>TOTP 动态码</span>
        <Input v-model="totpCode" :maxlength="6" placeholder="6 位验证器动态码" />
      </label>

      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

      <footer class="login-actions">
        <Button type="default" html-type="button" @click="close">取消</Button>
        <Button type="primary" html-type="submit" :loading="loading">
          {{ loading ? '登录中...' : '登录' }}
        </Button>
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
</style>
