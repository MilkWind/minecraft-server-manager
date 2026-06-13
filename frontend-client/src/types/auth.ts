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
