<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { Button, Card, Checkbox, CodeBlock, Input, Tabs } from 'animal-island-vue'
import type {
  CustomCommand,
  CustomCommandUpsertRequest,
  LogEntry,
  ManagedAsset,
  PlayerActionRequest,
  SendMessageRequest,
  ServerSnapshot,
  UpdateServerConfigRequest,
} from '../types/api'

const props = defineProps<{
  snapshot: ServerSnapshot
  logs: LogEntry[]
  actionLoading: boolean
  actionMessage: string
}>()

const emit = defineEmits<{
  power: [action: 'start' | 'stop' | 'restart']
  execute: [command: string]
  updateConfig: [payload: UpdateServerConfigRequest]
  createCommand: [payload: CustomCommandUpsertRequest]
  updateCommand: [commandId: string, payload: CustomCommandUpsertRequest]
  deleteCommand: [commandId: string]
  opPlayer: [payload: PlayerActionRequest]
  deopPlayer: [payload: PlayerActionRequest]
  banPlayer: [payload: PlayerActionRequest]
  sendMessage: [payload: SendMessageRequest]
  suspendAsset: [payload: { assetId: string }]
  resumeAsset: [payload: { assetId: string }]
}>()

const activeTab = ref('controls')
const customCommand = ref('')
const editingCommandId = ref('')

const configForm = reactive({
  displayName: '',
  rootDirectory: '',
  jvmArguments: '',
  publicAddress: '',
  gameVersion: '',
})

const commandForm = reactive({
  displayName: '',
  commandText: '',
  description: '',
})

const playerActionForm = reactive({
  playerName: '',
  reason: '',
})

const messageForm = reactive({
  targetPlayer: '',
  message: '',
})

const assetOptions = computed(() =>
  [...props.snapshot.mods, ...props.snapshot.datapacks, ...props.snapshot.resourcePacks].map((asset: ManagedAsset) => ({
    label: `${asset.name} (${asset.type})`,
    value: asset.id,
  })),
)

const selectedAssets = computed(() =>
  [...props.snapshot.mods, ...props.snapshot.datapacks, ...props.snapshot.resourcePacks]
    .filter((asset) => asset.enabled)
    .map((asset) => asset.id),
)

const allAssets = computed(() =>
  [...props.snapshot.mods, ...props.snapshot.datapacks, ...props.snapshot.resourcePacks],
)

function fillCommand(command: CustomCommand) {
  customCommand.value = command.commandText
  editingCommandId.value = command.id
  commandForm.displayName = command.displayName
  commandForm.commandText = command.commandText
  commandForm.description = command.description
}

function submitCommand() {
  const normalized = customCommand.value.trim()
  if (!normalized) {
    return
  }

  emit('execute', normalized)
}

function submitConfig() {
  emit('updateConfig', {
    displayName: configForm.displayName.trim(),
    rootDirectory: configForm.rootDirectory.trim(),
    jvmArguments: configForm.jvmArguments.trim(),
    publicAddress: configForm.publicAddress.trim(),
    gameVersion: configForm.gameVersion.trim(),
  })
}

function submitCommandDefinition() {
  const payload = {
    displayName: commandForm.displayName.trim(),
    commandText: commandForm.commandText.trim(),
    description: commandForm.description.trim(),
  }

  if (editingCommandId.value) {
    emit('updateCommand', editingCommandId.value, payload)
  } else {
    emit('createCommand', payload)
  }
}

function resetCommandForm() {
  editingCommandId.value = ''
  commandForm.displayName = ''
  commandForm.commandText = ''
  commandForm.description = ''
}

function submitPlayerAction(action: 'op' | 'deop' | 'ban') {
  const payload = {
    playerName: playerActionForm.playerName.trim(),
    reason: playerActionForm.reason.trim(),
  }

  if (!payload.playerName) {
    return
  }

  if (action === 'op') {
    emit('opPlayer', payload)
  } else if (action === 'deop') {
    emit('deopPlayer', payload)
  } else {
    emit('banPlayer', payload)
  }
}

function submitMessage() {
  const payload = {
    targetPlayer: messageForm.targetPlayer.trim(),
    message: messageForm.message.trim(),
  }

  if (!payload.message) {
    return
  }

  emit('sendMessage', payload)
}

watch(
  () => props.snapshot,
  (snapshot) => {
    configForm.displayName = snapshot.displayName
    configForm.rootDirectory = snapshot.rootDirectory ?? ''
    configForm.jvmArguments = snapshot.jvmArguments ?? ''
    configForm.publicAddress = snapshot.publicAddress
    configForm.gameVersion = snapshot.gameVersion
  },
  { immediate: true },
)
</script>

<template>
  <Tabs
    v-model="activeTab"
    :items="[
      { key: 'controls', label: '控制台' },
      { key: 'commands', label: '快捷命令' },
      { key: 'logs', label: '完整日志' },
    ]"
  >
    <template #controls>
      <div class="manager-grid">
        <Card class="panel-card">
          <h3>进程控制</h3>
          <p>当前状态：{{ snapshot.status }}</p>
          <div class="inline-actions">
            <Button type="primary" :loading="actionLoading" @click="emit('power', 'start')">启动</Button>
            <Button :loading="actionLoading" @click="emit('power', 'stop')">停止</Button>
            <Button :loading="actionLoading" @click="emit('power', 'restart')">重启</Button>
          </div>
          <p class="status-note">{{ actionMessage || '后端已接入第一版真实进程控制入口。' }}</p>
        </Card>

        <Card class="panel-card">
          <h3>服务器配置</h3>
          <div class="config-grid form-grid">
            <Input v-model="configForm.displayName" placeholder="服务器名称" allow-clear />
            <Input v-model="configForm.publicAddress" placeholder="公开地址" allow-clear />
            <Input v-model="configForm.rootDirectory" placeholder="服务器根目录" allow-clear />
            <Input v-model="configForm.jvmArguments" placeholder="JVM 参数" allow-clear />
            <Input v-model="configForm.gameVersion" placeholder="游戏版本" allow-clear />
          </div>
          <div class="inline-actions top-space">
            <Button type="primary" :loading="actionLoading" @click="submitConfig">保存配置</Button>
          </div>
        </Card>

        <Card class="panel-card">
          <h3>资源启停预览</h3>
          <Checkbox
            :model-value="selectedAssets"
            :options="assetOptions"
            direction="vertical"
            disabled
          />
          <div class="command-list top-space">
            <button
              v-for="asset in allAssets"
              :key="asset.id"
              type="button"
              class="command-chip"
              @click="asset.enabled ? emit('suspendAsset', { assetId: asset.id }) : emit('resumeAsset', { assetId: asset.id })"
            >
              <strong>{{ asset.name }}</strong>
              <span>{{ asset.enabled ? '点击停用' : '点击恢复' }}</span>
            </button>
          </div>
          <p class="status-note">资源状态来自真实目录扫描；变更后建议重启服务器。</p>
        </Card>

        <Card class="panel-card">
          <h3>玩家管理</h3>
          <div class="form-grid">
            <Input v-model="playerActionForm.playerName" placeholder="玩家名" allow-clear />
            <Input v-model="playerActionForm.reason" placeholder="原因（封禁时可选）" allow-clear />
          </div>
          <div class="inline-actions top-space">
            <Button type="primary" :loading="actionLoading" @click="submitPlayerAction('op')">设为 OP</Button>
            <Button :loading="actionLoading" @click="submitPlayerAction('deop')">取消 OP</Button>
            <Button danger :loading="actionLoading" @click="submitPlayerAction('ban')">封禁</Button>
          </div>
        </Card>

        <Card class="panel-card">
          <h3>消息发送</h3>
          <div class="form-grid">
            <Input v-model="messageForm.targetPlayer" placeholder="目标玩家，留空则广播" allow-clear />
            <Input v-model="messageForm.message" placeholder="消息内容" allow-clear />
          </div>
          <div class="inline-actions top-space">
            <Button type="primary" :loading="actionLoading" @click="submitMessage">发送消息</Button>
          </div>
        </Card>
      </div>
    </template>

    <template #commands>
      <div class="manager-grid">
        <Card class="panel-card">
          <h3>发送控制台命令</h3>
          <Input
            v-model="customCommand"
            placeholder="例如：say 服务器将在 10 分钟后重启"
            allow-clear
          />
          <div class="inline-actions top-space">
            <Button type="primary" :loading="actionLoading" @click="submitCommand">发送</Button>
          </div>
        </Card>

        <Card class="panel-card">
          <h3>快捷命令管理</h3>
          <div class="form-grid">
            <Input v-model="commandForm.displayName" placeholder="显示名称" allow-clear />
            <Input v-model="commandForm.commandText" placeholder="实际控制台命令" allow-clear />
            <Input v-model="commandForm.description" placeholder="说明" allow-clear />
          </div>
          <div class="inline-actions top-space">
            <Button type="primary" :loading="actionLoading" @click="submitCommandDefinition">
              {{ editingCommandId ? '更新命令' : '创建命令' }}
            </Button>
            <Button v-if="editingCommandId" @click="resetCommandForm">取消编辑</Button>
          </div>
          <div class="command-list">
            <button
              v-for="command in snapshot.customCommands"
              :key="command.id"
              type="button"
              class="command-chip"
              @click="fillCommand(command)"
            >
              <strong>{{ command.displayName }}</strong>
              <span>{{ command.description }}</span>
            </button>
          </div>
          <div v-if="editingCommandId" class="inline-actions">
            <Button danger :loading="actionLoading" @click="emit('deleteCommand', editingCommandId)">删除当前命令</Button>
          </div>
          <CodeBlock :code="customCommand || '// 选择快捷命令后将在这里展示实际控制台命令'" />
        </Card>
      </div>
    </template>

    <template #logs>
      <Card class="panel-card">
        <h3>完整日志</h3>
        <CodeBlock :code="logs.map((entry) => `[${entry.level}] ${entry.message}`).join('\n')" />
      </Card>
    </template>
  </Tabs>
</template>
