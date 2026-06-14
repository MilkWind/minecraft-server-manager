<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { Button, Card, Loading, Icon, Divider, Collapse, Typewriter, Tooltip } from 'animal-island-vue';
import { RouterLink } from 'vue-router';
import AppShell from '@/components/AppShell.vue';
import CreateServerModal from '@/components/CreateServerModal.vue';
import LoginModal from '@/components/LoginModal.vue';
import { useServerDirectory } from '@/composables/useServerDirectory';
import { useSession } from '@/composables/useSession';
import type { CreateManagedServerRequest } from '@/types/api';

const directory = useServerDirectory();
const sessionState = useSession();
const showCreate = ref(false);
const showLogin = ref(false);
const errorMessage = ref('');

const hasManagerAccess = computed(() => sessionState.isAuthenticated.value);

function statusColor(status: string): 'app-green' | 'app-red' | 'app-yellow' | 'brown' | 'default' {
  const s = status.toLowerCase();
  if (s === 'running') return 'app-green';
  if (s === 'stopped') return 'app-red';
  if (s === 'starting' || s === 'restarting') return 'app-yellow';
  if (s === 'error') return 'brown';
  return 'default';
}

function statusLabel(status: string): string {
  const s = status.toLowerCase();
  if (s === 'running') return '运行中';
  if (s === 'stopped') return '已停止';
  if (s === 'starting') return '启动中';
  if (s === 'restarting') return '重启中';
  if (s === 'error') return '异常';
  return status;
}

onMounted(async () => {
  await sessionState.loadCurrentSession();
  if (hasManagerAccess.value) {
    await directory.loadServers();
  } else {
    directory.clearServers();
  }
});

watch(hasManagerAccess, async (isAuthenticated, wasAuthenticated) => {
  if (isAuthenticated && !wasAuthenticated) {
    await directory.loadServers();
  }

  if (!isAuthenticated && wasAuthenticated) {
    directory.clearServers();
  }
});

function openCreateFlow() {
  if (!hasManagerAccess.value) {
    return;
  }

  showCreate.value = true;
}

function openLogin() {
  errorMessage.value = '';
  showLogin.value = true;
}

function onLoginSuccess() {
  showLogin.value = false;
}

async function createServer(payload: CreateManagedServerRequest) {
  try {
    errorMessage.value = '';
    sessionState.selectServer(payload.serverId);
    await directory.createManagedServer(payload);
    showCreate.value = false;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '创建受管服务器失败。';
  }
}
</script>

<template>
  <AppShell
    icon="icon-helicopter"
    title="服务器目录"
    subtitle="管理员通过公开专属注册链接绑定 2FA 后，可在此输入动态码登录。访客可以直接打开任意访客路线。"
  >
    <template #header-actions>
      <Button v-if="hasManagerAccess" type="primary" size="large" @click="openCreateFlow">创建受管服务器</Button>
    </template>

    <p v-if="errorMessage" class="error-banner">{{ errorMessage }}</p>
    <Loading :active="directory.loading.value" />

    <Collapse
      v-if="!hasManagerAccess"
      question="管理员登录"
      :default-expanded="true"
      class="login-collapse"
    >
      <div class="auth-card">
        <h3>管理员登录</h3>
        <p>
          使用已绑定管理员账号的验证器动态码登录。登录后即可选择要管理的服务器。
        </p>
        <div class="auth-actions">
          <Button type="primary" @click="openLogin">输入动态码登录</Button>
        </div>
      </div>
    </Collapse>

    <Divider v-if="hasManagerAccess" type="wave-yellow" class="section-divider" />

    <section v-if="hasManagerAccess && directory.servers.value.length > 0" class="server-grid">
      <Card
        v-for="server in directory.servers.value"
        :key="server.serverId"
        :color="statusColor(server.status)"
        class="server-card"
      >
        <div class="card-header">
          <p class="card-status">{{ statusLabel(server.status) }}</p>
        </div>
        <h3>
          <Icon name="icon-map" :size="18" class="card-icon" />
          {{ server.displayName }}
        </h3>
        <p class="card-address">
          <Tooltip :title="server.publicAddress" placement="top">
            <span>地址：{{ server.publicAddress }}</span>
          </Tooltip>
        </p>
        <p><Icon name="icon-design" :size="16" class="card-icon" />版本：{{ server.gameVersion }}</p>
        <p><Icon name="icon-chat" :size="16" class="card-icon" />在线玩家：{{ server.onlinePlayerCount }}</p>
        <div class="card-actions">
          <RouterLink :to="`/servers/${server.serverId}/visitor`" custom v-slot="{ navigate }">
            <Button type="default" @click="navigate">访客视图</Button>
          </RouterLink>
          <RouterLink :to="`/servers/${server.serverId}/manager`" custom v-slot="{ navigate }">
            <Button type="primary" @click="navigate">管理视图</Button>
          </RouterLink>
        </div>
      </Card>
    </section>

    <Card v-else-if="hasManagerAccess" class="empty-state-card">
      <Typewriter
        text="还没有受管服务器，点击上方按钮创建第一个吧。"
        :auto-play="true"
        :speed="40"
        class="empty-typewriter"
      />
    </Card>

    <CreateServerModal
      v-model="showCreate"
      :busy="directory.creating.value"
      @close="showCreate = false"
      @submit="createServer"
    />

    <LoginModal
      v-model:open="showLogin"
      @close="showLogin = false"
      @success="onLoginSuccess"
    />
  </AppShell>
</template>

<style scoped>
.error-banner {
  margin: 0 0 18px;
  border-radius: var(--animal-border-radius-base);
  padding: 14px 16px;
  background: rgba(224, 90, 90, 0.14);
  color: var(--animal-error-color);
  font-weight: 700;
}

.login-collapse {
  margin-bottom: 0;
}

.section-divider {
  margin: 18px 0;
}

.server-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 18px;
}

.auth-card,
.server-card,
.empty-state-card {
  display: grid;
  gap: 12px;
}

.auth-card h3,
.auth-card p,
.server-card h3,
.server-card p {
  margin: 0;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-status {
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  margin: 0;
}

.card-icon {
  vertical-align: middle;
  margin-right: 4px;
}

.card-address span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 220px;
  display: inline-block;
  vertical-align: bottom;
}

.card-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.auth-actions {
  display: flex;
  justify-content: flex-end;
}

.empty-state-card {
  text-align: center;
  padding: 32px;
}

.empty-typewriter {
  font-size: 16px;
  color: var(--animal-text-color-secondary);
}
</style>
