<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
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

const consoleCommand = ref('');
const playerName = ref('');
const banReason = ref('');
const message = ref('');
const targetPlayer = ref('');
const copyFeedback = ref('');

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
  <section class="manager-grid">
    <article v-if="snapshot?.restartRecommended" class="panel wide warning-panel">
      <h3>建议重启服务器</h3>
      <p>资源状态已经发生变化，建议执行重启以确保模组、数据包或资源包状态完全生效。</p>
    </article>

    <article class="panel">
      <h3>电源控制</h3>
      <div class="button-row">
        <button :disabled="busy" @click="emit('power', 'start')">启动</button>
        <button :disabled="busy" @click="emit('power', 'stop')">停止</button>
        <button :disabled="busy" @click="emit('power', 'restart')">重启</button>
      </div>
    </article>

    <article class="panel">
      <h3>控制台命令</h3>
      <div class="stack">
        <input v-model="consoleCommand" placeholder="例如：say 服务器维护中" />
        <button :disabled="busy" @click="submitConsole">发送命令</button>
      </div>
    </article>

    <article class="panel">
      <h3>玩家管理</h3>
      <div class="stack">
        <input v-model="playerName" placeholder="玩家名" />
        <input v-model="banReason" placeholder="封禁原因（可选）" />
        <div class="button-row">
          <button :disabled="busy" @click="submitPlayerAction('op')">授予 OP</button>
          <button :disabled="busy" @click="submitPlayerAction('deop')">移除 OP</button>
          <button :disabled="busy" @click="submitPlayerAction('ban')">封禁</button>
        </div>
      </div>
    </article>

    <article class="panel">
      <h3>消息发送</h3>
      <div class="stack">
        <input v-model="targetPlayer" placeholder="目标玩家（留空则全服广播）" />
        <input v-model="message" placeholder="消息内容" />
        <button :disabled="busy" @click="submitMessage">发送消息</button>
      </div>
    </article>

    <article class="panel wide">
      <h3>服务器配置</h3>
      <div class="form-grid">
        <input v-model="configForm.displayName" placeholder="显示名称" />
        <input v-model="configForm.publicAddress" placeholder="公网地址" />
        <input v-model="configForm.rootDirectory" placeholder="服务器根目录" />
        <input v-model="configForm.gameVersion" placeholder="游戏版本" />
        <input v-model="configForm.jvmArguments" class="wide-input" placeholder="JVM 参数" />
      </div>
      <button :disabled="busy" @click="submitConfig">保存配置</button>
    </article>

    <article class="panel wide">
      <h3>自定义命令</h3>
      <div class="form-grid">
        <input v-model="commandForm.displayName" placeholder="命令名称" />
        <input v-model="commandForm.commandText" placeholder="实际命令，例如 say hello" />
        <input v-model="commandForm.description" class="wide-input" placeholder="描述（可选）" />
      </div>
      <div class="button-row">
        <button :disabled="busy" @click="submitCommand">{{ editingCommandId ? '更新命令' : '创建命令' }}</button>
        <button v-if="editingCommandId" :disabled="busy" @click="resetCommandForm">取消编辑</button>
      </div>

      <ul v-if="commands.length" class="command-list">
        <li v-for="command in commands" :key="command.id">
          <div class="command-copy">
            <strong>{{ command.displayName }}</strong>
            <p>{{ command.commandText }}</p>
            <small>{{ command.description || '无描述' }}</small>
          </div>
          <div class="button-row">
            <button :disabled="busy" @click="emit('executeCommand', command.commandText)">执行</button>
            <button :disabled="busy" @click="editCommand(command)">编辑</button>
            <button :disabled="busy" @click="emit('deleteCommand', command.id)">删除</button>
          </div>
        </li>
      </ul>
      <p v-else class="empty-text">还没有自定义命令。</p>
    </article>

    <article class="panel wide">
      <h3>资源切换</h3>
      <ul v-if="assets.length" class="asset-list">
        <li v-for="asset in assets" :key="asset.id">
          <div>
            <strong>{{ asset.name }}</strong>
            <p>{{ asset.type }} · {{ asset.enabled ? '当前启用' : '当前停用' }}</p>
          </div>
          <button :disabled="busy" @click="onAssetToggle(asset.id, !asset.enabled)">
            {{ asset.enabled ? '停用' : '启用' }}
          </button>
        </li>
      </ul>
      <p v-else class="empty-text">没有可切换的资源。</p>
    </article>

    <article class="panel wide">
      <div class="log-header">
        <h3>完整日志</h3>
        <button :disabled="busy" @click="copyLogs">复制日志</button>
      </div>
      <p v-if="copyFeedback" class="copy-feedback">{{ copyFeedback }}</p>
      <ul class="log-list">
        <li v-for="entry in logs" :key="entry.id">
          <time>{{ new Date(entry.timestamp).toLocaleString() }}</time>
          <strong>[{{ entry.level }}] {{ entry.source }}</strong>
          <span>{{ entry.message }}</span>
        </li>
      </ul>
    </article>
  </section>
</template>

<style scoped>
.manager-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 18px;
}

.panel {
  grid-column: span 6;
  display: grid;
  gap: 14px;
  border: 2px solid rgba(91, 143, 90, 0.28);
  border-radius: 28px;
  padding: 20px;
  background: rgba(255, 252, 243, 0.84);
}

.wide {
  grid-column: span 12;
}

.warning-panel {
  border-color: rgba(214, 148, 36, 0.4);
  background: rgba(255, 246, 213, 0.92);
}

.panel h3,
.panel p,
.panel small {
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

input,
button {
  font: inherit;
}

input {
  width: 100%;
  box-sizing: border-box;
  border: 2px solid rgba(91, 143, 90, 0.34);
  border-radius: 16px;
  padding: 11px 14px;
  background: rgba(255, 255, 255, 0.8);
}

button {
  border: 0;
  border-radius: 999px;
  padding: 10px 16px;
  background: #5b8f5a;
  color: #fffdf3;
  font-weight: 800;
  cursor: pointer;
}

button:disabled {
  cursor: wait;
  opacity: 0.66;
}

.asset-list,
.log-list,
.command-list {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.asset-list li,
.log-list li,
.command-list li {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  border-radius: 18px;
  padding: 14px;
  background: rgba(236, 245, 223, 0.74);
}

.asset-list p,
.command-copy p,
.command-copy small,
.copy-feedback {
  color: #657364;
}

.log-list li,
.command-list li {
  flex-direction: column;
}

.log-list time {
  color: #657364;
  font-size: 12px;
}

.empty-text {
  color: #657364;
}

@media (max-width: 1000px) {
  .panel {
    grid-column: span 12;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .wide-input {
    grid-column: span 1;
  }
}
</style>
