<script setup lang="ts">
import { ref } from 'vue';
import { Button, Input, Modal, Tooltip } from 'animal-island-vue';
import { useSession } from '@/composables/useSession';

const props = defineProps<{
  open: boolean;
}>();

const emit = defineEmits<{
  close: [];
  success: [];
  'update:open': [value: boolean];
}>();

const { login, loading } = useSession();

const totpCode = ref('');
const errorMessage = ref('');

async function submit() {
  errorMessage.value = '';

  try {
    await login({
      totpCode: totpCode.value,
    });
    totpCode.value = '';
    emit('success');
    emit('update:open', false);
  } catch (error) {
    errorMessage.value = error instanceof Error
      ? error.message
      : '登录失败，请检查 TOTP 动态码。';
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
      输入已绑定管理员账号的 6 位验证器动态码即可登录。
    </p>

    <form class="login-form" @submit.prevent="submit">
      <label>
        <Tooltip title="打开已绑定的验证器应用，输入当前显示的 6 位数字" placement="top">
          <span>TOTP 动态码</span>
        </Tooltip>
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
