export interface AuthSession {
  token: string;
  username: string;
  displayName: string;
  expiresAt: string;
  allowedServerIds: string[];
}

export interface LoginRequest {
  username: string;
  password: string;
  totpCode: string;
  serverId: string;
}

export interface ManagerRegistrationRequest {
  username: string;
  displayName: string;
  password: string;
}

export interface ManagerRegistrationQrPayload {
  registrationId: string;
  username: string;
  displayName: string;
  qrCodeImage: string;
  manualEntryKey: string;
  otpauthUri: string;
}

export interface ManagerRegistrationConfirmRequest {
  registrationId: string;
  totpCode: string;
}

export interface ManagerRegistrationResult {
  username: string;
  displayName: string;
  message: string;
}

export interface PublicServerSummary {
  serverId: string;
  displayName: string;
  status: string;
  publicAddress: string;
  onlinePlayerCount: number;
  gameVersion: string;
}

export interface Player {
  name: string;
  operator: boolean;
  latencyMs: number;
}

export interface ManagedAsset {
  id: string;
  name: string;
  type: string;
  enabled: boolean;
  path: string;
}

export interface LogEntry {
  id: string;
  timestamp: string;
  level: string;
  source: string;
  message: string;
  publicVisible: boolean;
}

export interface ServerMetrics {
  cpuUsagePercent: number;
  memoryUsedMb: number;
  memoryMaxMb: number;
  networkInboundKbps: number;
  networkOutboundKbps: number;
}

export interface CustomCommand {
  id: string;
  displayName: string;
  commandText: string;
  description: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface ServerSnapshot {
  serverId: string;
  displayName: string;
  status: string;
  publicAddress: string;
  gameVersion: string;
  onlinePlayerCount: number;
  onlinePlayers: Player[];
  mods: ManagedAsset[];
  datapacks: ManagedAsset[];
  chatMessages: LogEntry[];
  metrics: ServerMetrics;
  restartRecommended: boolean;
  rootDirectory: string | null;
  jvmArguments: string | null;
  customCommands: CustomCommand[];
}

export interface UpdateServerConfigRequest {
  displayName: string;
  rootDirectory: string;
  jvmArguments: string;
  publicAddress: string;
  gameVersion: string;
}

export interface CreateManagedServerRequest extends UpdateServerConfigRequest {
  serverId: string;
}

export interface CustomCommandUpsertRequest {
  displayName: string;
  commandText: string;
  description: string;
}

export interface PlayerActionRequest {
  playerName: string;
  reason?: string;
}

export interface SendMessageRequest {
  message: string;
  targetPlayer?: string;
}

export interface AssetActionRequest {
  assetId: string;
}
