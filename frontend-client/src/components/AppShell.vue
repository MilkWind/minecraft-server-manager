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

.app-shell::before,
.app-shell::after {
  position: absolute;
  pointer-events: none;
  content: '';
}

.app-shell::before {
  right: -12%;
  bottom: -22vh;
  left: -12%;
  z-index: -2;
  height: 48vh;
  background:
    radial-gradient(85% 95% at 10% 22%, rgba(111, 186, 44, 0.36) 0 42%, transparent 43%),
    radial-gradient(92% 90% at 54% 12%, rgba(130, 213, 187, 0.44) 0 44%, transparent 45%),
    radial-gradient(86% 88% at 90% 24%, rgba(209, 218, 73, 0.28) 0 42%, transparent 43%);
  filter: drop-shadow(0 -10px 24px rgba(61, 52, 40, 0.06));
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

  .app-shell::before {
    bottom: -26vh;
    height: 40vh;
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
