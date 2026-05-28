import { computed, ref } from 'vue';
import { apiRequest } from '@/lib/api';
import {
  readStoredServerId,
  readStoredToken,
  removeStoredServerId,
  removeStoredToken,
  storeServerId,
  storeToken,
} from '@/lib/storage';
import type { AuthSession, LoginRequest } from '@/types/api';

const session = ref<AuthSession | null>(null);
const loading = ref(false);
const selectedServerId = ref(readStoredServerId());

export function useSession() {
  const token = computed(() => session.value?.token ?? readStoredToken());
  const isAuthenticated = computed(() => Boolean(token.value));
  const displayName = computed(() => session.value?.displayName ?? '');
  const allowedServerIds = computed(() => session.value?.allowedServerIds ?? []);

  async function login(request: LoginRequest) {
    loading.value = true;
    try {
      const response = await apiRequest<AuthSession>('/api/manager/auth/login', {
        method: 'POST',
        body: JSON.stringify(request),
      });
      session.value = response;
      storeToken(response.token);
      selectedServerId.value = request.serverId;
      storeServerId(request.serverId);
      return response;
    } finally {
      loading.value = false;
    }
  }

  async function loadCurrentSession() {
    const storedToken = readStoredToken();
    if (!storedToken) {
      session.value = null;
      return null;
    }

    loading.value = true;
    try {
      const response = await apiRequest<AuthSession>('/api/manager/auth/session');
      session.value = response;
      storeToken(response.token);
      if (!selectedServerId.value && response.allowedServerIds.length > 0) {
        selectedServerId.value = response.allowedServerIds[0];
        storeServerId(response.allowedServerIds[0]);
      }
      return response;
    } catch {
      session.value = null;
      removeStoredToken();
      removeStoredServerId();
      return null;
    } finally {
      loading.value = false;
    }
  }

  async function logout() {
    try {
      await apiRequest<void>('/api/manager/auth/logout', { method: 'POST' });
    } finally {
      session.value = null;
      removeStoredToken();
      removeStoredServerId();
    }
  }

  return {
    session,
    token,
    isAuthenticated,
    displayName,
    allowedServerIds,
    selectedServerId,
    loading,
    login,
    loadCurrentSession,
    logout,
  };
}
