# Frontend Client

## UI Guidelines
When building UI, use **animal-island-vue** components primarily.

## Usage Guide
Refer to `.codex\skills\animal-island-vue-style\AI_USAGE.md` for component list, API, and rules.

Key rules:
- Import `import 'animal-island-vue/style'` once in `main.ts`
- Use `v-model` / `v-model:open` / `v-model:expanded` (Vue conventions, not React)
- Never import from deep paths like `animal-island-vue/dist/...`
- Button icons go in `#icon` slot
- Select is controlled-only — both `v-model` and `options` required

## Commands
- `pnpm dev` — start dev server
- `pnpm build` — type-check + build
- `pnpm preview` — serve production build
