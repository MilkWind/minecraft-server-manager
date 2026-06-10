# Design Implementation Verification Report

Date: 2026-06-09

Reference document: `design-complete.md`

## 1. Scope and method

This report compares the current repository implementation against the design record in `design-complete.md`.

Assessment method:

- `FULL`: implemented and aligned with the design intent.
- `PARTIAL`: implemented in a limited, weaker, or materially different way.
- `MISSING`: not implemented or not evidenced in the current repository.

Estimated scoring rule:

- `FULL` = 100%
- `PARTIAL` = 50%
- `MISSING` = 0%

Estimated overall matching degree: `83%`

This is an engineering estimate, not a formal compliance score.

## 2. Build and validation status

- Frontend build status: `PASS`
  - Verified with `pnpm build` after redirecting `HOME`, `USERPROFILE`, `LOCALAPPDATA`, `TEMP`, and `TMP` to repo-local paths.
- Backend compile status: `PASS`
  - Verified with `mvn "-Dmaven.repo.local=../.m2/repository" -o compile`.
- Backend test status: `NOT FULLY VERIFIED`
  - `mvn test` could not complete because the sandbox blocks network access and the local Maven cache does not contain all required Surefire dependencies.

## 3. Category summary

| Category | Estimated match |
| --- | --- |
| Product goals | 100% |
| Visitor goals | 93% |
| Manager goals | 88% |
| Routing, auth, authorization, API rules | 79% |
| Backend/runtime/logging/metrics | 69% |
| Architecture, storage, deployment, conventions | 87% |

## 4. Goal-by-goal assessment

### 4.1 Product goals

| Goal | Status | Match | Current situation |
| --- | --- | --- | --- |
| Manage multiple Minecraft servers from one web application | FULL | 100% | Multiple servers are modeled in `server_config`, routed by `serverId`, listed by `ServerCatalogService`, and rendered in frontend server pages. |
| Expose safe public information to visitors | FULL | 100% | Public APIs are under `/api/public/**`; public snapshot excludes root directory, JVM args, custom commands, and full logs. |
| Expose administrative controls only to authenticated managers | FULL | 100% | Manager APIs require authentication through Spring Security and `ManagerAuthenticationFilter`. |
| Keep deployment simple and low-cost | FULL | 100% | Single frontend, single backend, SQLite, polling, local Caddy reverse proxy config. |
| Use a design suitable for a small number of servers and viewers | FULL | 100% | Implementation is intentionally lightweight and not multi-tenant. |

### 4.2 Visitor goals

| Goal | Status | Match | Current situation |
| --- | --- | --- | --- |
| View online players | FULL | 100% | Player list is exposed in public snapshot and rendered in `ServerOverview.vue`. |
| View loaded mods | FULL | 100% | `ServerAssetService.listMods()` scans the managed server `mods` directory and public snapshot exposes the result. |
| View loaded datapacks | FULL | 100% | `ServerAssetService.listDatapacks()` scans `world/datapacks` and public snapshot exposes the result. |
| View server game version | FULL | 100% | `gameVersion` is stored in `server_config`, returned by snapshot APIs, and shown in the UI. |
| View server chat messages | FULL | 100% | Chat lines are parsed from server output and public snapshot includes only filtered chat entries. |
| View server performance data: CPU, memory, network speed | PARTIAL | 50% | UI and API exist, but memory is measured from the backend JVM instead of the Minecraft process, and network speed is not based on real traffic volume. |
| Visitors cannot perform any control action | FULL | 100% | Public API surface is read-only; control APIs are manager-only. |

### 4.3 Manager goals

| Goal | Status | Match | Current situation |
| --- | --- | --- | --- |
| Use all visitor features | FULL | 100% | Manager snapshot contains the visitor snapshot data plus manager-only fields. |
| View full server logs | FULL | 100% | Manager logs are read from `logs/latest.log` and the rotated `*.log.gz` history through `ServerLogService`. |
| One-click copy for logs | FULL | 100% | `ManagerControlPanel.vue` provides clipboard copy for the current log list. |
| Start server | FULL | 100% | Implemented by `/power/start` and `ServerProcessService.start()`. |
| Stop server | FULL | 100% | Implemented by `/power/stop` and `ServerProcessService.stop()`. |
| Restart server | FULL | 100% | Implemented by `/power/restart` and `ServerProcessService.restart()`. |
| OP players | FULL | 100% | Implemented by `/players/op` and console dispatch. |
| DEOP players | FULL | 100% | Implemented by `/players/deop` and console dispatch. |
| Ban players | FULL | 100% | Implemented by `/players/ban` and console dispatch. |
| Send messages to all players | FULL | 100% | Implemented by `/messages` with `say`. |
| Send messages to a specific player | FULL | 100% | Implemented by `/messages` with `msg <player>`. |
| Suspend mods and datapacks | FULL | 100% | Implemented by moving assets into `_manager_disabled` folders. |
| Resume mods and datapacks | FULL | 100% | Implemented by moving assets back from `_manager_disabled`. |
| Restart server after asset state changes | PARTIAL | 50% | The system marks `restartRecommended` and exposes restart controls, but restart is not automatic or enforced as part of the asset action flow. |
| Define server-specific custom commands | FULL | 100% | Custom commands are persisted per server in SQLite. |
| Execute server-specific custom commands | PARTIAL | 50% | The UI can execute saved commands, but execution goes through raw command text instead of validating a saved command identity on the backend. |
| Configure JVM parameters for each server | FULL | 100% | Config form updates `jvmArguments`, and process launch uses those arguments. |
| Add a new managed server by specifying its server root directory | FULL | 100% | `CreateManagedServerRequest` and `/api/manager/servers` support this flow. |

### 4.4 Routing, authentication, authorization, and API rules

| Goal | Status | Match | Current situation |
| --- | --- | --- | --- |
| Route model includes `serverId` and client type | FULL | 100% | Frontend route is `/servers/:serverId/:clientType(visitor|manager)`. |
| Manager directory route exists | FULL | 100% | Frontend includes `/servers` as the manager directory page. |
| Route structure is not a security boundary | FULL | 100% | Backend auth is enforced independently of route shape. |
| Every manager API validates authentication | FULL | 100% | All non-public APIs require authentication under Spring Security. |
| Every manager API validates target `serverId` where applicable | FULL | 100% | Service methods call `requireServerConfig(serverId)` before acting. |
| Visitor APIs expose only public-safe data | FULL | 100% | Public snapshot omits manager-only fields and there is no public log endpoint. |
| Manager features are not protected only by hidden frontend UI | FULL | 100% | Backend enforcement is present; frontend hiding is only UX. |
| Manager access requires 2FA | FULL | 100% | Login requires username, password, and TOTP code; registration provisions TOTP. |
| Backend creates a session-bound authentication token | PARTIAL | 50% | Backend creates a persisted bearer token with expiry, but the system is stateless and the token is not bound to an HTTP session lifecycle. |
| Each manager has an exclusive token | FULL | 100% | Existing sessions for the username are revoked on login before a new token is issued. |
| Logout invalidates the current token | FULL | 100% | Logout deletes the session record. |
| If the session changes or becomes invalid, the token is deprecated | PARTIAL | 50% | Token expiry and deletion are implemented, but there is no real session-bound invalidation model because the app uses stateless bearer auth. |
| After invalidation, manager must log in again | FULL | 100% | Expired or deleted tokens return unauthenticated and the frontend clears local session state. |
| State-changing manager APIs exist for power, player actions, messages, assets, and custom commands | FULL | 100% | API surface matches the design categories. |
| Backend validates command identity where applicable | PARTIAL | 50% | CRUD operations validate command ownership by `serverId`, but command execution uses freeform text instead of command ID based validation. |

### 4.5 Backend, runtime, logging, and metrics

| Goal | Status | Match | Current situation |
| --- | --- | --- | --- |
| Authentication and authorization handled by backend | FULL | 100% | Implemented by Spring Security, auth services, and manager session persistence. |
| Server discovery and configuration lookup | FULL | 100% | Config is stored in SQLite and bootstrapped from `application.yaml`. |
| Polling or collecting runtime information | FULL | 100% | Frontend polls snapshots; backend scheduled refresh triggers player list updates. |
| Reading server output | FULL | 100% | `ServerProcessService` reads process stdout and parses lines. |
| Sending commands to Minecraft server consoles | FULL | 100% | Commands are written to the managed process stdin. |
| Persisting manager auth data | FULL | 100% | Manager users and manager sessions are stored in SQLite. |
| Persisting custom command metadata | FULL | 100% | `custom_command` table stores per-server command metadata. |
| Full logs are manager-only | FULL | 100% | Only manager APIs expose logs, and those logs are assembled from `logs/latest.log` and rotated `*.log.gz` files. |
| Visitor chat is filtered from server output | FULL | 100% | Chat parsing is separate from full log display. |
| CPU usage reflects server runtime | FULL | 100% | CPU uses `ProcessHandle.info().totalCpuDuration()` for the managed server process. |
| Memory usage reflects server runtime | MISSING | 0% | Memory is currently read from the backend JVM `Runtime`, not from the managed Minecraft process. |
| Network speed reflects server runtime | MISSING | 0% | Current network calculation is based on interface metadata size, not real per-server traffic throughput. |
| Polling is conservative | FULL | 100% | Snapshot polling is 6-8 seconds and player-list refresh is 15 seconds by default. |
| Custom commands are restricted to Minecraft console input only | FULL | 100% | Command execution writes text to the Minecraft process only; there is no shell command execution path. |

### 4.6 Architecture, storage, deployment, and conventions

| Goal | Status | Match | Current situation |
| --- | --- | --- | --- |
| Frontend and backend are separated | FULL | 100% | Repository contains separate Vue and Spring Boot projects. |
| Frontend uses Vue | FULL | 100% | Implemented in `frontend-client`. |
| Frontend routing uses Vue Router | FULL | 100% | Implemented in `frontend-client/src/router/index.ts`. |
| Frontend UI library is `animal-island-vue` | FULL | 100% | Current UI imports and uses that library throughout the main views and components. |
| Backend uses Spring Boot | FULL | 100% | Implemented in `backend-server`. |
| Security core uses Spring Security with 2FA | FULL | 100% | Spring Security plus TOTP login flow are present. |
| ORM uses MyBatis Plus | FULL | 100% | Mappers and starter dependency are present. |
| Database uses SQLite | FULL | 100% | JDBC config and schema target SQLite. |
| Persistent storage covers manager auth, 2FA, custom commands, and server metadata | FULL | 100% | All of these are represented in the schema and active code paths. |
| Each server is started from its server `.jar` in the root directory | FULL | 100% | `ServerProcessService.discoverJar()` finds the jar under the configured root and starts it directly. |
| Managers specify server root directory when adding a server | FULL | 100% | Supported by server creation form and API. |
| JVM parameters are applied when launching the process | FULL | 100% | `buildCommand()` prepends configured JVM args to the Java command. |
| REST with polling is used instead of WebSocket/SSE | FULL | 100% | The implementation is REST polling only. |
| Reverse proxy uses Caddy | FULL | 100% | `deploy/Caddyfile` and `deploy/README.md` are present. |
| HTTPS termination is supported | FULL | 100% | Caddy deployment guidance expects HTTPS termination. |
| Serve application only over HTTPS | PARTIAL | 50% | Deployment guidance recommends HTTPS, but the provided Caddyfile defaults to `localhost` and does not itself enforce a production-only HTTPS deployment rule. |
| Code comments and annotations use English | FULL | 100% | Current backend code and most frontend source comments follow this convention. |
| UI text uses Chinese | PARTIAL | 50% | Some views use Chinese, but major parts of the directory, login, and registration flows still use English UI copy. |

## 5. Main mismatches and risks

### High-priority gaps

1. Runtime metrics are not design-complete.
   - Memory metrics show backend JVM memory, not Minecraft server memory.
   - Network speed is not calculated from real traffic.
   - This directly weakens both visitor and manager monitoring goals.

2. Authentication model does not fully match the design wording.
   - The design describes a session-bound token.
   - The implementation uses a stateless bearer token stored in SQLite with TTL.
   - This is secure enough for the current app shape, but it is not the same model.

### Medium-priority gaps

3. Custom command execution is not bound to stored command identity.
   - Saved commands are executed as raw command text.
   - Backend validation covers command CRUD ownership, but not "execute saved command X for server Y" as a distinct backend operation.

4. Asset change flow does not include an integrated restart step.
   - The system marks `restartRecommended` and lets the manager restart manually.
   - This is operationally useful, but weaker than a full "change then restart" workflow.

5. UI language convention is incomplete.
   - The design says UI text should use Chinese.
   - Several user-facing flows still contain English text.

## 6. Final conclusion

The current implementation is already strong on structure, route model, public-vs-manager separation, 2FA login, per-server management, server process control, asset toggling, and SQLite-backed configuration.

The repository is not yet fully aligned with `design-complete.md` in the areas that matter most for operational accuracy:

- metrics correctness
- exact auth-session model
- backend-validated execution of saved custom commands

If the target is "design-complete" rather than "feature-demo complete", those gaps should be treated as the remaining core work.
