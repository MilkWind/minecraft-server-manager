<script setup lang="ts">
import { Footer, Time, Title, Divider} from 'animal-island-vue';
import type { IconName } from 'animal-island-vue';

defineProps<{
  title: string;
  subtitle?: string;
  managerMode?: boolean;
  icon?: IconName;
}>();
</script>

<template>
  <div class="app-shell">
    <header class="hero">
      <div class="hero-copy">
        <p class="eyebrow">{{ managerMode ? '管理控制台' : '访客视图' }}</p>
        <div class="title-row">
          <Title size="large" color="app-green">{{ title }}</Title>
        </div>
      </div>
      <div class="hero-tools">
        <Time />
        <slot name="header-actions" />
      </div>
    </header>

    <Divider type="line-brown" class="hero-divider" />

    <main class="content">
      <slot />
    </main>

    <Footer type="tree" class="app-footer" />
  </div>
</template>

<style scoped>
.app-shell {
  position: relative;
  isolation: isolate;
  overflow: hidden;
  min-height: 100vh;
  padding: 24px;
  background:
    radial-gradient(circle at 12% 14%, rgba(255, 204, 0, 0.18) 0 72px, transparent 74px),
    radial-gradient(circle at 82% 10%, rgba(25, 200, 185, 0.16) 0 120px, transparent 122px),
    radial-gradient(circle at 18% 72%, rgba(245, 195, 28, 0.14) 0 160px, transparent 162px),
    linear-gradient(180deg, #dff6e8 0%, #f8f8f0 46%, #f0e8d8 100%);
  color: var(--animal-text-color);
  font-family: var(--animal-font-family);
}

.app-shell::after {
  position: absolute;
  pointer-events: none;
  content: '';
}

.app-shell::after {
  inset: 0;
  z-index: -1;
  background:
    radial-gradient(ellipse at center, rgba(114, 93, 66, 0.13) 0 2px, transparent 3px) 0 0 / 34px 34px,
    radial-gradient(ellipse at center, rgba(25, 200, 185, 0.15) 0 5px, transparent 6px) 10px 12px / 86px 74px,
    linear-gradient(115deg, transparent 0 46%, rgba(255, 255, 255, 0.2) 47% 53%, transparent 54% 100%);
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.85), rgba(0, 0, 0, 0.18));
}

.hero {
  position: relative;
  z-index: 2;
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-start;
  margin: 0 auto 0;
  max-width: 1240px;
}

.hero-copy {
  display: grid;
  gap: 10px;
  flex: 1;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.eyebrow {
  color: var(--animal-primary-color);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  margin: 0;
}

.hero-tools {
  display: grid;
  justify-items: end;
  gap: 14px;
  flex-shrink: 0;
}

.hero-divider {
  position: relative;
  z-index: 2;
  margin: 18px auto 24px;
  max-width: 1240px;
}

.content {
  position: relative;
  z-index: 2;
  margin: 0 auto;
  max-width: 1240px;
  padding-bottom: 130px;
}

.app-footer {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 1;
  pointer-events: none;
}

@media (max-width: 900px) {
  .app-shell {
    padding: 16px;
    background:
      radial-gradient(circle at 18% 10%, rgba(255, 204, 0, 0.18) 0 58px, transparent 60px),
      radial-gradient(circle at 88% 18%, rgba(25, 200, 185, 0.16) 0 96px, transparent 98px),
      linear-gradient(180deg, #dff6e8 0%, #f8f8f0 52%, #f0e8d8 100%);
  }

  .hero {
    flex-direction: column;
  }

  .hero-tools {
    width: 100%;
    justify-items: start;
  }

  .hero-divider {
    margin: 14px auto 20px;
  }
}
</style>
