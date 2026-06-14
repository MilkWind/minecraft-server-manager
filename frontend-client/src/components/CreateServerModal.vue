<script setup lang="ts">
import { reactive } from 'vue';
import { Button, Input, Modal, Tooltip } from 'animal-island-vue';
import type { CreateManagedServerRequest } from '@/types/api';

defineProps<{
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
  <Modal
    :open="open ?? modelValue ?? false"
    title="创建受管服务器"
    :mask-closable="true"
    :show-footer="false"
    :typewriter="true"
    width="760"
    @close="close"
    @update:open="emit('update:modelValue', $event)"
  >
    <p class="modal-copy">填写服务器目录和基础信息后，将其纳入当前管理系统。</p>

    <div class="form-grid">
      <Tooltip title="服务器唯一标识，创建后不可更改，建议使用英文和短横线" placement="top">
        <Input v-model="form.serverId" placeholder="服务器 ID" />
      </Tooltip>
      <Input v-model="form.displayName" placeholder="显示名称" />
      <Tooltip title="服务器文件所在宿主机的绝对路径" placement="top">
        <Input v-model="form.rootDirectory" placeholder="服务器根目录" />
      </Tooltip>
      <Input v-model="form.publicAddress" placeholder="公网地址" />
      <Input v-model="form.gameVersion" placeholder="游戏版本" />
      <Input v-model="form.jvmArguments" class="wide-input" placeholder="JVM 参数，例如 -Xms2G -Xmx4G" />
    </div>

    <footer class="button-row">
      <Button type="default" @click="close">取消</Button>
      <Button type="primary" :loading="busy" @click="submit">提交创建</Button>
    </footer>
  </Modal>
</template>

<style scoped>
.modal-copy {
  margin: 0 0 18px;
  color: var(--animal-text-color-secondary);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.wide-input {
  grid-column: span 2;
}

.button-row {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
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
