<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { Button, Card, Input, Loading } from 'animal-island-vue';
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
const loginServerId = ref('MilkWind');
const errorMessage = ref('');

const hasManagerAccess = computed(() => sessionState.isAuthenticated.value);

onMounted(async () => {
  await sessionState.loadCurrentSession();
  loginServerId.value = sessionState.selectedServerId.value || 'MilkWind';
  if (hasManagerAccess.value) {
    await directory.loadServers();
  } else {
    directory.clearServers();
  }
});

watch(hasManagerAccess, async (isAuthenticated, wasAuthenticated) => {
  if (isAuthenticated && !wasAuthenticated) {
    loginServerId.value = sessionState.selectedServerId.value || loginServerId.value || 'MilkWind';
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
  if (!loginServerId.value.trim()) {
    errorMessage.value = 'Enter the target server ID before opening manager sign-in.';
    return;
  }

  errorMessage.value = '';
  showLogin.value = true;
}

function onLoginSuccess() {
  showLogin.value = false;
  loginServerId.value = sessionState.selectedServerId.value || loginServerId.value;
}

async function createServer(payload: CreateManagedServerRequest) {
  try {
    errorMessage.value = '';
    sessionState.selectServer(payload.serverId);
    await directory.createManagedServer(payload);
    showCreate.value = false;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Creating the managed server failed.';
  }
}
</script>

<template>
  <AppShell
    title="Server Directory"
    subtitle="Managers can sign in here after binding a private manager-registration link. Visitors can open any visitor route directly."
  >
    <template #header-actions>
      <Button v-if="hasManagerAccess" type="primary" size="large" @click="openCreateFlow">Create Managed Server</Button>
    </template>

    <p v-if="errorMessage" class="error-banner">{{ errorMessage }}</p>
    <Loading :active="directory.loading.value" />

    <section v-if="hasManagerAccess" class="server-grid">
      <Card v-for="server in directory.servers.value" :key="server.serverId" class="server-card">
        <p class="card-status">{{ server.status }}</p>
        <h3>{{ server.displayName }}</h3>
        <p>{{ server.publicAddress }}</p>
        <p>Version: {{ server.gameVersion }}</p>
        <p>Players Online: {{ server.onlinePlayerCount }}</p>
        <div class="card-actions">
          <RouterLink :to="`/servers/${server.serverId}/visitor`" custom v-slot="{ navigate }">
            <Button type="default" @click="navigate">Visitor View</Button>
          </RouterLink>
          <RouterLink :to="`/servers/${server.serverId}/manager`" custom v-slot="{ navigate }">
            <Button type="primary" @click="navigate">Manager View</Button>
          </RouterLink>
        </div>
      </Card>
    </section>

    <Card v-else class="auth-card">
      <h3>Manager Sign In</h3>
      <p>
        Use the username, password, and authenticator code from your private manager registration flow. Choose the server you
        want to manage after sign-in.
      </p>
      <Input v-model="loginServerId" placeholder="server ID, for example MilkWind" />
      <div class="auth-actions">
        <Button type="primary" :disabled="!loginServerId.trim()" @click="openLogin">Continue to Sign In</Button>
      </div>
    </Card>

    <CreateServerModal
      v-model="showCreate"
      :busy="directory.creating.value"
      @close="showCreate = false"
      @submit="createServer"
    />

    <LoginModal
      v-model:open="showLogin"
      :server-id="loginServerId.trim()"
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

.server-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 18px;
}

.auth-card,
.server-card {
  display: grid;
  gap: 12px;
}

.auth-card h3,
.auth-card p,
.server-card h3,
.server-card p {
  margin: 0;
}

.card-status {
  color: var(--animal-primary-color);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.12em;
  text-transform: uppercase;
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
</style>
