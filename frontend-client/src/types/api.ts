export interface AuthSession {
  token: string;
  username: string;
  displayName: string;
  expiresAt: string;
  allowedServerIds: string[];
}

export interface LoginRequest {
  totpCode: string;
}

export interface ManagerRegistrationQrPayload {
  username: string;
  displayName: string;
  qrCodeImage: string;
  manualEntryKey: string;
  otpauthUri: string;
}

export interface ManagerRegistrationConfirmRequest {
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
