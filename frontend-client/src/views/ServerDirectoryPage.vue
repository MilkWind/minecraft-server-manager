<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import AppShell from '@/components/AppShell.vue';
import CreateServerModal from '@/components/CreateServerModal.vue';
import { useServerDirectory } from '@/composables/useServerDirectory';
import type { CreateManagedServerRequest } from '@/types/api';

const directory = useServerDirectory();
const showCreate = ref(false);
const errorMessage = ref('');

onMounted(() => {
  void directory.loadServers();
});

async function createServer(payload: CreateManagedServerRequest) {
  try {
    errorMessage.value = '';
    await directory.createManagedServer(payload);
    showCreate.value = false;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '创建服务器失败。';
  }
}
</script>

<template>
  <AppShell
    title="服务器目录"
    subtitle="选择公开查看入口，或为受管服务器建立新的目录记录。"
  >
    <template #header-actions>
      <button class="create-button" @click="showCreate = true">创建受管服务器</button>
    </template>

    <p v-if="errorMessage" class="error-banner">{{ errorMessage }}</p>

    <section class="server-grid">
      <article v-for="server in directory.servers.value" :key="server.serverId" class="server-card">
        <p class="card-status">{{ server.status }}</p>
        <h3>{{ server.displayName }}</h3>
        <p>{{ server.publicAddress }}</p>
        <p>版本：{{ server.gameVersion }}</p>
        <p>在线人数：{{ server.onlinePlayerCount }}</p>
        <div class="card-actions">
          <RouterLink :to="`/servers/${server.serverId}/visitor`">访客视图</RouterLink>
          <RouterLink :to="`/servers/${server.serverId}/manager`">管理视图</RouterLink>
        </div>
      </article>
    </section>

    <CreateServerModal
      v-model="showCreate"
      :busy="directory.creating.value"
      @close="showCreate = false"
      @submit="createServer"
    />
  </AppShell>
</template>

<style scoped>
.create-button {
  border: 0;
  border-radius: 999px;
  padding: 10px 18px;
  background: #5b8f5a;
  color: #fffdf3;
  font: inherit;
  font-weight: 800;
  cursor: pointer;
}

.error-banner {
  margin: 0 0 18px;
  border-radius: 18px;
  padding: 14px 16px;
  background: rgba(185, 72, 53, 0.14);
  color: #9a3326;
}

.server-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 18px;
}

.server-card {
  display: grid;
  gap: 12px;
  border: 2px solid rgba(91, 143, 90, 0.28);
  border-radius: 28px;
  padding: 20px;
  background: rgba(255, 252, 243, 0.84);
}

.server-card h3,
.server-card p {
  margin: 0;
}

.card-status {
  color: #5b8f5a;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.card-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.card-actions a {
  border-radius: 999px;
  padding: 10px 14px;
  background: rgba(91, 143, 90, 0.14);
  color: #3f683f;
  font-weight: 800;
  text-decoration: none;
}
</style>
