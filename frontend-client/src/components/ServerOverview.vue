<script setup lang="ts">
import type { ServerSnapshot } from '@/types/api';

defineProps<{
  snapshot: ServerSnapshot | null;
  managerView?: boolean;
}>();
</script>

<template>
  <section v-if="snapshot" class="overview-grid">
    <article class="panel status-panel">
      <h3>{{ snapshot.displayName }}</h3>
      <p class="status-line">
        <strong>状态：</strong>{{ snapshot.status }}
      </p>
      <p><strong>地址：</strong>{{ snapshot.publicAddress }}</p>
      <p><strong>版本：</strong>{{ snapshot.gameVersion }}</p>
      <p><strong>在线玩家：</strong>{{ snapshot.onlinePlayerCount }}</p>
      <p v-if="snapshot.restartRecommended" class="restart-warning">
        资源状态已变化，建议重启服务器。
      </p>
      <p v-if="managerView && snapshot.rootDirectory"><strong>根目录：</strong>{{ snapshot.rootDirectory }}</p>
      <p v-if="managerView && snapshot.jvmArguments"><strong>JVM：</strong>{{ snapshot.jvmArguments }}</p>
    </article>

    <article class="panel">
      <h3>性能概览</h3>
      <div class="metric-grid">
        <div>
          <span>CPU</span>
          <strong>{{ snapshot.metrics.cpuUsagePercent.toFixed(1) }}%</strong>
        </div>
        <div>
          <span>内存</span>
          <strong>{{ snapshot.metrics.memoryUsedMb }} / {{ snapshot.metrics.memoryMaxMb }} MB</strong>
        </div>
        <div>
          <span>入站</span>
          <strong>{{ snapshot.metrics.networkInboundKbps.toFixed(1) }} KB/s</strong>
        </div>
        <div>
          <span>出站</span>
          <strong>{{ snapshot.metrics.networkOutboundKbps.toFixed(1) }} KB/s</strong>
        </div>
      </div>
    </article>

    <article class="panel">
      <h3>在线玩家</h3>
      <ul v-if="snapshot.onlinePlayers.length" class="tag-list">
        <li v-for="player in snapshot.onlinePlayers" :key="player.name">{{ player.name }}</li>
      </ul>
      <p v-else class="empty-text">当前没有在线玩家。</p>
    </article>

    <article class="panel">
      <h3>聊天消息</h3>
      <ul class="log-list">
        <li v-for="entry in snapshot.chatMessages" :key="entry.id">
          <time>{{ new Date(entry.timestamp).toLocaleString() }}</time>
          <span>{{ entry.message }}</span>
        </li>
      </ul>
    </article>

    <article class="panel">
      <h3>模组</h3>
      <ul v-if="snapshot.mods.length" class="asset-list">
        <li v-for="asset in snapshot.mods" :key="asset.id">
          <span>{{ asset.name }}</span>
          <strong>{{ asset.enabled ? '启用' : '停用' }}</strong>
        </li>
      </ul>
      <p v-else class="empty-text">没有发现模组。</p>
    </article>

    <article class="panel">
      <h3>数据包</h3>
      <ul v-if="snapshot.datapacks.length" class="asset-list">
        <li v-for="asset in snapshot.datapacks" :key="asset.id">
          <span>{{ asset.name }}</span>
          <strong>{{ asset.enabled ? '启用' : '停用' }}</strong>
        </li>
      </ul>
      <p v-else class="empty-text">没有发现数据包。</p>
    </article>

    <article class="panel">
      <h3>资源包</h3>
      <ul v-if="snapshot.resourcePacks.length" class="asset-list">
        <li v-for="asset in snapshot.resourcePacks" :key="asset.id">
          <span>{{ asset.name }}</span>
          <strong>{{ asset.enabled ? '启用' : '停用' }}</strong>
        </li>
      </ul>
      <p v-else class="empty-text">没有发现资源包。</p>
    </article>
  </section>

  <section v-else class="empty-state">
    <h3>暂无服务器数据</h3>
    <p>请稍后刷新，或先启动服务器后再查看。</p>
  </section>
</template>

<style scoped>
.overview-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 18px;
}

.panel,
.empty-state {
  border: 2px solid rgba(91, 143, 90, 0.26);
  border-radius: 28px;
  padding: 20px;
  background: rgba(255, 252, 243, 0.82);
  box-shadow: 0 18px 48px rgba(43, 76, 40, 0.08);
}

.panel h3,
.empty-state h3,
.panel p,
.empty-state p {
  margin: 0;
}

.panel {
  display: grid;
  gap: 14px;
}

.status-panel {
  grid-column: span 4;
}

.restart-warning {
  border-radius: 14px;
  padding: 10px 12px;
  background: rgba(214, 148, 36, 0.14);
  color: #8f5f16;
  font-weight: 800;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.metric-grid div,
.asset-list li,
.log-list li {
  border-radius: 18px;
  padding: 12px 14px;
  background: rgba(236, 245, 223, 0.72);
}

.metric-grid span,
.log-list time {
  display: block;
  color: #6e7b65;
  font-size: 12px;
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
  background: #5b8f5a;
  color: #fffdf3;
  font-weight: 700;
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

.empty-text {
  color: #6e7b65;
}

.panel:nth-child(2),
.panel:nth-child(4) {
  grid-column: span 8;
}

.panel:nth-child(3),
.panel:nth-child(5),
.panel:nth-child(6),
.panel:nth-child(7) {
  grid-column: span 4;
}

@media (max-width: 1100px) {
  .status-panel,
  .panel:nth-child(2),
  .panel:nth-child(3),
  .panel:nth-child(4),
  .panel:nth-child(5),
  .panel:nth-child(6),
  .panel:nth-child(7) {
    grid-column: span 12;
  }
}
</style>
