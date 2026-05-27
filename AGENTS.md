# Repository Guidelines

## Project Structure & Module Organization
This repository has two top-level apps:
- `backend-server/`: Spring Boot 4 backend + Maven build system (`src/main/java`, `src/main/resources`, `src/test/java`).
- `frontend-client/`: Vue 3 + TypeScript + Vite client + Pnpm build system (`src/`, `public/`).

Keep backend code under `minecraft.milkwind.manager` and group related classes by feature. Keep UI components in `frontend-client/src/components/` and shared assets in `frontend-client/src/assets/`.

## Build, Test, and Development Commands
Run commands from the matching subproject directory.
- `./mvnw test` in `backend-server/`: runs the Spring Boot test suite.
- `./mvnw spring-boot:run` in `backend-server/`: starts the API locally.
- `pnpm build` in `frontend-client/`: type-checks and creates a production build.

## Coding Style & Naming Conventions
Use 4-space indentation in Java and the existing project formatter defaults in Vue/TypeScript files. Prefer descriptive package names, PascalCase for Java classes and Vue components, and camelCase for methods, variables, and composables. Keep filenames aligned with exported types, such as `ServerConfig.java` or `ServerStatus.vue`.

## Testing Guidelines
The backend currently uses JUnit 5 via `spring-boot-starter-test`; `BackendServerApplicationTests` is the baseline context test. Add new tests under `backend-server/src/test/java` and name them `*Test` or `*Tests`. The frontend has no automated test runner configured yet, so verify UI changes with `pnpm build` and manual browser checks.

## Configuration Notes
Do not commit secrets or local environment files. Backend runtime settings belong in `backend-server/src/main/resources/application.yaml`; frontend environment variables should stay in local, untracked files.

## Minecraft Commands Guidelines
For Minecraft commands, refer to the [Minecraft Commands](minecraft-commands.md) document.
