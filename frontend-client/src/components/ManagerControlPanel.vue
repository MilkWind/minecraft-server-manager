<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import {
  Button, Card, Input, Switch, Tabs, Divider,
  Select, Checkbox, CodeBlock, Tooltip, Typewriter, Collapse,
  type TabItem,
} from 'animal-island-vue';
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
  batchAssetToggle: [payload: { assetId: string; nextEnabled: boolean }[]];
}>();

const activeTab = ref('operations');
const consoleCommand = ref('');
const selectedPlayer = ref('');
const banReason = ref('');
const message = ref('');
const selectedTarget = ref('__broadcast__');
const copyFeedback = ref('');
const commandFeedbackKey = ref(0);
const commandFeedbackText = ref('');
const logLevelFilter = ref('all');
const selectedModIds = ref<string[]>([]);
const selectedDatapackIds = ref<string[]>([]);

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

const playerOptions = computed(() =>
  (props.snapshot?.onlinePlayers ?? []).map((p) => ({ key: p.name, label: p.name })),
);

const targetOptions = computed(() => [
  { key: '__broadcast__', label: '全服广播' },
  ...(props.snapshot?.onlinePlayers ?? []).map((p) => ({ key: p.name, label: p.name })),
]);

const modCheckboxOptions = computed(() =>
  (props.snapshot?.mods ?? []).map((m) => ({ label: m.name, value: m.id })),
);

const datapackCheckboxOptions = computed(() =>
  (props.snapshot?.datapacks ?? []).map((d) => ({ label: d.name, value: d.id })),
);

watch(
  () => props.snapshot,
  (snapshot) => {
    if (!snapshot) return;
    configForm.displayName = snapshot.displayName;
    configForm.rootDirectory = snapshot.rootDirectory ?? '';
    configForm.jvmArguments = snapshot.jvmArguments ?? '';
    configForm.publicAddress = snapshot.publicAddress;
    configForm.gameVersion = snapshot.gameVersion;
  },
  { immediate: true },
);

const commands = computed<CustomCommand[]>(() => props.snapshot?.customCommands ?? []);

const filteredLogs = computed(() => {
  if (!props.logs || logLevelFilter.value === 'all') return props.logs;
  return props.logs.filter((entry) => entry.level === logLevelFilter.value);
});

const logCode = computed(() =>
  filteredLogs.value
    .map((entry) => `[${entry.timestamp}] [${entry.level}] ${entry.source}: ${entry.message}`)
    .join('\n'),
);

const logLevelOptions = [
  { key: 'all', label: '全部' },
  { key: 'INFO', label: '信息' },
  { key: 'WARN', label: '警告' },
  { key: 'ERROR', label: '错误' },
];

function submitConsole() {
  if (!consoleCommand.value.trim()) return;
  emit('console', consoleCommand.value.trim());
  consoleCommand.value = '';
}

function submitPlayerAction(action: 'op' | 'deop' | 'ban') {
  if (!selectedPlayer.value) return;
  emit('playerAction', {
    action,
    playerName: selectedPlayer.value,
    reason: action === 'ban' ? banReason.value.trim() : undefined,
  });
  if (action === 'ban') banReason.value = '';
}

function submitMessage() {
  if (!message.value.trim()) return;
  emit('sendMessage', {
    message: message.value.trim(),
    targetPlayer: selectedTarget.value === '__broadcast__' ? undefined : selectedTarget.value,
  });
  message.value = '';
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
  if (!commandForm.displayName.trim() || !commandForm.commandText.trim()) return;
  const payload = {
    displayName: commandForm.displayName.trim(),
    commandText: commandForm.commandText.trim(),
    description: commandForm.description.trim(),
  };
  if (editingCommandId.value) {
    emit('updateCommand', { commandId: editingCommandId.value, ...payload });
  } else {
    emit('createCommand', payload);
  }
  resetCommandForm();
}

function onExecuteCommand(commandText: string) {
  commandFeedbackText.value = '命令已发送...';
  commandFeedbackKey.value++;
  emit('executeCommand', commandText);
}

async function copyLogs() {
  const text = filteredLogs.value
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

function batchToggleMods(nextEnabled: boolean) {
  if (selectedModIds.value.length === 0) return;
  emit('batchAssetToggle', selectedModIds.value.map((id) => ({ assetId: id, nextEnabled })));
  selectedModIds.value = [];
}

function batchToggleDatapacks(nextEnabled: boolean) {
  if (selectedDatapackIds.value.length === 0) return;
  emit('batchAssetToggle', selectedDatapackIds.value.map((id) => ({ assetId: id, nextEnabled })));
  selectedDatapackIds.value = [];
}
</script>

<template>
  <section class="manager-surface">
    <Card v-if="snapshot?.restartRecommended" class="warning-panel">
      <h3>建议重启服务器</h3>
      <p>资源状态已经发生变化，建议执行重启以确保模组、数据包或资源包状态完全生效。</p>
    </Card>

    <Tabs v-model="activeTab" :items="tabs" leaf-animation shadow>
      <!-- Tab: 运维 -->
      <template #operations>
        <div class="tab-grid">
          <Card class="panel">
            <h3>电源控制</h3>
            <div class="button-row">
              <Tooltip title="启动服务器进程，玩家可以连接" placement="top">
                <Button :disabled="busy" @click="emit('power', 'start')">启动</Button>
              </Tooltip>
              <Tooltip title="停止服务器进程，所有玩家将被断开" placement="top">
                <Button :disabled="busy" @click="emit('power', 'stop')">停止</Button>
              </Tooltip>
              <Tooltip title="先停止再启动服务器，建议在维护时使用" placement="top">
                <Button type="primary" :disabled="busy" @click="emit('power', 'restart')">重启</Button>
              </Tooltip>
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
              <Select
                v-if="playerOptions.length > 0"
                v-model="selectedPlayer"
                :options="playerOptions"
                placeholder="选择在线玩家"
              />
              <Input v-else v-model="selectedPlayer" allow-clear placeholder="玩家名" />
              <Input v-model="banReason" allow-clear placeholder="封禁原因（可选）" />
              <div class="button-row">
                <Tooltip title="给予该玩家管理员权限" placement="top">
                  <Button :disabled="busy || !selectedPlayer" @click="submitPlayerAction('op')">授予 OP</Button>
                </Tooltip>
                <Tooltip title="撤销该玩家的管理员权限" placement="top">
                  <Button :disabled="busy || !selectedPlayer" @click="submitPlayerAction('deop')">移除 OP</Button>
                </Tooltip>
                <Tooltip title="将玩家加入服务器黑名单" placement="top">
                  <Button danger :disabled="busy || !selectedPlayer" @click="submitPlayerAction('ban')">封禁</Button>
                </Tooltip>
              </div>
            </div>
          </Card>

          <Card class="panel">
            <h3>消息发送</h3>
            <div class="stack">
              <Select
                v-model="selectedTarget"
                :options="targetOptions"
                placeholder="选择目标"
              />
              <Input v-model="message" allow-clear placeholder="消息内容" />
              <Tooltip title="留空目标将向所有在线玩家发送消息" placement="top">
                <Button type="primary" :disabled="busy" @click="submitMessage">发送消息</Button>
              </Tooltip>
            </div>
          </Card>
        </div>

        <Collapse question="常用命令参考" :default-expanded="false" class="ref-collapse">
          <div class="command-ref">
            <p><code>say &lt;message&gt;</code> — 向所有玩家发送消息</p>
            <p><code>whitelist add &lt;player&gt;</code> — 将玩家加入白名单</p>
            <p><code>whitelist remove &lt;player&gt;</code> — 将玩家移出白名单</p>
            <p><code>kick &lt;player&gt; [reason]</code> — 踢出玩家</p>
            <p><code>ban &lt;player&gt; [reason]</code> — 封禁玩家</p>
            <p><code>time set &lt;value&gt;</code> — 设置游戏时间 (day/noon/night/midnight)</p>
            <p><code>weather &lt;clear|rain|thunder&gt;</code> — 设置天气</p>
            <p><code>save-all</code> — 保存世界</p>
            <p><code>list</code> — 列出在线玩家</p>
            <p><code>op &lt;player&gt;</code> — 授予 OP</p>
            <p><code>deop &lt;player&gt;</code> — 移除 OP</p>
          </div>
        </Collapse>
      </template>

      <!-- Tab: 配置 -->
      <template #config>
        <Card class="panel">
          <h3>服务器配置</h3>
          <div class="form-grid">
            <Tooltip title="在管理面板中显示的服务器名称" placement="top">
              <Input v-model="configForm.displayName" placeholder="显示名称" />
            </Tooltip>
            <Tooltip title="玩家连接服务器使用的公网地址，如 example.com:25565" placement="top">
              <Input v-model="configForm.publicAddress" placeholder="公网地址" />
            </Tooltip>
            <Tooltip title="服务器文件在宿主机上的绝对路径" placement="top">
              <Input v-model="configForm.rootDirectory" placeholder="服务器根目录" />
            </Tooltip>
            <Tooltip title="Minecraft 游戏版本号，如 1.21" placement="top">
              <Input v-model="configForm.gameVersion" placeholder="游戏版本" />
            </Tooltip>
            <Tooltip title="Java 虚拟机启动参数，如 -Xms2G -Xmx4G" placement="top">
              <Input v-model="configForm.jvmArguments" class="wide-input" placeholder="JVM 参数" />
            </Tooltip>
          </div>
          <Divider type="line-brown" />
          <Button type="primary" :disabled="busy" @click="submitConfig">保存配置</Button>

          <Collapse question="高级配置说明" :default-expanded="false">
            <div class="config-help">
              <p><strong>JVM 参数建议：</strong></p>
              <p>小型服务器: <code>-Xms1G -Xmx2G</code></p>
              <p>中型服务器 (10-20 人): <code>-Xms2G -Xmx4G</code></p>
              <p>大型服务器 (20+ 人): <code>-Xms4G -Xmx8G</code></p>
              <p><strong>根目录：</strong>必须是 Minecraft 服务端文件所在目录的绝对路径，包含 server.properties 和 world 文件夹。</p>
              <p><strong>公网地址：</strong>如果使用端口转发，格式为 <code>域名或IP:端口</code>，如 <code>example.com:25565</code>。</p>
            </div>
          </Collapse>
        </Card>
      </template>

      <!-- Tab: 命令 -->
      <template #commands>
        <Card class="panel">
          <h3>自定义命令</h3>
          <div class="form-grid">
            <Input v-model="commandForm.displayName" placeholder="命令名称" />
            <Input v-model="commandForm.commandText" placeholder="实际命令，例如 say hello" />
            <Input v-model="commandForm.description" class="wide-input" placeholder="备注（可选）" />
          </div>
          <div class="button-row">
            <Button type="primary" :disabled="busy" @click="submitCommand">
              {{ editingCommandId ? '更新命令' : '创建命令' }}
            </Button>
            <Button v-if="editingCommandId" :disabled="busy" @click="resetCommandForm">取消编辑</Button>
          </div>

          <div v-if="commandFeedbackText" class="command-feedback">
            <Typewriter
              :key="commandFeedbackKey"
              :text="commandFeedbackText"
              :auto-play="true"
              :speed="35"
            />
          </div>

          <div v-if="commands.length" class="command-list">
            <Collapse
              v-for="command in commands"
              :key="command.id"
              :question="command.displayName"
              :default-expanded="false"
            >
              <div class="command-detail">
                <div class="command-copy">
                  <p><code>{{ command.commandText }}</code></p>
                  <small>{{ command.description || '无备注' }}</small>
                </div>
                <div class="button-row command-actions">
                  <Tooltip title="执行此命令" placement="top">
                    <Button :disabled="busy" @click="onExecuteCommand(command.commandText)">执行</Button>
                  </Tooltip>
                  <Tooltip title="编辑此命令" placement="top">
                    <Button :disabled="busy" @click="editCommand(command)">编辑</Button>
                  </Tooltip>
                  <Tooltip title="删除此命令" placement="top">
                    <Button danger :disabled="busy" @click="emit('deleteCommand', command.id)">删除</Button>
                  </Tooltip>
                </div>
              </div>
            </Collapse>
          </div>
          <p v-else class="empty-text">还没有自定义命令。</p>
        </Card>
      </template>

      <!-- Tab: 资源 -->
      <template #assets>
        <Card class="panel">
          <h3>资源切换</h3>

          <Collapse :question="`模组 (${modCheckboxOptions.length})`" :default-expanded="modCheckboxOptions.length > 0">
            <template v-if="modCheckboxOptions.length > 0">
              <div class="asset-select-section">
                <Checkbox
                  v-model="selectedModIds"
                  :options="modCheckboxOptions"
                  direction="horizontal"
                  size="small"
                />
                <div v-if="selectedModIds.length > 0" class="batch-bar">
                  <Button size="small" @click="batchToggleMods(true)">
                    批量启用 ({{ selectedModIds.length }})
                  </Button>
                  <Button size="small" danger @click="batchToggleMods(false)">
                    批量停用 ({{ selectedModIds.length }})
                  </Button>
                </div>
              </div>
              <ul class="asset-list">
                <li v-for="asset in snapshot?.mods" :key="asset.id">
                  <div>
                    <strong>{{ asset.name }}</strong>
                    <p>模组 · {{ asset.enabled ? '当前启用' : '当前停用' }}</p>
                  </div>
                  <Switch
                    :model-value="asset.enabled"
                    :disabled="busy"
                    @update:model-value="onAssetToggle(asset.id, $event)"
                  />
                </li>
              </ul>
            </template>
            <p v-else class="empty-text">没有发现模组。</p>
          </Collapse>

          <Collapse :question="`数据包 (${datapackCheckboxOptions.length})`" :default-expanded="false" class="asset-group">
            <template v-if="datapackCheckboxOptions.length > 0">
              <div class="asset-select-section">
                <Checkbox
                  v-model="selectedDatapackIds"
                  :options="datapackCheckboxOptions"
                  direction="horizontal"
                  size="small"
                />
                <div v-if="selectedDatapackIds.length > 0" class="batch-bar">
                  <Button size="small" @click="batchToggleDatapacks(true)">
                    批量启用 ({{ selectedDatapackIds.length }})
                  </Button>
                  <Button size="small" danger @click="batchToggleDatapacks(false)">
                    批量停用 ({{ selectedDatapackIds.length }})
                  </Button>
                </div>
              </div>
              <ul class="asset-list">
                <li v-for="asset in snapshot?.datapacks" :key="asset.id">
                  <div>
                    <strong>{{ asset.name }}</strong>
                    <p>数据包 · {{ asset.enabled ? '当前启用' : '当前停用' }}</p>
                  </div>
                  <Switch
                    :model-value="asset.enabled"
                    :disabled="busy"
                    @update:model-value="onAssetToggle(asset.id, $event)"
                  />
                </li>
              </ul>
            </template>
            <p v-else class="empty-text">没有发现数据包。</p>
          </Collapse>
        </Card>
      </template>

      <!-- Tab: 日志 -->
      <template #logs>
        <Card class="panel">
          <div class="log-header">
            <div class="log-controls">
              <h3>完整日志</h3>
              <Select
                v-model="logLevelFilter"
                :options="logLevelOptions"
                placeholder="筛选等级"
              />
            </div>
            <Tooltip title="复制所有筛选后的日志到剪贴板" placement="top">
              <Button :disabled="busy" @click="copyLogs">复制日志</Button>
            </Tooltip>
          </div>
          <p v-if="copyFeedback" class="copy-feedback">{{ copyFeedback }}</p>

          <Collapse :question="`完整日志 (${filteredLogs.length} 条)`" :default-expanded="false">
            <CodeBlock v-if="logCode" :code="logCode" />
            <p v-else class="empty-text">暂无日志。</p>
          </Collapse>
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
  align-items: flex-start;
}

.log-controls {
  display: flex;
  align-items: center;
  gap: 16px;
}

.ref-collapse {
  margin-top: 18px;
}

.command-ref p {
  margin: 6px 0;
  font-size: 14px;
  line-height: 1.6;
}

.command-ref code {
  background: rgba(236, 245, 223, 0.74);
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 13px;
}

.config-help p {
  margin: 6px 0;
  font-size: 14px;
  line-height: 1.6;
}

.config-help code {
  background: rgba(236, 245, 223, 0.74);
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 13px;
}

.command-feedback {
  padding: 10px 0;
  font-size: 14px;
  color: var(--animal-primary-color);
  font-weight: 700;
}

.command-detail {
  display: grid;
  gap: 12px;
}

.command-copy p {
  margin: 0 0 4px;
}

.command-copy code {
  background: rgba(236, 245, 223, 0.74);
  border-radius: 4px;
  padding: 2px 8px;
  font-size: 14px;
  word-break: break-all;
}

.command-actions {
  padding-top: 4px;
}

.asset-group {
  margin-top: 14px;
}

.asset-select-section {
  margin-bottom: 12px;
}

.batch-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
  padding: 10px 14px;
  border-radius: var(--animal-border-radius-base);
  background: rgba(25, 200, 185, 0.1);
}

.asset-list {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.asset-list li {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  border-radius: var(--animal-border-radius-base);
  padding: 14px;
  background: rgba(236, 245, 223, 0.74);
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

  .log-header {
    flex-direction: column;
  }

  .log-controls {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>
