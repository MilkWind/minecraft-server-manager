import { computed, ref } from 'vue'
import { api } from '../lib/api'
import { loadSession, saveSession } from '../lib/storage'
import type { AuthSession, LoginRequest } from '../types/api'

const session = ref<AuthSession | null>(loadSession())

export function useSession() {
  const isAuthenticated = computed(() => session.value != null)

  async function login(request: LoginRequest) {
    const nextSession = await api.login(request)
    session.value = nextSession
    saveSession(nextSession)
    return nextSession
  }

  async function refreshSession() {
    if (session.value == null) {
      return null
    }

    try {
      const nextSession = await api.getSession(session.value.token)
      session.value = nextSession
      saveSession(nextSession)
      return nextSession
    } catch {
      session.value = null
      saveSession(null)
      return null
    }
  }

  async function logout() {
    if (session.value != null) {
      try {
        await api.logout(session.value.token)
      } finally {
        session.value = null
        saveSession(null)
      }
    }
  }

  return {
    session,
    isAuthenticated,
    login,
    refreshSession,
    logout,
  }
}
