<script setup lang="ts">
import { computed } from 'vue';
import { Card, Table } from 'animal-island-vue';
import type { ServerSnapshot } from '@/types/api';

const props = defineProps<{
  snapshot: ServerSnapshot | null;
  managerView?: boolean;
}>();

const metricsColumns = [
  { title: '指标', dataIndex: 'label' },
  { title: '数值', dataIndex: 'value' },
] as const;

const metricsRows = computed(() =>
  props.snapshot
    ? [
        { label: 'CPU', value: `${props.snapshot.metrics.cpuUsagePercent.toFixed(1)}%` },
        { label: '内存', value: `${props.snapshot.metrics.memoryUsedMb} / ${props.snapshot.metrics.memoryMaxMb} MB` },
        { label: '入站', value: `${props.snapshot.metrics.networkInboundKbps.toFixed(1)} KB/s` },
        { label: '出站', value: `${props.snapshot.metrics.networkOutboundKbps.toFixed(1)} KB/s` },
      ]
    : [],
);
</script>

<template>
  <section v-if="snapshot" class="overview-grid">
    <Card class="panel status-panel">
      <h3>{{ snapshot.displayName }}</h3>
      <p class="status-line"><strong>状态：</strong>{{ snapshot.status }}</p>
      <p><strong>地址：</strong>{{ snapshot.publicAddress }}</p>
      <p><strong>版本：</strong>{{ snapshot.gameVersion }}</p>
      <p><strong>在线玩家：</strong>{{ snapshot.onlinePlayerCount }}</p>
      <p v-if="snapshot.restartRecommended" class="restart-warning">资源状态已变化，建议重启服务器。</p>
      <p v-if="managerView && snapshot.rootDirectory"><strong>根目录：</strong>{{ snapshot.rootDirectory }}</p>
      <p v-if="managerView && snapshot.jvmArguments"><strong>JVM：</strong>{{ snapshot.jvmArguments }}</p>
    </Card>

    <Card class="panel metrics-panel">
      <h3>性能概览</h3>
      <Table :columns="metricsColumns as any" :data-source="metricsRows as any" :show-header="false" />
    </Card>

    <Card class="panel">
      <h3>在线玩家</h3>
      <ul v-if="snapshot.onlinePlayers.length" class="tag-list">
        <li v-for="player in snapshot.onlinePlayers" :key="player.name">{{ player.name }}</li>
      </ul>
      <p v-else class="empty-text">当前没有在线玩家。</p>
    </Card>

    <Card class="panel">
      <h3>聊天消息</h3>
      <ul class="log-list">
        <li v-for="entry in snapshot.chatMessages" :key="entry.id">
          <time>{{ new Date(entry.timestamp).toLocaleString() }}</time>
          <span>{{ entry.message }}</span>
        </li>
      </ul>
    </Card>

    <Card class="panel">
      <h3>模组</h3>
      <ul v-if="snapshot.mods.length" class="asset-list">
        <li v-for="asset in snapshot.mods" :key="asset.id">
          <span>{{ asset.name }}</span>
          <strong>{{ asset.enabled ? '启用' : '停用' }}</strong>
        </li>
      </ul>
      <p v-else class="empty-text">没有发现模组。</p>
    </Card>

    <Card class="panel">
      <h3>数据包</h3>
      <ul v-if="snapshot.datapacks.length" class="asset-list">
        <li v-for="asset in snapshot.datapacks" :key="asset.id">
          <span>{{ asset.name }}</span>
          <strong>{{ asset.enabled ? '启用' : '停用' }}</strong>
        </li>
      </ul>
      <p v-else class="empty-text">没有发现数据包。</p>
    </Card>

    <Card class="panel">
      <h3>资源包</h3>
      <ul v-if="snapshot.resourcePacks.length" class="asset-list">
        <li v-for="asset in snapshot.resourcePacks" :key="asset.id">
          <span>{{ asset.name }}</span>
          <strong>{{ asset.enabled ? '启用' : '停用' }}</strong>
        </li>
      </ul>
      <p v-else class="empty-text">没有发现资源包。</p>
    </Card>
  </section>

  <Card v-else class="empty-state">
    <h3>暂无服务器数据</h3>
    <p>请稍后刷新，或先启动服务器后再查看。</p>
  </Card>
</template>

<style scoped>
.overview-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 18px;
}

.panel,
.empty-state {
  display: grid;
  gap: 14px;
}

.status-panel {
  grid-column: span 4;
}

.metrics-panel {
  grid-column: span 8;
}

.panel:nth-child(3),
.panel:nth-child(4),
.panel:nth-child(5),
.panel:nth-child(6) {
  grid-column: span 4;
}

.panel h3,
.empty-state h3,
.panel p,
.empty-state p {
  margin: 0;
}

.status-line,
.empty-text {
  color: var(--animal-text-color-secondary);
}

.restart-warning {
  border-radius: var(--animal-border-radius-base);
  padding: 10px 12px;
  background: rgba(245, 195, 28, 0.18);
  color: #8f5f16;
  font-weight: 800;
}

.tag-list,
.asset-list,
.log-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.tag-list {
  grid-template-columns: repeat(auto-fit, minmax(120px, max-content));
}

.tag-list li {
  border-radius: 999px;
  padding: 10px 14px;
  background: var(--animal-primary-color);
  color: #fff;
  font-weight: 700;
}

.metric-table {
  width: 100%;
}

.metric-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.asset-list li,
.log-list li {
  border-radius: var(--animal-border-radius-base);
  padding: 12px 14px;
  background: rgba(236, 245, 223, 0.72);
}

.asset-list li,
.log-list li {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.log-list li {
  flex-direction: column;
}

@media (max-width: 1100px) {
  .status-panel,
  .metrics-panel,
  .panel:nth-child(3),
  .panel:nth-child(4),
  .panel:nth-child(5),
  .panel:nth-child(6) {
    grid-column: span 12;
  }
}
</style>
