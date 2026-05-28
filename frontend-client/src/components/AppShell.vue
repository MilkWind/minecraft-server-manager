<script setup lang="ts">
defineProps<{
  title: string;
  subtitle?: string;
  managerMode?: boolean;
}>();
</script>

<template>
  <div class="app-shell">
    <header class="hero">
      <div class="hero-copy">
        <p class="eyebrow">{{ managerMode ? 'Manager Console' : 'Visitor View' }}</p>
        <h1>{{ title }}</h1>
        <p v-if="subtitle">{{ subtitle }}</p>
      </div>
      <slot name="header-actions" />
    </header>

    <main class="content">
      <slot />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  padding: 24px;
  background:
    radial-gradient(circle at top left, rgba(246, 222, 123, 0.6), transparent 32%),
    radial-gradient(circle at top right, rgba(115, 177, 112, 0.4), transparent 26%),
    linear-gradient(180deg, #f7f2d6 0%, #e8f6df 54%, #d7ecdf 100%);
  color: #304431;
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-start;
  margin: 0 auto 24px;
  max-width: 1240px;
  padding: 28px;
  border: 2px solid rgba(91, 143, 90, 0.34);
  border-radius: 32px;
  background: rgba(255, 252, 241, 0.76);
  box-shadow: 0 20px 60px rgba(39, 68, 36, 0.12);
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
}

.hero-copy p {
  color: #657364;
  line-height: 1.6;
}

.eyebrow {
  color: #5b8f5a;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.content {
  margin: 0 auto;
  max-width: 1240px;
}

@media (max-width: 900px) {
  .app-shell {
    padding: 16px;
  }

  .hero {
    flex-direction: column;
    padding: 22px;
  }
}
</style>
