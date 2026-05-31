import { computed, ref, type MaybeRefOrGetter, toValue } from 'vue';
import { apiRequest } from '@/lib/api';
import type {
  AssetActionRequest,
  CreateManagedServerRequest,
  CustomCommand,
  CustomCommandUpsertRequest,
  LogEntry,
  PlayerActionRequest,
  SendMessageRequest,
  ServerSnapshot,
  UpdateServerConfigRequest,
} from '@/types/api';

export function useServerSnapshot(serverIdSource: MaybeRefOrGetter<string>, managerViewSource: MaybeRefOrGetter<boolean>) {
  const snapshot = ref<ServerSnapshot | null>(null);
  const logs = ref<LogEntry[]>([]);
  const loading = ref(false);
  const busy = ref(false);
  const pollTimer = ref<number | null>(null);

  const onlinePlayers = computed(() => snapshot.value?.onlinePlayers ?? []);
  const mods = computed(() => snapshot.value?.mods ?? []);
  const datapacks = computed(() => snapshot.value?.datapacks ?? []);
  const resourcePacks = computed(() => snapshot.value?.resourcePacks ?? []);
  const chatMessages = computed(() => snapshot.value?.chatMessages ?? []);
  const customCommands = computed<CustomCommand[]>(() => snapshot.value?.customCommands ?? []);
  let currentPollingIntervalMs = 8000;

  function handleVisibilityChange() {
    if (document.hidden) {
      stopPolling();
    } else if (pollTimer.value === null) {
      startPolling(currentPollingIntervalMs);
    }
  }

  const resolvedServerId = () => toValue(serverIdSource);
  const resolvedManagerView = () => toValue(managerViewSource);
  const basePath = () =>
    resolvedManagerView()
      ? `/api/manager/servers/${resolvedServerId()}`
      : `/api/public/servers/${resolvedServerId()}`;

  async function loadSnapshot() {
    loading.value = true;
    try {
      snapshot.value = await apiRequest<ServerSnapshot>(`${basePath()}/snapshot`);
      return snapshot.value;
    } finally {
      loading.value = false;
    }
  }

  async function loadLogs() {
    if (!resolvedManagerView()) {
      logs.value = [];
      return [];
    }

    logs.value = await apiRequest<LogEntry[]>(`${basePath()}/logs`);
    return logs.value;
  }

  async function refreshAll() {
    await loadSnapshot();
    if (resolvedManagerView()) {
      await loadLogs();
    }
  }

  function startPolling(intervalMs = 8000) {
    currentPollingIntervalMs = intervalMs;
    stopPolling();
    void refreshAll();
    pollTimer.value = window.setInterval(() => {
      if (!document.hidden) {
        void refreshAll();
      }
    }, intervalMs);

    document.removeEventListener('visibilitychange', handleVisibilityChange);
    document.addEventListener('visibilitychange', handleVisibilityChange);
  }

  function stopPolling() {
    if (pollTimer.value !== null) {
      window.clearInterval(pollTimer.value);
      pollTimer.value = null;
    }
  }

  function disposePolling() {
    stopPolling();
    document.removeEventListener('visibilitychange', handleVisibilityChange);
  }

  async function runBusyAction<T>(action: () => Promise<T>) {
    busy.value = true;
    try {
      const result = await action();
      await refreshAll();
      return result;
    } finally {
      busy.value = false;
    }
  }

  function power(action: 'start' | 'stop' | 'restart') {
    return runBusyAction(() =>
      apiRequest(`${basePath()}/power/${action}`, {
        method: 'POST',
      }),
    );
  }

  function executeConsoleCommand(command: string) {
    return runBusyAction(() =>
      apiRequest(`${basePath()}/commands/execute`, {
        method: 'POST',
        body: JSON.stringify({ command }),
      }),
    );
  }

  function updateServerConfig(request: UpdateServerConfigRequest) {
    return runBusyAction(() =>
      apiRequest(`${basePath()}/config`, {
        method: 'PUT',
        body: JSON.stringify(request),
      }),
    );
  }

  function createManagedServer(request: CreateManagedServerRequest) {
    return runBusyAction(() =>
      apiRequest('/api/manager/servers', {
        method: 'POST',
        body: JSON.stringify(request),
      }),
    );
  }

  function createCustomCommand(request: CustomCommandUpsertRequest) {
    return runBusyAction(() =>
      apiRequest(`${basePath()}/commands`, {
        method: 'POST',
        body: JSON.stringify(request),
      }),
    );
  }

  function updateCustomCommand(commandId: string, request: CustomCommandUpsertRequest) {
    return runBusyAction(() =>
      apiRequest(`${basePath()}/commands/${commandId}`, {
        method: 'PUT',
        body: JSON.stringify(request),
      }),
    );
  }

  function deleteCustomCommand(commandId: string) {
    return runBusyAction(() =>
      apiRequest(`${basePath()}/commands/${commandId}`, {
        method: 'DELETE',
      }),
    );
  }

  function runPlayerAction(action: 'op' | 'deop' | 'ban', request: PlayerActionRequest) {
    return runBusyAction(() =>
      apiRequest(`${basePath()}/players/${action}`, {
        method: 'POST',
        body: JSON.stringify(request),
      }),
    );
  }

  function sendMessage(request: SendMessageRequest) {
    return runBusyAction(() =>
      apiRequest(`${basePath()}/messages`, {
        method: 'POST',
        body: JSON.stringify(request),
      }),
    );
  }

  function suspendAsset(request: AssetActionRequest) {
    return runBusyAction(() =>
      apiRequest(`${basePath()}/assets/suspend`, {
        method: 'POST',
        body: JSON.stringify(request),
      }),
    );
  }

  function resumeAsset(request: AssetActionRequest) {
    return runBusyAction(() =>
      apiRequest(`${basePath()}/assets/resume`, {
        method: 'POST',
        body: JSON.stringify(request),
      }),
    );
  }

  return {
    snapshot,
    logs,
    loading,
    busy,
    onlinePlayers,
    mods,
    datapacks,
    resourcePacks,
    chatMessages,
    customCommands,
    loadSnapshot,
    loadLogs,
    refreshAll,
    startPolling,
    stopPolling,
    disposePolling,
    power,
    executeConsoleCommand,
    updateServerConfig,
    createManagedServer,
    createCustomCommand,
    updateCustomCommand,
    deleteCustomCommand,
    runPlayerAction,
    sendMessage,
    suspendAsset,
    resumeAsset,
  };
}
