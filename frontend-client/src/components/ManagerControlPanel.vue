<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { Button, Card, Input, Switch, Table, Tabs, type TabItem } from 'animal-island-vue';
import type { CustomCommand, ServerSnapshot, UpdateServerConfigRequest } from '@/types/api';

const props = defineProps<{
  snapshot: ServerSnapshot | null;
  logs: Array<{
    id: string;
    timestamp: string;
    level: string;
    source: string;
    message: string;
  }>;
  busy?: boolean;
}>();

const emit = defineEmits<{
  power: [action: 'start' | 'stop' | 'restart'];
  console: [command: string];
  playerAction: [payload: { action: 'op' | 'deop' | 'ban'; playerName: string; reason?: string }];
  sendMessage: [payload: { message: string; targetPlayer?: string }];
  assetToggle: [payload: { assetId: string; nextEnabled: boolean }];
  updateConfig: [payload: UpdateServerConfigRequest];
  createCommand: [payload: { displayName: string; commandText: string; description: string }];
  updateCommand: [payload: { commandId: string; displayName: string; commandText: string; description: string }];
  deleteCommand: [commandId: string];
  executeCommand: [commandText: string];
}>();

const activeTab = ref('operations');
const consoleCommand = ref('');
const playerName = ref('');
const banReason = ref('');
const message = ref('');
const targetPlayer = ref('');
const copyFeedback = ref('');

const tabs: TabItem[] = [
  { key: 'operations', label: '运维' },
  { key: 'config', label: '配置' },
  { key: 'commands', label: '命令' },
  { key: 'assets', label: '资源' },
  { key: 'logs', label: '日志' },
];

const configForm = reactive<UpdateServerConfigRequest>({
  displayName: '',
  rootDirectory: '',
  jvmArguments: '',
  publicAddress: '',
  gameVersion: '',
});

const commandForm = reactive({
  displayName: '',
  commandText: '',
  description: '',
});

const editingCommandId = ref('');

watch(
  () => props.snapshot,
  (snapshot) => {
    if (!snapshot) {
      return;
    }

    configForm.displayName = snapshot.displayName;
    configForm.rootDirectory = snapshot.rootDirectory ?? '';
    configForm.jvmArguments = snapshot.jvmArguments ?? '';
    configForm.publicAddress = snapshot.publicAddress;
    configForm.gameVersion = snapshot.gameVersion;
  },
  { immediate: true },
);

const assets = computed(() => [
  ...(props.snapshot?.mods ?? []),
  ...(props.snapshot?.datapacks ?? []),
  ...(props.snapshot?.resourcePacks ?? []),
]);

const commands = computed<CustomCommand[]>(() => props.snapshot?.customCommands ?? []);

const logColumns = [
  { title: '时间', dataIndex: 'timestamp' },
  { title: '等级', dataIndex: 'level', width: 90 },
  { title: '来源', dataIndex: 'source', width: 140 },
  { title: '消息', dataIndex: 'message' },
];

function submitConsole() {
  if (!consoleCommand.value.trim()) {
    return;
  }

  emit('console', consoleCommand.value.trim());
  consoleCommand.value = '';
}

function submitPlayerAction(action: 'op' | 'deop' | 'ban') {
  if (!playerName.value.trim()) {
    return;
  }

  emit('playerAction', {
    action,
    playerName: playerName.value.trim(),
    reason: action === 'ban' ? banReason.value.trim() : undefined,
  });

  if (action === 'ban') {
    banReason.value = '';
  }
}

function submitMessage() {
  if (!message.value.trim()) {
    return;
  }

  emit('sendMessage', {
    message: message.value.trim(),
    targetPlayer: targetPlayer.value.trim() || undefined,
  });
  message.value = '';
  targetPlayer.value = '';
}

function submitConfig() {
  emit('updateConfig', { ...configForm });
}

function resetCommandForm() {
  editingCommandId.value = '';
  commandForm.displayName = '';
  commandForm.commandText = '';
  commandForm.description = '';
}

function editCommand(command: CustomCommand) {
  editingCommandId.value = command.id;
  commandForm.displayName = command.displayName;
  commandForm.commandText = command.commandText;
  commandForm.description = command.description;
}

function submitCommand() {
  if (!commandForm.displayName.trim() || !commandForm.commandText.trim()) {
    return;
  }

  const payload = {
    displayName: commandForm.displayName.trim(),
    commandText: commandForm.commandText.trim(),
    description: commandForm.description.trim(),
  };

  if (editingCommandId.value) {
    emit('updateCommand', {
      commandId: editingCommandId.value,
      ...payload,
    });
  } else {
    emit('createCommand', payload);
  }

  resetCommandForm();
}

async function copyLogs() {
  const text = props.logs
    .map((entry) => `[${entry.timestamp}] [${entry.level}] ${entry.source}: ${entry.message}`)
    .join('\n');

  if (!text) {
    copyFeedback.value = '没有可复制的日志。';
    return;
  }

  try {
    await navigator.clipboard.writeText(text);
    copyFeedback.value = '日志已复制到剪贴板。';
  } catch {
    copyFeedback.value = '复制失败，请稍后再试。';
  }
}

function onAssetToggle(assetId: string, nextEnabled: boolean) {
  emit('assetToggle', { assetId, nextEnabled });
}
</script>

<template>
  <section class="manager-surface">
    <Card v-if="snapshot?.restartRecommended" class="warning-panel">
      <h3>建议重启服务器</h3>
      <p>资源状态已经发生变化，建议执行重启以确保模组、数据包或资源包状态完全生效。</p>
    </Card>

    <Tabs v-model="activeTab" :items="tabs" leaf-animation shadow>
      <template #operations>
        <div class="tab-grid">
          <Card class="panel">
            <h3>电源控制</h3>
            <div class="button-row">
              <Button :disabled="busy" @click="emit('power', 'start')">启动</Button>
              <Button :disabled="busy" @click="emit('power', 'stop')">停止</Button>
              <Button type="primary" :disabled="busy" @click="emit('power', 'restart')">重启</Button>
            </div>
          </Card>

          <Card class="panel">
            <h3>控制台命令</h3>
            <div class="stack">
              <Input v-model="consoleCommand" allow-clear placeholder="例如：say 服务器维护中" />
              <Button type="primary" :disabled="busy" @click="submitConsole">发送命令</Button>
            </div>
          </Card>

          <Card class="panel">
            <h3>玩家管理</h3>
            <div class="stack">
              <Input v-model="playerName" allow-clear placeholder="玩家名" />
              <Input v-model="banReason" allow-clear placeholder="封禁原因（可选）" />
              <div class="button-row">
                <Button :disabled="busy" @click="submitPlayerAction('op')">授予 OP</Button>
                <Button :disabled="busy" @click="submitPlayerAction('deop')">移除 OP</Button>
                <Button danger :disabled="busy" @click="submitPlayerAction('ban')">封禁</Button>
              </div>
            </div>
          </Card>

          <Card class="panel">
            <h3>消息发送</h3>
            <div class="stack">
              <Input v-model="targetPlayer" allow-clear placeholder="目标玩家（留空则全服广播）" />
              <Input v-model="message" allow-clear placeholder="消息内容" />
              <Button type="primary" :disabled="busy" @click="submitMessage">发送消息</Button>
            </div>
          </Card>
        </div>
      </template>

      <template #config>
        <Card class="panel">
          <h3>服务器配置</h3>
          <div class="form-grid">
            <Input v-model="configForm.displayName" placeholder="显示名称" />
            <Input v-model="configForm.publicAddress" placeholder="公网地址" />
            <Input v-model="configForm.rootDirectory" placeholder="服务器根目录" />
            <Input v-model="configForm.gameVersion" placeholder="游戏版本" />
            <Input v-model="configForm.jvmArguments" class="wide-input" placeholder="JVM 参数" />
          </div>
          <Button type="primary" :disabled="busy" @click="submitConfig">保存配置</Button>
        </Card>
      </template>

      <template #commands>
        <Card class="panel">
          <h3>自定义命令</h3>
          <div class="form-grid">
            <Input v-model="commandForm.displayName" placeholder="命令名称" />
            <Input v-model="commandForm.commandText" placeholder="实际命令，例如 say hello" />
            <Input v-model="commandForm.description" class="wide-input" placeholder="描述（可选）" />
          </div>
          <div class="button-row">
            <Button type="primary" :disabled="busy" @click="submitCommand">
              {{ editingCommandId ? '更新命令' : '创建命令' }}
            </Button>
            <Button v-if="editingCommandId" :disabled="busy" @click="resetCommandForm">取消编辑</Button>
          </div>

          <ul v-if="commands.length" class="command-list">
            <li v-for="command in commands" :key="command.id">
              <div class="command-copy">
                <strong>{{ command.displayName }}</strong>
                <p>{{ command.commandText }}</p>
                <small>{{ command.description || '无描述' }}</small>
              </div>
              <div class="button-row">
                <Button :disabled="busy" @click="emit('executeCommand', command.commandText)">执行</Button>
                <Button :disabled="busy" @click="editCommand(command)">编辑</Button>
                <Button danger :disabled="busy" @click="emit('deleteCommand', command.id)">删除</Button>
              </div>
            </li>
          </ul>
          <p v-else class="empty-text">还没有自定义命令。</p>
        </Card>
      </template>

      <template #assets>
        <Card class="panel">
          <h3>资源切换</h3>
          <ul v-if="assets.length" class="asset-list">
            <li v-for="asset in assets" :key="asset.id">
              <div>
                <strong>{{ asset.name }}</strong>
                <p>{{ asset.type }} · {{ asset.enabled ? '当前启用' : '当前停用' }}</p>
              </div>
              <Switch
                :model-value="asset.enabled"
                :disabled="busy"
                @update:model-value="onAssetToggle(asset.id, $event)"
              />
            </li>
          </ul>
          <p v-else class="empty-text">没有可切换的资源。</p>
        </Card>
      </template>

      <template #logs>
        <Card class="panel">
          <div class="log-header">
            <h3>完整日志</h3>
            <Button :disabled="busy" @click="copyLogs">复制日志</Button>
          </div>
          <p v-if="copyFeedback" class="copy-feedback">{{ copyFeedback }}</p>
          <Table :columns="logColumns as any" :data-source="logs as any" row-key="id" :scroll="{ x: 720 }" empty-text="暂无日志" />
        </Card>
      </template>
    </Tabs>
  </section>
</template>

<style scoped>
.manager-surface {
  display: grid;
  gap: 18px;
  margin-top: 18px;
}

.warning-panel,
.panel {
  display: grid;
  gap: 14px;
}

.warning-panel {
  border-color: rgba(245, 195, 28, 0.55);
}

.tab-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  padding-top: 18px;
}

.panel h3,
.panel p,
.panel small,
.warning-panel h3,
.warning-panel p {
  margin: 0;
}

.stack,
.form-grid {
  display: grid;
  gap: 12px;
}

.form-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.wide-input {
  grid-column: span 2;
}

.button-row,
.log-header {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.log-header {
  justify-content: space-between;
  align-items: center;
}

.asset-list,
.command-list {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.asset-list li,
.command-list li {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  border-radius: var(--animal-border-radius-base);
  padding: 14px;
  background: rgba(236, 245, 223, 0.74);
}

.command-list li {
  flex-direction: column;
}

.asset-list p,
.command-copy p,
.command-copy small,
.copy-feedback,
.empty-text {
  color: var(--animal-text-color-secondary);
}

@media (max-width: 1000px) {
  .tab-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .wide-input {
    grid-column: span 1;
  }

  .asset-list li {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
