<script setup lang="ts">
import { reactive } from 'vue';
import type { CreateManagedServerRequest } from '@/types/api';

const props = defineProps<{
  open?: boolean;
  modelValue?: boolean;
  busy?: boolean;
}>();

const emit = defineEmits<{
  close: [];
  'update:modelValue': [value: boolean];
  submit: [payload: CreateManagedServerRequest];
}>();

const form = reactive<CreateManagedServerRequest>({
  serverId: '',
  displayName: '',
  rootDirectory: '',
  jvmArguments: '',
  publicAddress: '',
  gameVersion: '',
});

function close() {
  emit('close');
  emit('update:modelValue', false);
}

function submit() {
  emit('submit', { ...form });
}
</script>

<template>
  <div v-if="open ?? modelValue" class="modal-backdrop" @click.self="close">
    <section class="modal-card">
      <header class="modal-header">
        <p class="eyebrow">Create Server</p>
        <h2>创建受管服务器</h2>
        <p>填写服务器目录与基础元数据后，将其纳入当前管理系统。</p>
      </header>

      <div class="form-grid">
        <input v-model="form.serverId" placeholder="server id" />
        <input v-model="form.displayName" placeholder="显示名称" />
        <input v-model="form.rootDirectory" placeholder="服务器根目录" />
        <input v-model="form.publicAddress" placeholder="公网地址" />
        <input v-model="form.gameVersion" placeholder="游戏版本" />
        <input v-model="form.jvmArguments" class="wide-input" placeholder="JVM 参数，例如 -Xms2G -Xmx4G" />
      </div>

      <footer class="button-row">
        <button class="ghost-button" type="button" @click="close">取消</button>
        <button class="primary-button" type="button" :disabled="busy" @click="submit">提交创建</button>
      </footer>
    </section>
  </div>
</template>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(31, 43, 32, 0.45);
  backdrop-filter: blur(10px);
}

.modal-card {
  width: min(760px, 100%);
  border: 2px solid #5b8f5a;
  border-radius: 28px;
  padding: 28px;
  background: linear-gradient(145deg, #fff8df, #eaf7d7);
  box-shadow: 0 24px 80px rgba(40, 69, 38, 0.28);
}

.modal-header {
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

.modal-header h2,
.modal-header p {
  margin: 0;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.wide-input {
  grid-column: span 2;
}

input {
  width: 100%;
  box-sizing: border-box;
  border: 2px solid rgba(91, 143, 90, 0.38);
  border-radius: 16px;
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.74);
  color: #263824;
  font: inherit;
}

.button-row {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
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

@media (max-width: 760px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .wide-input {
    grid-column: span 1;
  }
}
</style>
