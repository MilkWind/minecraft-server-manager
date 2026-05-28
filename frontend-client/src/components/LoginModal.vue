<script setup lang="ts">
import { computed, ref } from 'vue';
import { useSession } from '@/composables/useSession';

const props = defineProps<{
  serverId: string;
  open?: boolean;
  modelValue?: boolean;
}>();

const emit = defineEmits<{
  close: [];
  success: [];
  'update:modelValue': [value: boolean];
}>();

const { login, loading } = useSession();

const username = ref('admin');
const password = ref('');
const totpCode = ref('');
const errorMessage = ref('');

const isOpen = computed(() => props.open ?? props.modelValue ?? false);

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
    emit('update:modelValue', false);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败，请检查账号和验证码。';
  }
}

function close() {
  emit('close');
  emit('update:modelValue', false);
}
</script>

<template>
  <div v-if="isOpen" class="login-backdrop" @click.self="close">
    <section class="login-card" aria-label="管理员登录">
      <header class="login-header">
        <p class="eyebrow">Manager Access</p>
        <h2>管理员登录</h2>
        <p>请输入账号、密码和动态验证码后继续管理服务器。</p>
      </header>

      <form class="login-form" @submit.prevent="submit">
        <label>
          <span>账号</span>
          <input v-model="username" autocomplete="username" placeholder="admin" />
        </label>

        <label>
          <span>密码</span>
          <input v-model="password" autocomplete="current-password" type="password" placeholder="请输入密码" />
        </label>

        <label>
          <span>动态验证码</span>
          <input
            v-model="totpCode"
            autocomplete="one-time-code"
            inputmode="numeric"
            maxlength="6"
            placeholder="6 位验证码"
          />
        </label>

        <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

        <footer class="login-actions">
          <button class="ghost-button" type="button" @click="close">取消</button>
          <button class="primary-button" type="submit" :disabled="loading">登录</button>
        </footer>
      </form>
    </section>
  </div>
</template>

<style scoped>
.login-backdrop {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(31, 43, 32, 0.45);
  backdrop-filter: blur(10px);
}

.login-card {
  width: min(420px, 100%);
  border: 2px solid #5b8f5a;
  border-radius: 28px;
  padding: 28px;
  background: linear-gradient(145deg, #fff8df, #eaf7d7);
  box-shadow: 0 24px 80px rgba(40, 69, 38, 0.28);
}

.login-header {
  display: grid;
  gap: 8px;
  margin-bottom: 22px;
}

.eyebrow {
  margin: 0;
  color: #5b8f5a;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.login-header h2,
.login-header p {
  margin: 0;
}

.login-header h2 {
  color: #314c32;
  font-size: 26px;
}

.login-header p {
  color: #6d715d;
  line-height: 1.6;
}

.login-form {
  display: grid;
  gap: 16px;
}

.login-form label {
  display: grid;
  gap: 8px;
  color: #41533e;
  font-weight: 700;
}

.login-form input {
  width: 100%;
  box-sizing: border-box;
  border: 2px solid rgba(91, 143, 90, 0.38);
  border-radius: 16px;
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.74);
  color: #263824;
  font: inherit;
  outline: none;
}

.login-form input:focus {
  border-color: #5b8f5a;
}

.error-message {
  margin: 0;
  border-radius: 14px;
  padding: 10px 12px;
  background: rgba(190, 68, 48, 0.12);
  color: #9a3326;
}

.login-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}

.ghost-button,
.primary-button {
  border: 0;
  border-radius: 999px;
  padding: 10px 18px;
  font: inherit;
  font-weight: 800;
  cursor: pointer;
}

.ghost-button {
  background: rgba(91, 143, 90, 0.12);
  color: #3f683f;
}

.primary-button {
  background: #5b8f5a;
  color: #fffdf1;
}

.primary-button:disabled {
  cursor: wait;
  opacity: 0.65;
}
</style>
