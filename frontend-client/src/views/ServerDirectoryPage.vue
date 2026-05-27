<script setup lang="ts">
import { computed, ref } from 'vue'
import { Button, Card, Loading } from 'animal-island-vue'
import { useRouter } from 'vue-router'
import AppShell from '../components/AppShell.vue'
import CreateServerModal from '../components/CreateServerModal.vue'
import { useServerDirectory } from '../composables/useServerDirectory'
import { api } from '../lib/api'
import { useSession } from '../composables/useSession'
import type { CreateManagedServerRequest } from '../types/api'

const router = useRouter()
const { servers, loading, error, loadServers } = useServerDirectory()
const { session, isAuthenticated, refreshSession } = useSession()

const createOpen = ref(false)
const createLoading = ref(false)
const createError = ref('')

const shellServers = computed(() => servers.value)

void refreshSession()

function openServer(serverId: string, clientType: 'visitor' | 'manager') {
  void router.push(`/servers/${serverId}/${clientType}`)
}

async function handleCreateServer(payload: CreateManagedServerRequest) {
  if (session.value == null) {
    createError.value = '请先登录管理账号'
    return
  }

  createLoading.value = true
  createError.value = ''

  try {
    await api.createManagedServer(payload, session.value.token)
    createOpen.value = false
    await loadServers()
  } catch (submitError) {
    createError.value = submitError instanceof Error ? submitError.message : '新增服务器失败'
  } finally {
    createLoading.value = false
  }
}
</script>

<template>
  <AppShell
    title="轻量管理多台 Minecraft 服务器"
    subtitle="当前实现已经接入真实路由、认证入口、SQLite 持久层、自定义命令管理，以及第一版服务器进程控制入口。"
    :servers="shellServers"
    current-client-type="visitor"
    @navigate="openServer"
  >
    <section class="content-stack">
      <Loading :active="loading" />
      <Card class="panel-card">
        <h2>服务器入口</h2>
        <p v-if="error" class="status-note">{{ error }}</p>
        <div class="directory-grid">
          <Card
            v-for="server in servers"
            :key="server.serverId"
            color="default"
            class="directory-card"
          >
            <h3>{{ server.displayName }}</h3>
            <p>{{ server.publicAddress }}</p>
            <p>版本 {{ server.gameVersion }} · 在线 {{ server.onlinePlayerCount }} 人</p>
            <div class="inline-actions top-space">
              <Button type="primary" @click="openServer(server.serverId, 'visitor')">查看访客页</Button>
              <Button @click="openServer(server.serverId, 'manager')">进入管理页</Button>
            </div>
          </Card>
        </div>
        <div class="inline-actions top-space">
          <Button type="dashed" @click="loadServers">刷新列表</Button>
          <Button v-if="isAuthenticated" type="primary" @click="createOpen = true">新增受管服务器</Button>
        </div>
      </Card>
    </section>

    <CreateServerModal
      v-model:open="createOpen"
      :loading="createLoading"
      :error="createError"
      @submit="handleCreateServer"
    />
  </AppShell>
</template>
