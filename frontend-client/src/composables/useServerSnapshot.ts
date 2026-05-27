import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { api } from '../lib/api'
import { useSession } from './useSession'
import type {
  AssetActionRequest,
  CustomCommandUpsertRequest,
  LogEntry,
  PlayerActionRequest,
  SendMessageRequest,
  ServerSnapshot,
  UpdateServerConfigRequest,
} from '../types/api'

const POLL_INTERVAL_MS = 8000

export function useServerSnapshot(serverId: () => string, isManagerView: () => boolean) {
  const snapshot = ref<ServerSnapshot | null>(null)
  const logs = ref<LogEntry[]>([])
  const loading = ref(false)
  const actionLoading = ref(false)
  const error = ref('')
  const actionMessage = ref('')
  const { session } = useSession()

  let pollHandle: number | null = null

  const canManage = computed(() => isManagerView() && session.value != null)

  async function loadSnapshot() {
    loading.value = true
    error.value = ''

    try {
      snapshot.value = canManage.value && session.value != null
        ? await api.getManagerSnapshot(serverId(), session.value.token)
        : await api.getVisitorSnapshot(serverId())

      if (canManage.value && session.value != null) {
        logs.value = await api.getManagerLogs(serverId(), session.value.token)
      } else {
        logs.value = snapshot.value.chatMessages
      }
    } catch (loadError) {
      error.value = loadError instanceof Error ? loadError.message : '服务器状态加载失败'
    } finally {
      loading.value = false
    }
  }

  async function runPowerAction(action: 'start' | 'stop' | 'restart') {
    if (session.value == null) {
      return
    }

    actionLoading.value = true
    actionMessage.value = ''

    try {
      const result = await api.runPowerAction(serverId(), action, session.value.token)
      actionMessage.value = result.message
      await loadSnapshot()
    } catch (actionError) {
      actionMessage.value = actionError instanceof Error ? actionError.message : '操作失败'
    } finally {
      actionLoading.value = false
    }
  }

  async function executeCommand(command: string) {
    if (session.value == null) {
      return
    }

    actionLoading.value = true
    actionMessage.value = ''

    try {
      const result = await api.executeCommand(serverId(), command, session.value.token)
      actionMessage.value = result.message
      await loadSnapshot()
    } catch (actionError) {
      actionMessage.value = actionError instanceof Error ? actionError.message : '命令发送失败'
    } finally {
      actionLoading.value = false
    }
  }

  async function updateServerConfig(request: UpdateServerConfigRequest) {
    if (session.value == null) {
      return
    }

    actionLoading.value = true
    actionMessage.value = ''

    try {
      const result = await api.updateServerConfig(serverId(), request, session.value.token)
      actionMessage.value = `已更新 ${result.displayName} 的配置`
      await loadSnapshot()
    } catch (actionError) {
      actionMessage.value = actionError instanceof Error ? actionError.message : '配置更新失败'
    } finally {
      actionLoading.value = false
    }
  }

  async function createCustomCommand(request: CustomCommandUpsertRequest) {
    if (session.value == null) {
      return
    }

    actionLoading.value = true
    actionMessage.value = ''

    try {
      await api.createCustomCommand(serverId(), request, session.value.token)
      actionMessage.value = '已创建快捷命令'
      await loadSnapshot()
    } catch (actionError) {
      actionMessage.value = actionError instanceof Error ? actionError.message : '快捷命令创建失败'
    } finally {
      actionLoading.value = false
    }
  }

  async function updateCustomCommand(commandId: string, request: CustomCommandUpsertRequest) {
    if (session.value == null) {
      return
    }

    actionLoading.value = true
    actionMessage.value = ''

    try {
      await api.updateCustomCommand(serverId(), commandId, request, session.value.token)
      actionMessage.value = '已更新快捷命令'
      await loadSnapshot()
    } catch (actionError) {
      actionMessage.value = actionError instanceof Error ? actionError.message : '快捷命令更新失败'
    } finally {
      actionLoading.value = false
    }
  }

  async function deleteCustomCommand(commandId: string) {
    if (session.value == null) {
      return
    }

    actionLoading.value = true
    actionMessage.value = ''

    try {
      await api.deleteCustomCommand(serverId(), commandId, session.value.token)
      actionMessage.value = '已删除快捷命令'
      await loadSnapshot()
    } catch (actionError) {
      actionMessage.value = actionError instanceof Error ? actionError.message : '快捷命令删除失败'
    } finally {
      actionLoading.value = false
    }
  }

  async function opPlayer(request: PlayerActionRequest) {
    if (session.value == null) {
      return
    }

    actionLoading.value = true
    actionMessage.value = ''

    try {
      const result = await api.opPlayer(serverId(), request, session.value.token)
      actionMessage.value = result.message
      await loadSnapshot()
    } catch (actionError) {
      actionMessage.value = actionError instanceof Error ? actionError.message : 'OP 操作失败'
    } finally {
      actionLoading.value = false
    }
  }

  async function deopPlayer(request: PlayerActionRequest) {
    if (session.value == null) {
      return
    }

    actionLoading.value = true
    actionMessage.value = ''

    try {
      const result = await api.deopPlayer(serverId(), request, session.value.token)
      actionMessage.value = result.message
      await loadSnapshot()
    } catch (actionError) {
      actionMessage.value = actionError instanceof Error ? actionError.message : '取消 OP 失败'
    } finally {
      actionLoading.value = false
    }
  }

  async function banPlayer(request: PlayerActionRequest) {
    if (session.value == null) {
      return
    }

    actionLoading.value = true
    actionMessage.value = ''

    try {
      const result = await api.banPlayer(serverId(), request, session.value.token)
      actionMessage.value = result.message
      await loadSnapshot()
    } catch (actionError) {
      actionMessage.value = actionError instanceof Error ? actionError.message : '封禁操作失败'
    } finally {
      actionLoading.value = false
    }
  }

  async function sendMessage(request: SendMessageRequest) {
    if (session.value == null) {
      return
    }

    actionLoading.value = true
    actionMessage.value = ''

    try {
      const result = await api.sendMessage(serverId(), request, session.value.token)
      actionMessage.value = result.message
      await loadSnapshot()
    } catch (actionError) {
      actionMessage.value = actionError instanceof Error ? actionError.message : '消息发送失败'
    } finally {
      actionLoading.value = false
    }
  }

  async function suspendAsset(request: AssetActionRequest) {
    if (session.value == null) {
      return
    }

    actionLoading.value = true
    actionMessage.value = ''

    try {
      const result = await api.suspendAsset(serverId(), request, session.value.token)
      actionMessage.value = result.message
      await loadSnapshot()
    } catch (actionError) {
      actionMessage.value = actionError instanceof Error ? actionError.message : '资源停用失败'
    } finally {
      actionLoading.value = false
    }
  }

  async function resumeAsset(request: AssetActionRequest) {
    if (session.value == null) {
      return
    }

    actionLoading.value = true
    actionMessage.value = ''

    try {
      const result = await api.resumeAsset(serverId(), request, session.value.token)
      actionMessage.value = result.message
      await loadSnapshot()
    } catch (actionError) {
      actionMessage.value = actionError instanceof Error ? actionError.message : '资源恢复失败'
    } finally {
      actionLoading.value = false
    }
  }

  function startPolling() {
    stopPolling()
    pollHandle = window.setInterval(() => {
      void loadSnapshot()
    }, POLL_INTERVAL_MS)
  }

  function stopPolling() {
    if (pollHandle != null) {
      window.clearInterval(pollHandle)
      pollHandle = null
    }
  }

  watch([() => serverId(), () => isManagerView(), () => session.value?.token], () => {
    void loadSnapshot()
    startPolling()
  }, { immediate: true })

  onMounted(startPolling)
  onBeforeUnmount(stopPolling)

  return {
    snapshot,
    logs,
    loading,
    actionLoading,
    error,
    actionMessage,
    canManage,
    loadSnapshot,
    runPowerAction,
    executeCommand,
    updateServerConfig,
    createCustomCommand,
    updateCustomCommand,
    deleteCustomCommand,
    opPlayer,
    deopPlayer,
    banPlayer,
    sendMessage,
    suspendAsset,
    resumeAsset,
  }
}
