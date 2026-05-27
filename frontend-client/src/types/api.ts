export interface ApiError {
  code: string
  message: string
}

export interface ApiResponse<T> {
  success: boolean
  data: T
  error: ApiError | null
  timestamp: string
}

export interface PublicServerSummary {
  serverId: string
  displayName: string
  status: string
  publicAddress: string
  onlinePlayerCount: number
  gameVersion: string
}

export interface ServerMetrics {
  cpuUsagePercent: number
  usedMemoryMb: number
  maxMemoryMb: number
  networkDownKbps: number
  networkUpKbps: number
}

export interface Player {
  name: string
  operator: boolean
  latencyMs: number
}

export interface ManagedAsset {
  id: string
  name: string
  type: string
  enabled: boolean
}

export interface LogEntry {
  id: string
  timestamp: string
  level: string
  source: string
  message: string
  publicVisible: boolean
}

export interface CustomCommand {
  id: string
  displayName: string
  commandText: string
  description: string
  createdBy: string
  createdAt: string
  updatedAt: string
}

export interface ServerSnapshot {
  serverId: string
  displayName: string
  status: string
  publicAddress: string
  gameVersion: string
  onlinePlayerCount: number
  onlinePlayers: Player[]
  mods: ManagedAsset[]
  datapacks: ManagedAsset[]
  resourcePacks: ManagedAsset[]
  chatMessages: LogEntry[]
  metrics: ServerMetrics
  rootDirectory: string | null
  jvmArguments: string | null
  customCommands: CustomCommand[]
}

export interface AuthSession {
  token: string
  username: string
  displayName: string
  createdAt: string
  expiresAt: string
  accessibleServerIds: string[]
}

export interface LoginRequest {
  username: string
  password: string
  totpCode: string
}

export interface PowerActionResult {
  serverId: string
  action: string
  status: string
  message: string
}

export interface ConsoleCommandResult {
  serverId: string
  command: string
  status: string
  message: string
}

export interface UpdateServerConfigRequest {
  displayName: string
  rootDirectory: string
  jvmArguments: string
  publicAddress: string
  gameVersion: string
}

export interface UpdateServerConfigResult {
  serverId: string
  displayName: string
  rootDirectory: string
  jvmArguments: string
  publicAddress: string
  gameVersion: string
}

export interface CustomCommandUpsertRequest {
  displayName: string
  commandText: string
  description: string
}

export interface CreateManagedServerRequest {
  serverId: string
  displayName: string
  rootDirectory: string
  jvmArguments: string
  publicAddress: string
  gameVersion: string
}

export interface CreateManagedServerResult {
  serverId: string
  displayName: string
  rootDirectory: string
  jvmArguments: string
  publicAddress: string
  gameVersion: string
  status: string
}

export interface PlayerActionRequest {
  playerName: string
  reason: string
}

export interface PlayerActionResult {
  serverId: string
  action: string
  playerName: string
  command: string
  status: string
  message: string
}

export interface SendMessageRequest {
  targetPlayer: string
  message: string
}

export interface SendMessageResult {
  serverId: string
  targetPlayer: string
  command: string
  status: string
  message: string
}

export interface AssetActionRequest {
  assetId: string
}

export interface AssetActionResult {
  serverId: string
  assetId: string
  assetType: string
  action: string
  status: string
  message: string
}
