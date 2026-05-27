<script setup lang="ts">
import { computed, reactive } from 'vue'
import { Button, Input, Modal } from 'animal-island-vue'
import type { CreateManagedServerRequest } from '../types/api'

const props = defineProps<{
  open: boolean
  loading: boolean
  error: string
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  submit: [payload: CreateManagedServerRequest]
}>()

const form = reactive<CreateManagedServerRequest>({
  serverId: '',
  displayName: '',
  rootDirectory: '',
  jvmArguments: '-Xms2G -Xmx4G',
  publicAddress: '',
  gameVersion: '1.21.1',
})

const canSubmit = computed(() =>
  form.serverId.trim()
  && form.displayName.trim()
  && form.rootDirectory.trim()
  && form.jvmArguments.trim()
  && form.publicAddress.trim()
  && form.gameVersion.trim(),
)

function handleSubmit() {
  if (!canSubmit.value) {
    return
  }

  emit('submit', {
    serverId: form.serverId.trim(),
    displayName: form.displayName.trim(),
    rootDirectory: form.rootDirectory.trim(),
    jvmArguments: form.jvmArguments.trim(),
    publicAddress: form.publicAddress.trim(),
    gameVersion: form.gameVersion.trim(),
  })
}
</script>

<template>
  <Modal
    :open="open"
    title="新增受管服务器"
    :typewriter="false"
    @update:open="emit('update:open', $event)"
  >
    <div class="login-form">
      <Input v-model="form.serverId" placeholder="服务器 ID，例如 survival-2" allow-clear />
      <Input v-model="form.displayName" placeholder="显示名称" allow-clear />
      <Input v-model="form.rootDirectory" placeholder="服务器根目录" allow-clear />
      <Input v-model="form.jvmArguments" placeholder="JVM 参数" allow-clear />
      <Input v-model="form.publicAddress" placeholder="公开地址，例如 play.example.com:25565" allow-clear />
      <Input v-model="form.gameVersion" placeholder="游戏版本" allow-clear />
      <p class="status-note">{{ error || '服务端会在根目录中自动发现首个 .jar 文件。' }}</p>
    </div>
    <template #footer>
      <div class="modal-actions">
        <Button @click="emit('update:open', false)">取消</Button>
        <Button type="primary" :loading="loading" :disabled="!canSubmit" @click="handleSubmit">创建</Button>
      </div>
    </template>
  </Modal>
</template>
