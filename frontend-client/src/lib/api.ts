import type {
  AssetActionRequest,
  AssetActionResult,
  ApiResponse,
  AuthSession,
  ConsoleCommandResult,
  CreateManagedServerRequest,
  CreateManagedServerResult,
  CustomCommand,
  CustomCommandUpsertRequest,
  LoginRequest,
  LogEntry,
  PowerActionResult,
  PublicServerSummary,
  PlayerActionRequest,
  PlayerActionResult,
  SendMessageRequest,
  SendMessageResult,
  ServerSnapshot,
  UpdateServerConfigRequest,
  UpdateServerConfigResult,
} from '../types/api'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

async function request<T>(path: string, init?: RequestInit, token?: string): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(init?.headers ?? {}),
    },
  })

  const payload = (await response.json()) as ApiResponse<T>

  if (!response.ok || !payload.success) {
    throw new Error(payload.error?.message ?? '请求失败')
  }

  return payload.data
}

export const api = {
  listPublicServers() {
    return request<PublicServerSummary[]>('/api/public/servers')
  },
  getVisitorSnapshot(serverId: string) {
    return request<ServerSnapshot>(`/api/public/servers/${serverId}/snapshot`)
  },
  login(body: LoginRequest) {
    return request<AuthSession>('/api/manager/auth/login', {
      method: 'POST',
      body: JSON.stringify(body),
    })
  },
  getSession(token: string) {
    return request<AuthSession>('/api/manager/auth/session', undefined, token)
  },
  logout(token: string) {
    return request<void>('/api/manager/auth/logout', {
      method: 'POST',
    }, token)
  },
  getManagerSnapshot(serverId: string, token: string) {
    return request<ServerSnapshot>(`/api/manager/servers/${serverId}/snapshot`, undefined, token)
  },
  getManagerLogs(serverId: string, token: string) {
    return request<LogEntry[]>(`/api/manager/servers/${serverId}/logs`, undefined, token)
  },
  runPowerAction(serverId: string, action: 'start' | 'stop' | 'restart', token: string) {
    return request<PowerActionResult>(`/api/manager/servers/${serverId}/power/${action}`, {
      method: 'POST',
    }, token)
  },
  executeCommand(serverId: string, command: string, token: string) {
    return request<ConsoleCommandResult>(`/api/manager/servers/${serverId}/commands/execute`, {
      method: 'POST',
      body: JSON.stringify({ command }),
    }, token)
  },
  updateServerConfig(serverId: string, body: UpdateServerConfigRequest, token: string) {
    return request<UpdateServerConfigResult>(`/api/manager/servers/${serverId}/config`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }, token)
  },
  listCustomCommands(serverId: string, token: string) {
    return request<CustomCommand[]>(`/api/manager/servers/${serverId}/commands`, undefined, token)
  },
  createCustomCommand(serverId: string, body: CustomCommandUpsertRequest, token: string) {
    return request<CustomCommand>(`/api/manager/servers/${serverId}/commands`, {
      method: 'POST',
      body: JSON.stringify(body),
    }, token)
  },
  updateCustomCommand(serverId: string, commandId: string, body: CustomCommandUpsertRequest, token: string) {
    return request<CustomCommand>(`/api/manager/servers/${serverId}/commands/${commandId}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }, token)
  },
  deleteCustomCommand(serverId: string, commandId: string, token: string) {
    return request<void>(`/api/manager/servers/${serverId}/commands/${commandId}`, {
      method: 'DELETE',
    }, token)
  },
  createManagedServer(body: CreateManagedServerRequest, token: string) {
    return request<CreateManagedServerResult>('/api/manager/servers', {
      method: 'POST',
      body: JSON.stringify(body),
    }, token)
  },
  opPlayer(serverId: string, body: PlayerActionRequest, token: string) {
    return request<PlayerActionResult>(`/api/manager/servers/${serverId}/players/op`, {
      method: 'POST',
      body: JSON.stringify(body),
    }, token)
  },
  deopPlayer(serverId: string, body: PlayerActionRequest, token: string) {
    return request<PlayerActionResult>(`/api/manager/servers/${serverId}/players/deop`, {
      method: 'POST',
      body: JSON.stringify(body),
    }, token)
  },
  banPlayer(serverId: string, body: PlayerActionRequest, token: string) {
    return request<PlayerActionResult>(`/api/manager/servers/${serverId}/players/ban`, {
      method: 'POST',
      body: JSON.stringify(body),
    }, token)
  },
  sendMessage(serverId: string, body: SendMessageRequest, token: string) {
    return request<SendMessageResult>(`/api/manager/servers/${serverId}/messages`, {
      method: 'POST',
      body: JSON.stringify(body),
    }, token)
  },
  suspendAsset(serverId: string, body: AssetActionRequest, token: string) {
    return request<AssetActionResult>(`/api/manager/servers/${serverId}/assets/suspend`, {
      method: 'POST',
      body: JSON.stringify(body),
    }, token)
  },
  resumeAsset(serverId: string, body: AssetActionRequest, token: string) {
    return request<AssetActionResult>(`/api/manager/servers/${serverId}/assets/resume`, {
      method: 'POST',
      body: JSON.stringify(body),
    }, token)
  },
}
