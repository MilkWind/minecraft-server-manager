<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import AppShell from '@/components/AppShell.vue';
import LoginModal from '@/components/LoginModal.vue';
import ManagerControlPanel from '@/components/ManagerControlPanel.vue';
import ServerOverview from '@/components/ServerOverview.vue';
import { useServerSnapshot } from '@/composables/useServerSnapshot';
import { useSession } from '@/composables/useSession';
import type { UpdateServerConfigRequest } from '@/types/api';

const route = useRoute();
const serverId = computed(() => String(route.params.serverId ?? ''));
const clientType = computed(() => String(route.params.clientType ?? 'visitor'));
const managerView = computed(() => clientType.value === 'manager');

const loginOpen = ref(false);
const errorMessage = ref('');

const sessionState = useSession();
const snapshotState = useServerSnapshot(serverId, managerView);

onMounted(async () => {
  await sessionState.loadCurrentSession();
  if (managerView.value && !sessionState.isAuthenticated.value) {
    loginOpen.value = true;
  }
  snapshotState.startPolling(managerView.value ? 6000 : 8000);
});

onBeforeUnmount(() => {
  snapshotState.stopPolling();
});

watch([serverId, managerView], () => {
  snapshotState.stopPolling();
  snapshotState.startPolling(managerView.value ? 6000 : 8000);
});

function openLogin() {
  loginOpen.value = true;
}

function closeLogin() {
  loginOpen.value = false;
}

function onLoginSuccess() {
  loginOpen.value = false;
  void snapshotState.refreshAll();
}

async function guardAction(action: () => Promise<unknown>, fallbackMessage: string) {
  try {
    errorMessage.value = '';
    await action();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : fallbackMessage;
  }
}

async function onPower(action: 'start' | 'stop' | 'restart') {
  await guardAction(() => snapshotState.power(action), '电源操作失败。');
}

async function onConsole(command: string) {
  await guardAction(() => snapshotState.executeConsoleCommand(command), '命令发送失败。');
}

async function onPlayerAction(payload: { action: 'op' | 'deop' | 'ban'; playerName: string; reason?: string }) {
  await guardAction(
    () =>
      snapshotState.runPlayerAction(payload.action, {
        playerName: payload.playerName,
        reason: payload.reason,
      }),
    '玩家操作失败。',
  );
}

async function onSendMessage(payload: { message: string; targetPlayer?: string }) {
  await guardAction(() => snapshotState.sendMessage(payload), '消息发送失败。');
}

async function onAssetToggle(payload: { assetId: string; nextEnabled: boolean }) {
  await guardAction(
    () =>
      payload.nextEnabled
        ? snapshotState.resumeAsset({ assetId: payload.assetId })
        : snapshotState.suspendAsset({ assetId: payload.assetId }),
    '资源切换失败。',
  );
}

async function onUpdateConfig(payload: UpdateServerConfigRequest) {
  await guardAction(() => snapshotState.updateServerConfig(payload), '保存服务器配置失败。');
}

async function onCreateCommand(payload: { displayName: string; commandText: string; description: string }) {
  await guardAction(() => snapshotState.createCustomCommand(payload), '创建自定义命令失败。');
}

async function onUpdateCommand(payload: {
  commandId: string;
  displayName: string;
  commandText: string;
  description: string;
}) {
  await guardAction(
    () =>
      snapshotState.updateCustomCommand(payload.commandId, {
        displayName: payload.displayName,
        commandText: payload.commandText,
        description: payload.description,
      }),
    '更新自定义命令失败。',
  );
}

async function onDeleteCommand(commandId: string) {
  await guardAction(() => snapshotState.deleteCustomCommand(commandId), '删除自定义命令失败。');
}

async function onExecuteCommand(commandText: string) {
  await guardAction(() => snapshotState.executeConsoleCommand(commandText), '执行自定义命令失败。');
}

async function logout() {
  await sessionState.logout();
  loginOpen.value = true;
}
</script>

<template>
  <AppShell
    :title="managerView ? '服务器管理台' : '服务器信息面板'"
    :subtitle="managerView ? '查看完整日志、控制服务端进程并执行管理操作。' : '查看服务器状态、玩家与公开聊天信息。'"
    :manager-mode="managerView"
  >
    <template #header-actions>
      <div class="header-actions">
        <button v-if="managerView && !sessionState.isAuthenticated.value" class="header-button" @click="openLogin">
          管理员登录
        </button>
        <button v-if="managerView && sessionState.isAuthenticated.value" class="header-button" @click="logout">
          退出登录
        </button>
      </div>
    </template>

    <p v-if="errorMessage" class="error-banner">{{ errorMessage }}</p>

    <ServerOverview :snapshot="snapshotState.snapshot.value" :manager-view="managerView" />

    <ManagerControlPanel
      v-if="managerView && sessionState.isAuthenticated.value"
      :snapshot="snapshotState.snapshot.value"
      :logs="snapshotState.logs.value"
      :busy="snapshotState.busy.value"
      @power="onPower"
      @console="onConsole"
      @player-action="onPlayerAction"
      @send-message="onSendMessage"
      @asset-toggle="onAssetToggle"
      @update-config="onUpdateConfig"
      @create-command="onCreateCommand"
      @update-command="onUpdateCommand"
      @delete-command="onDeleteCommand"
      @execute-command="onExecuteCommand"
    />

    <LoginModal
      v-if="managerView"
      v-model="loginOpen"
      :server-id="serverId"
      @close="closeLogin"
      @success="onLoginSuccess"
    />
  </AppShell>
</template>

<style scoped>
.header-actions {
  display: flex;
  gap: 12px;
}

.header-button {
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
</style>
