<script setup lang="ts">
import { Card, Footer, Time } from 'animal-island-vue';

defineProps<{
  title: string;
  subtitle?: string;
  managerMode?: boolean;
}>();
</script>

<template>
  <div class="app-shell">
    <header class="hero">
      <Card type="title" class="hero-card">
        <div class="hero-copy">
          <p class="eyebrow">{{ managerMode ? 'Manager Console' : 'Visitor View' }}</p>
          <h1>{{ title }}</h1>
          <p v-if="subtitle">{{ subtitle }}</p>
        </div>
      </Card>
      <div class="hero-tools">
        <Time />
        <slot name="header-actions" />
      </div>
    </header>

    <main class="content">
      <slot />
    </main>

    <Footer type="tree" class="app-footer" />
  </div>
</template>

<style scoped>
.app-shell {
  position: relative;
  overflow: hidden;
  min-height: 100vh;
  padding: 24px;
  background:
    linear-gradient(180deg, rgba(247, 243, 223, 0.92), rgba(230, 249, 246, 0.78)),
    url('@/assets/hero.png') center top / cover fixed;
  color: var(--animal-text-color);
  font-family: var(--animal-font-family);
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-start;
  margin: 0 auto 24px;
  max-width: 1240px;
}

.hero-card {
  flex: 1;
}

.hero-copy {
  display: grid;
  gap: 8px;
}

.hero-copy h1,
.hero-copy p {
  margin: 0;
}

.hero-copy h1 {
  font-size: clamp(32px, 4vw, 50px);
  line-height: 1.04;
  color: var(--animal-warm-color-soft);
}

.hero-copy p {
  color: var(--animal-text-color-secondary);
  line-height: 1.6;
}

.eyebrow {
  color: var(--animal-primary-color);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.hero-tools {
  display: grid;
  justify-items: end;
  gap: 14px;
}

.content {
  position: relative;
  z-index: 1;
  margin: 0 auto;
  max-width: 1240px;
  padding-bottom: 130px;
}

.app-footer {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 0;
  pointer-events: none;
}

@media (max-width: 900px) {
  .app-shell {
    padding: 16px;
  }

  .hero {
    flex-direction: column;
  }

  .hero-tools {
    width: 100%;
    justify-items: start;
  }
}
</style>
