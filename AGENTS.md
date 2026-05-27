# Repository Guidelines

## Project Structure & Module Organization
This repository has two top-level apps:
- `backend-server/`: Spring Boot 4 backend (`src/main/java`, `src/main/resources`, `src/test/java`).
- `frontend-client/`: Vue 3 + TypeScript + Vite client (`src/`, `public/`).

Keep backend code under `minecraft.milkwind.manager` and group related classes by feature. Keep UI components in `frontend-client/src/components/` and shared assets in `frontend-client/src/assets/`.

## Build, Test, and Development Commands
Run commands from the matching subproject directory.
- `./mvnw test` in `backend-server/`: runs the Spring Boot test suite.
- `./mvnw spring-boot:run` in `backend-server/`: starts the API locally.
- `pnpm install` in `frontend-client/`: installs frontend dependencies.
- `pnpm dev` in `frontend-client/`: starts the Vite dev server.
- `pnpm build` in `frontend-client/`: type-checks and creates a production build.
- `pnpm preview` in `frontend-client/`: serves the production build locally.

## Coding Style & Naming Conventions
Use 4-space indentation in Java and the existing project formatter defaults in Vue/TypeScript files. Prefer descriptive package names, PascalCase for Java classes and Vue components, and camelCase for methods, variables, and composables. Keep filenames aligned with exported types, such as `ServerConfig.java` or `ServerStatus.vue`.

## Testing Guidelines
The backend currently uses JUnit 5 via `spring-boot-starter-test`; `BackendServerApplicationTests` is the baseline context test. Add new tests under `backend-server/src/test/java` and name them `*Test` or `*Tests`. The frontend has no automated test runner configured yet, so verify UI changes with `pnpm build` and manual browser checks.

## Commit & Pull Request Guidelines
Recent commits use short, lower-case prefixes such as `init fronetend and backend project` and `docs: add ...`. Follow the same style: concise, imperative, and scoped when useful. Pull requests should describe the change, list the affected app(s), and note any manual verification steps. Include screenshots for visible UI changes and link related issues when available.

## Configuration Notes
Do not commit secrets or local environment files. Backend runtime settings belong in `backend-server/src/main/resources/application.yaml`; frontend environment variables should stay in local, untracked files.
