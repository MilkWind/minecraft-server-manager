<script setup lang="ts">
import { Card, Divider, Table, type TableColumn, Typewriter } from 'animal-island-vue'
import type { ManagedAsset, Player, ServerMetrics, ServerSnapshot } from '../types/api'

defineProps<{
  snapshot: ServerSnapshot
}>()

type PlayerRow = {
  id: string
  name: string
  role: string
  latency: string
}

type AssetRow = {
  id: string
  name: string
  type: string
  enabled: string
}

const playerColumns: TableColumn<PlayerRow>[] = [
  { title: '玩家', dataIndex: 'name' },
  { title: '身份', dataIndex: 'role' },
  { title: '延迟', dataIndex: 'latency' },
]

const assetColumns: TableColumn<AssetRow>[] = [
  { title: '名称', dataIndex: 'name' },
  { title: '类型', dataIndex: 'type' },
  { title: '状态', dataIndex: 'enabled' },
]

function buildPlayerRows(players: Player[]): PlayerRow[] {
  return players.map((player) => ({
    id: player.name,
    name: player.name,
    role: player.operator ? 'OP' : '玩家',
    latency: `${player.latencyMs} ms`,
  }))
}

function buildMetrics(metrics: ServerMetrics) {
  return [
    { key: 'cpu', label: 'CPU 使用率', value: `${metrics.cpuUsagePercent.toFixed(1)}%` },
    { key: 'memory', label: '内存占用', value: `${metrics.usedMemoryMb} / ${metrics.maxMemoryMb} MB` },
    { key: 'down', label: '下行速率', value: `${metrics.networkDownKbps.toFixed(1)} kbps` },
    { key: 'up', label: '上行速率', value: `${metrics.networkUpKbps.toFixed(1)} kbps` },
  ]
}

function buildAssetRows(assets: ManagedAsset[]): AssetRow[] {
  return assets.map((asset) => ({
    id: asset.id,
    name: asset.name,
    type: asset.type,
    enabled: asset.enabled ? '启用' : '停用',
  }))
}
</script>

<template>
  <div class="overview-grid">
    <Card class="panel-card">
      <h2>{{ snapshot.displayName }}</h2>
      <Typewriter :auto-play="false" :text="`版本 ${snapshot.gameVersion} · 地址 ${snapshot.publicAddress}`" />
      <div class="metric-grid">
        <div v-for="metric in buildMetrics(snapshot.metrics)" :key="metric.key" class="metric-card">
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
        </div>
      </div>
    </Card>

    <Card class="panel-card">
      <h3>在线玩家</h3>
      <Table
        :columns="playerColumns"
        :data-source="buildPlayerRows(snapshot.onlinePlayers)"
        row-key="id"
        empty-text="当前无人在线"
      />
    </Card>

    <Card class="panel-card">
      <h3>聊天频道</h3>
      <div class="chat-list">
        <p v-for="entry in snapshot.chatMessages" :key="entry.id">{{ entry.message }}</p>
      </div>
    </Card>

    <Card class="panel-card">
      <h3>模组</h3>
      <Table :columns="assetColumns" :data-source="buildAssetRows(snapshot.mods)" row-key="id" />
    </Card>

    <Card class="panel-card">
      <h3>数据包</h3>
      <Table
        :columns="assetColumns"
        :data-source="buildAssetRows(snapshot.datapacks)"
        row-key="id"
      />
    </Card>

    <Card class="panel-card">
      <h3>资源包</h3>
      <Table
        :columns="assetColumns"
        :data-source="buildAssetRows(snapshot.resourcePacks)"
        row-key="id"
      />
    </Card>
  </div>

  <Divider />
</template>
