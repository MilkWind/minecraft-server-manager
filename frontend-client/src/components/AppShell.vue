<script setup lang="ts">
import { computed } from 'vue'
import { Button, Card, Divider, Icon } from 'animal-island-vue'
import type { PublicServerSummary } from '../types/api'

const props = defineProps<{
  title: string
  subtitle: string
  servers: PublicServerSummary[]
  currentServerId?: string
  currentClientType?: 'visitor' | 'manager'
}>()

const emit = defineEmits<{
  navigate: [serverId: string, clientType: 'visitor' | 'manager']
}>()

const activeServer = computed(() =>
  props.servers.find((server) => server.serverId === props.currentServerId) ?? null,
)
</script>

<template>
  <div class="app-shell">
    <header class="hero-panel">
      <div class="hero-copy">
        <Card type="title" color="app-yellow">Minecraft Server Manager</Card>
        <h1>{{ title }}</h1>
        <p>{{ subtitle }}</p>
      </div>
      <Card color="app-teal" class="hero-status">
        <div class="hero-status-row">
          <Icon name="icon-map" :size="28" />
          <div>
            <strong>{{ activeServer?.displayName ?? '选择服务器' }}</strong>
            <p>{{ activeServer?.publicAddress ?? '从左侧目录进入对应视图' }}</p>
          </div>
        </div>
        <div class="hero-status-meta">
          <span>当前视图：{{ currentClientType === 'manager' ? '管理端' : '访客端' }}</span>
          <span v-if="activeServer">在线 {{ activeServer.onlinePlayerCount }} 人</span>
        </div>
      </Card>
    </header>

    <Divider type="wave-yellow" />

    <div class="shell-body">
      <aside class="server-rail">
        <Card class="rail-card">
          <div class="rail-header">
            <Icon name="icon-critterpedia" :size="24" />
            <strong>服务器目录</strong>
          </div>
          <div class="server-link-list">
            <div v-for="server in servers" :key="server.serverId" class="server-link-card">
              <div class="server-link-copy">
                <strong>{{ server.displayName }}</strong>
                <p>{{ server.gameVersion }} · {{ server.status }}</p>
              </div>
              <div class="server-link-actions">
                <Button
                  size="small"
                  :type="currentServerId === server.serverId && currentClientType === 'visitor' ? 'primary' : 'default'"
                  @click="emit('navigate', server.serverId, 'visitor')"
                >
                  访客
                </Button>
                <Button
                  size="small"
                  :type="currentServerId === server.serverId && currentClientType === 'manager' ? 'primary' : 'default'"
                  @click="emit('navigate', server.serverId, 'manager')"
                >
                  管理
                </Button>
              </div>
            </div>
          </div>
        </Card>
      </aside>

      <main class="shell-main">
        <slot />
      </main>
    </div>
  </div>
</template>
