<script setup lang="ts">
import { computed, ref } from 'vue'
import { Button, Card, Loading } from 'animal-island-vue'
import { useRoute, useRouter } from 'vue-router'
import AppShell from '../components/AppShell.vue'
import LoginModal from '../components/LoginModal.vue'
import ManagerControlPanel from '../components/ManagerControlPanel.vue'
import ServerOverview from '../components/ServerOverview.vue'
import { useServerDirectory } from '../composables/useServerDirectory'
import { useServerSnapshot } from '../composables/useServerSnapshot'
import { useSession } from '../composables/useSession'

const route = useRoute()
const router = useRouter()
const loginOpen = ref(false)
const loginLoading = ref(false)
const loginError = ref('')

const { servers } = useServerDirectory()
const { session, isAuthenticated, login, logout, refreshSession } = useSession()

const serverId = computed(() => String(route.params.serverId))
const clientType = computed(() => route.params.clientType === 'manager' ? 'manager' : 'visitor')

const {
  snapshot,
  logs,
  loading,
  actionLoading,
  error,
  actionMessage,
  canManage,
  runPowerAction,
  executeCommand,
  updateServerConfig,
  createCustomCommand,
  updateCustomCommand,
  deleteCustomCommand,
  opPlayer,
  deopPlayer,
  banPlayer,
  sendMessage,
  suspendAsset,
  resumeAsset,
} = useServerSnapshot(() => serverId.value, () => clientType.value === 'manager')

void refreshSession()

async function handleLogin(payload: { username: string; password: string; totpCode: string }) {
  loginLoading.value = true
  loginError.value = ''

  try {
    await login(payload)
    loginOpen.value = false
  } catch (submitError) {
    loginError.value = submitError instanceof Error ? submitError.message : '登录失败'
  } finally {
    loginLoading.value = false
  }
}

async function handleLogout() {
  await logout()
}

function navigate(server: string, type: 'visitor' | 'manager') {
  void router.push(`/servers/${server}/${type}`)
}

function openLogin() {
  loginError.value = ''
  loginOpen.value = true
}
</script>

<template>
  <AppShell
    :title="clientType === 'manager' ? '管理工作台' : '访客面板'"
    :subtitle="clientType === 'manager'
      ? '管理视图已接入鉴权、日志、命令、自定义命令管理和第一版真实进程控制。'
      : '公开视图只展示安全可公开的数据。'"
    :servers="servers"
    :current-server-id="serverId"
    :current-client-type="clientType"
    @navigate="navigate"
  >
    <section class="content-stack">
      <Loading :active="loading" />

      <Card v-if="clientType === 'manager'" class="panel-card">
        <div class="workspace-header">
          <div>
            <h2>管理认证</h2>
            <p>{{ isAuthenticated ? `当前登录：${session?.displayName}` : '需要先完成 2FA 登录后才能访问管理接口。' }}</p>
          </div>
          <div class="inline-actions">
            <Button v-if="!isAuthenticated" type="primary" @click="openLogin">登录</Button>
            <Button v-else @click="handleLogout">退出登录</Button>
          </div>
        </div>
      </Card>

      <Card v-if="error" color="app-red" class="panel-card">
        <p>{{ error }}</p>
      </Card>

      <template v-if="snapshot">
        <ServerOverview :snapshot="snapshot" />

        <ManagerControlPanel
          v-if="clientType === 'manager' && canManage"
          :snapshot="snapshot"
          :logs="logs"
          :action-loading="actionLoading"
          :action-message="actionMessage"
          @power="runPowerAction"
          @execute="executeCommand"
          @update-config="updateServerConfig"
          @create-command="createCustomCommand"
          @update-command="updateCustomCommand"
          @delete-command="deleteCustomCommand"
          @op-player="opPlayer"
          @deop-player="deopPlayer"
          @ban-player="banPlayer"
          @send-message="sendMessage"
          @suspend-asset="suspendAsset"
          @resume-asset="resumeAsset"
        />

        <Card v-else-if="clientType === 'manager'" class="panel-card">
          <h3>等待登录</h3>
          <p>当前页面已经切到管理路由，但未持有有效 token，后端不会返回管理数据。</p>
          <div class="inline-actions top-space">
            <Button type="primary" @click="openLogin">开始登录</Button>
          </div>
        </Card>
      </template>
    </section>

    <LoginModal
      v-model:open="loginOpen"
      :loading="loginLoading"
      :error="loginError"
      @submit="handleLogin"
    />
  </AppShell>
</template>
