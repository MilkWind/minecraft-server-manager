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
      : 'Sign-in failed. Check the manager username, password, and TOTP code.';
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
    title="Manager Sign In"
    :mask-closable="true"
    :show-footer="false"
    :typewriter="true"
    width="520"
    @close="close"
    @update:open="emit('update:open', $event)"
  >
    <p class="modal-copy">
      Sign in with the manager account you registered from the private manager-registration link.
    </p>

    <form class="login-form" @submit.prevent="submit">
      <label>
        <span>Username</span>
        <Input v-model="username" placeholder="manager username" />
      </label>

      <label>
        <span>Password</span>
        <Input v-model="password" type="password" placeholder="manager password" />
      </label>

      <label>
        <span>TOTP Code</span>
        <Input v-model="totpCode" :maxlength="6" placeholder="6-digit authenticator code" />
      </label>

      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

      <footer class="login-actions">
        <Button type="default" html-type="button" @click="close">Cancel</Button>
        <Button type="primary" html-type="submit" :loading="loading">
          {{ loading ? 'Signing in...' : 'Sign in' }}
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
