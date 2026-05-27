import { onMounted, ref } from 'vue'
import { api } from '../lib/api'
import type { PublicServerSummary } from '../types/api'

export function useServerDirectory() {
  const servers = ref<PublicServerSummary[]>([])
  const loading = ref(false)
  const error = ref('')

  async function loadServers() {
    loading.value = true
    error.value = ''

    try {
      servers.value = await api.listPublicServers()
    } catch (loadError) {
      error.value = loadError instanceof Error ? loadError.message : '服务器列表加载失败'
    } finally {
      loading.value = false
    }
  }

  onMounted(loadServers)

  return {
    servers,
    loading,
    error,
    loadServers,
  }
}
