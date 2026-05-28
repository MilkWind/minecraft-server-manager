import { ref } from 'vue';
import { apiRequest } from '@/lib/api';
import type { CreateManagedServerRequest, PublicServerSummary } from '@/types/api';

const servers = ref<PublicServerSummary[]>([]);
const loading = ref(false);
const creating = ref(false);

export function useServerDirectory() {
  async function loadServers() {
    loading.value = true;
    try {
      servers.value = await apiRequest<PublicServerSummary[]>('/api/public/servers');
      return servers.value;
    } finally {
      loading.value = false;
    }
  }

  async function createManagedServer(request: CreateManagedServerRequest) {
    creating.value = true;
    try {
      await apiRequest('/api/manager/servers', {
        method: 'POST',
        body: JSON.stringify(request),
      });
      await loadServers();
    } finally {
      creating.value = false;
    }
  }

  return {
    servers,
    loading,
    creating,
    loadServers,
    createManagedServer,
  };
}
