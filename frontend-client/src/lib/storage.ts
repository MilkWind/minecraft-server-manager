const TOKEN_KEY = 'minecraft-manager-token';
const SERVER_ID_KEY = 'minecraft-manager-server-id';

function getStorage() {
  if (typeof window === 'undefined') {
    return null;
  }
  return window.localStorage;
}

export function readStoredToken(): string {
  return getStorage()?.getItem(TOKEN_KEY) ?? '';
}

export function storeToken(token: string): void {
  getStorage()?.setItem(TOKEN_KEY, token);
}

export function removeStoredToken(): void {
  getStorage()?.removeItem(TOKEN_KEY);
}

export function readStoredServerId(): string {
  return getStorage()?.getItem(SERVER_ID_KEY) ?? '';
}

export function storeServerId(serverId: string): void {
  getStorage()?.setItem(SERVER_ID_KEY, serverId);
}

export function removeStoredServerId(): void {
  getStorage()?.removeItem(SERVER_ID_KEY);
}
