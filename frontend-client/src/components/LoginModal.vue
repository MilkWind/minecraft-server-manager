<script setup lang="ts">
import { computed, reactive } from 'vue'
import { Button, Input, Modal } from 'animal-island-vue'

const props = defineProps<{
  open: boolean
  loading: boolean
  error: string
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  submit: [payload: { username: string; password: string; totpCode: string }]
}>()

const form = reactive({
  username: 'admin',
  password: 'admin123456',
  totpCode: '123456',
})

const canSubmit = computed(() =>
  form.username.trim() && form.password.trim() && form.totpCode.trim(),
)

function handleSubmit() {
  if (!canSubmit.value) {
    return
  }

  emit('submit', {
    username: form.username.trim(),
    password: form.password.trim(),
    totpCode: form.totpCode.trim(),
  })
}
</script>

<template>
  <Modal
    :open="props.open"
    title="管理员登录"
    :typewriter="false"
    @update:open="emit('update:open', $event)"
    @ok="handleSubmit"
  >
    <div class="login-form">
      <Input v-model="form.username" placeholder="用户名" allow-clear />
      <Input v-model="form.password" type="password" placeholder="密码" allow-clear />
      <Input v-model="form.totpCode" placeholder="6 位动态验证码" allow-clear />
      <p class="status-note">{{ props.error || '当前为引导账号：admin / admin123456 / 123456' }}</p>
    </div>
    <template #footer>
      <div class="modal-actions">
        <Button @click="emit('update:open', false)">取消</Button>
        <Button type="primary" :disabled="!canSubmit" :loading="props.loading" @click="handleSubmit">
          登录
        </Button>
      </div>
    </template>
  </Modal>
</template>
