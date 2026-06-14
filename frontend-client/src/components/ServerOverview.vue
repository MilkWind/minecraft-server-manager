<script setup lang="ts">
import { computed } from 'vue';
import { Card, Icon, Collapse, Tooltip } from 'animal-island-vue';
import type { ServerSnapshot } from '@/types/api';

const props = defineProps<{
  snapshot: ServerSnapshot | null;
  managerView?: boolean;
}>();

const chatCount = computed(() => props.snapshot?.chatMessages?.length ?? 0);
const playerCount = computed(() => props.snapshot?.onlinePlayers?.length ?? 0);
const modCount = computed(() => props.snapshot?.mods?.length ?? 0);
const datapackCount = computed(() => props.snapshot?.datapacks?.length ?? 0);

function statusTooltip(status: string): string {
  const s = status.toLowerCase();
  if (s === 'running') return '服务器正在运行，玩家可以连接。';
  if (s === 'stopped') return '服务器已停止，玩家无法连接。';
  if (s === 'starting') return '服务器正在启动中，请稍候。';
  if (s === 'restarting') return '服务器正在重启，短暂中断后恢复。';
  if (s === 'error') return '服务器运行异常，请检查日志。';
  return `当前服务器状态：${status}`;
}
</script>

<template>
  <section v-if="snapshot" class="overview-grid">
    <Card
      v-if="snapshot.restartRecommended"
      color="app-orange"
      type="dashed"
      class="restart-banner"
    >
      <div class="restart-content">
        <Icon name="icon-helicopter" :size="22" />
        <div>
          <strong>建议重启服务器</strong>
          <p>资源状态已变化，建议执行重启以完整应用更改。</p>
        </div>
      </div>
    </Card>

    <Card color="app-teal" class="panel status-panel">
      <h3>
        <Icon name="icon-map" class="panel-icon" />
        {{ snapshot.displayName }}
      </h3>
      <p class="status-line">
        <Tooltip :title="statusTooltip(snapshot.status)">
          <span><strong>状态：</strong>{{ snapshot.status }}</span>
        </Tooltip>
      </p>
      <p><strong>地址：</strong>{{ snapshot.publicAddress }}</p>
      <p><strong>版本：</strong>{{ snapshot.gameVersion }}</p>
      <p><strong>在线玩家：</strong>{{ snapshot.onlinePlayerCount }}</p>
      <p v-if="managerView && snapshot.rootDirectory"><strong>根目录：</strong>{{ snapshot.rootDirectory }}</p>
      <p v-if="managerView && snapshot.jvmArguments"><strong>JVM：</strong>{{ snapshot.jvmArguments }}</p>
    </Card>

    <Card color="app-yellow" class="panel players-panel">
      <h3>
        <Icon name="icon-chat" class="panel-icon" />
        在线玩家
      </h3>
      <Collapse
        v-if="playerCount > 5"
        :question="`在线玩家 (${playerCount})`"
        :default-expanded="true"
      >
        <ul class="tag-list">
          <li v-for="player in snapshot.onlinePlayers" :key="player.name">{{ player.name }}</li>
        </ul>
      </Collapse>
      <ul v-else-if="playerCount > 0" class="tag-list">
        <li v-for="player in snapshot.onlinePlayers" :key="player.name">{{ player.name }}</li>
      </ul>
      <p v-else class="empty-text">当前没有在线玩家。</p>
    </Card>

    <Card color="app-blue" class="panel chat-panel">
      <h3>
        <Icon name="icon-camera" class="panel-icon" />
        聊天消息
      </h3>
      <Collapse
        v-if="chatCount > 5"
        :question="`聊天消息 (${chatCount})`"
        :default-expanded="false"
      >
        <ul class="log-list">
          <li v-for="entry in snapshot.chatMessages" :key="entry.id">
            <time>{{ new Date(entry.timestamp).toLocaleString() }}</time>
            <span>{{ entry.message }}</span>
          </li>
        </ul>
      </Collapse>
      <ul v-else-if="chatCount > 0" class="log-list">
        <li v-for="entry in snapshot.chatMessages" :key="entry.id">
          <time>{{ new Date(entry.timestamp).toLocaleString() }}</time>
          <span>{{ entry.message }}</span>
        </li>
      </ul>
      <p v-else class="empty-text">暂无聊天消息。</p>
    </Card>

    <Card color="lime-green" class="panel mods-panel">
      <h3>
        <Icon name="icon-design" class="panel-icon" />
        模组
      </h3>
      <Collapse
        v-if="modCount > 5"
        :question="`模组 (${modCount})`"
        :default-expanded="false"
      >
        <ul class="asset-list">
          <li v-for="asset in snapshot.mods" :key="asset.id">
            <span>{{ asset.name }}</span>
            <strong>{{ asset.enabled ? '启用' : '停用' }}</strong>
          </li>
        </ul>
      </Collapse>
      <ul v-else-if="modCount > 0" class="asset-list">
        <li v-for="asset in snapshot.mods" :key="asset.id">
          <span>{{ asset.name }}</span>
          <strong>{{ asset.enabled ? '启用' : '停用' }}</strong>
        </li>
      </ul>
      <p v-else class="empty-text">没有发现模组。</p>
    </Card>

    <Card color="yellow-green" class="panel datapacks-panel">
      <h3>
        <Icon name="icon-variant" class="panel-icon" />
        数据包
      </h3>
      <Collapse
        v-if="datapackCount > 5"
        :question="`数据包 (${datapackCount})`"
        :default-expanded="false"
      >
        <ul class="asset-list">
          <li v-for="asset in snapshot.datapacks" :key="asset.id">
            <span>{{ asset.name }}</span>
            <strong>{{ asset.enabled ? '启用' : '停用' }}</strong>
          </li>
        </ul>
      </Collapse>
      <ul v-else-if="datapackCount > 0" class="asset-list">
        <li v-for="asset in snapshot.datapacks" :key="asset.id">
          <span>{{ asset.name }}</span>
          <strong>{{ asset.enabled ? '启用' : '停用' }}</strong>
        </li>
      </ul>
      <p v-else class="empty-text">没有发现数据包。</p>
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

.restart-banner {
  grid-column: span 12;
}

.restart-content {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.restart-content strong {
  display: block;
  color: #8f5f16;
  font-size: 16px;
}

.restart-content p {
  margin: 6px 0 0;
  color: #8f5f16;
  font-size: 14px;
}

.status-panel {
  grid-column: span 12;
}

.players-panel {
  grid-column: span 4;
}

.chat-panel {
  grid-column: span 4;
}

.mods-panel {
  grid-column: span 3;
}

.datapacks-panel {
  grid-column: span 3;
}

.panel-icon {
  vertical-align: middle;
  margin-right: 6px;
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
  .players-panel,
  .chat-panel,
  .mods-panel,
  .datapacks-panel {
    grid-column: span 12;
  }
}
</style>
