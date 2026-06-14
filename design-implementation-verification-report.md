# Design Implementation Verification Report

Date: 2026-06-13

Reference document: `design-complete.md`

Decision supplement: `decisions-for-report.md`

## 1. Scope and method

This report compares the current repository implementation against `design-complete.md`, adjusted by the explicit answers in `decisions-for-report.md`.

Assessment method:

- `FULL`: implemented and aligned with the design intent or the accepted decision supplement.
- `PARTIAL`: implemented in a limited, weaker, or materially different way.
- `MISSING`: not implemented or not evidenced in the current repository.
- `SUPERSEDED`: the original design item was intentionally removed or changed by `decisions-for-report.md`; it is not counted as an implementation gap.

Estimated decision-adjusted overall matching degree: `98%`

This is an engineering estimate, not a formal compliance score. The remaining known non-decision gap is HTTPS-only deployment enforcement in the sample Nginx configuration.

## 2. Build and validation status

- Frontend build status: `PASS`
  - Verified with `pnpm build` after redirecting `HOME`, `USERPROFILE`, `LOCALAPPDATA`, `TEMP`, and `TMP` to repo-local paths.
- Backend compile status: `PASS`
  - Verified with `mvn "-Dmaven.repo.local=D:\development-projects\personal-projects\minecraft-server-manager\.m2\repository" -o compile`.
- Backend test status: `PASS`
  - Verified with `mvn "-Dmaven.repo.local=D:\development-projects\personal-projects\minecraft-server-manager\.m2\repository" -o test` after setting `MAVEN_OPTS=-Djava.io.tmpdir=D:\development-projects\personal-projects\minecraft-server-manager\.codex-temp` and redirecting `TEMP`/`TMP` to the repo-local temp directory.

## 3. Category summary

| Category | Estimated decision-adjusted match |
| --- | --- |
| Product goals | 100% |
| Visitor goals | 100% |
| Manager goals | 100% |
| Routing, auth, authorization, API rules | 100% |
| Backend/runtime/logging | 100% |
| Architecture, storage, deployment, conventions | 94% |

## 4. Goal-by-goal assessment

### 4.1 Product goals

| Goal | Status | Match | Current situation |
| --- | --- | --- | --- |
| Manage multiple Minecraft servers from one web application | FULL | 100% | Multiple servers are modeled in `server_config`, routed by `serverId`, listed by `ServerCatalogService`, and rendered in frontend server pages. |
| Expose safe public information to visitors | FULL | 100% | Public APIs are under `/api/public/**`; public snapshots exclude root directory, JVM args, custom commands, and full logs. |
| Expose administrative controls only to authenticated managers | FULL | 100% | Manager APIs require authentication through Spring Security and `ManagerAuthenticationFilter`. |
| Keep deployment simple and low-cost | FULL | 100% | Single frontend, single backend, SQLite, polling, local Nginx reverse proxy config. |
| Use a design suitable for a small number of servers and viewers | FULL | 100% | Implementation is intentionally lightweight and not multi-tenant. |

### 4.2 Visitor goals

| Goal | Status | Match | Current situation |
| --- | --- | --- | --- |
| View online players | FULL | 100% | Player list is exposed in public snapshots and rendered in `ServerOverview.vue`. |
| View loaded mods | FULL | 100% | `ServerAssetService.listMods()` scans the managed server `mods` directory and public snapshots expose the result. |
| View loaded datapacks | FULL | 100% | `ServerAssetService.listDatapacks()` scans `world/datapacks` and public snapshots expose the result. |
| View server game version | FULL | 100% | `gameVersion` is stored in `server_config`, returned by snapshot APIs, and shown in the UI. |
| View server chat messages | FULL | 100% | Chat lines are parsed from server output and public snapshots include only filtered chat entries. |
| View server performance data: CPU, memory, network speed | SUPERSEDED | N/A | Removed per answers 1 and 6. Backend metrics DTO/service/runtime-state fields were removed, snapshots no longer contain `metrics`, and the frontend performance panel was removed. |
| Visitors cannot perform any control action | FULL | 100% | Public API surface is read-only; control APIs are manager-only. |

### 4.3 Manager goals

| Goal | Status | Match | Current situation |
| --- | --- | --- | --- |
| Use all visitor features | FULL | 100% | Manager snapshots contain the visitor snapshot data plus manager-only fields. |
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
| Restart server after asset state changes | FULL | 100% | Retained per answer 2: asset changes set `restartRecommended` and the UI prompts managers to restart manually; the program layer does not auto-restart servers. |
| Define server-specific custom commands | FULL | 100% | Custom commands are persisted per server in SQLite with display name, command text, and a UI remark field. |
| Execute server-specific custom commands | FULL | 100% | Retained per answer 5: managers may execute arbitrary server console commands, including saved commands invoked later from the command list. |
| Configure JVM parameters for each server | FULL | 100% | Config form updates `jvmArguments`, and process launch uses those arguments. |
| Add a new managed server by specifying its server root directory | FULL | 100% | `CreateManagedServerRequest` and `/api/manager/servers` support this flow. |

### 4.4 Routing, authentication, authorization, and API rules

| Goal | Status | Match | Current situation |
| --- | --- | --- | --- |
| Route model includes `serverId` and client type | FULL | 100% | Frontend route is `/servers/:serverId/:clientType(visitor|manager)`. |
| Manager directory route exists | FULL | 100% | Frontend includes `/servers` as the manager directory page. |
| Route structure is not a security boundary | FULL | 100% | Backend auth is enforced independently of route shape. |
| Every manager API validates authentication | FULL | 100% | All non-public APIs require authentication under Spring Security. |
| Every manager API validates target `serverId` where applicable | FULL | 100% | Service methods validate target server IDs before acting. |
| Visitor APIs expose only public-safe data | FULL | 100% | Public snapshots omit manager-only fields and there is no public log endpoint. |
| Manager features are not protected only by hidden frontend UI | FULL | 100% | Backend enforcement is present; frontend hiding is only UX. |
| Manager access requires 2FA | FULL | 100% | Login requires username, password, and TOTP code; registration provisions TOTP. |
| Backend creates a session-bound authentication token | FULL | 100% | Answers 3 and 4 supersede HTTP-session binding. The backend remains stateless, creates a persisted expiring bearer token, and does not couple tokens to an HTTP session lifecycle. |
| Each manager has an exclusive token | FULL | 100% | Existing tokens for the username are revoked on login before a new token is issued. |
| Logout invalidates the current token | FULL | 100% | Backend `/api/manager/auth/logout` deletes the current token record; frontend logout calls it and clears local token/server state. |
| If the session changes or becomes invalid, the token is deprecated | FULL | 100% | Answers 3 and 4 keep the current token-expiration/deletion model. Expired, deleted, or missing tokens are treated as unauthenticated and require login again. |
| After invalidation, manager must log in again | FULL | 100% | Expired or deleted tokens return unauthenticated and the frontend clears local auth state. |
| State-changing manager APIs exist for power, player actions, messages, assets, and custom commands | FULL | 100% | API surface matches the design categories. |
| Backend validates command identity where applicable | FULL | 100% | Answer 5 accepts arbitrary manager-entered commands. CRUD still validates saved command ownership by `serverId`; freeform execution validates manager auth, target server, and nonblank command text before writing only to the Minecraft console. |

### 4.5 Backend, runtime, logging, and monitoring

| Goal | Status | Match | Current situation |
| --- | --- | --- | --- |
| Authentication and authorization handled by backend | FULL | 100% | Implemented by Spring Security, auth services, and persisted token records. |
| Server discovery and configuration lookup | FULL | 100% | Config is stored in SQLite and bootstrapped from `application.yaml`. |
| Polling or collecting runtime information | FULL | 100% | Frontend polls snapshots; backend scheduled refresh triggers player-list updates. Performance metric collection was intentionally removed by answers 1 and 6. |
| Reading server output | FULL | 100% | `ServerProcessService` reads process stdout and parses lines. |
| Sending commands to Minecraft server consoles | FULL | 100% | Commands are written to the managed process stdin. |
| Persisting manager auth data | FULL | 100% | Manager users and bearer-token records are stored in SQLite. |
| Persisting custom command metadata | FULL | 100% | `custom_command` table stores per-server command metadata and remarks. |
| Full logs are manager-only | FULL | 100% | Only manager APIs expose logs, and those logs are assembled from `logs/latest.log` and rotated `*.log.gz` files. |
| Visitor chat is filtered from server output | FULL | 100% | Chat parsing is separate from full log display. |
| CPU usage reflects server runtime | SUPERSEDED | N/A | Removed per answers 1 and 6 together with all performance metric UI/API/runtime logic. |
| Memory usage reflects server runtime | SUPERSEDED | N/A | Removed per answers 1 and 6 together with all performance metric UI/API/runtime logic. |
| Network speed reflects server runtime | SUPERSEDED | N/A | Removed per answers 1 and 6 together with all performance metric UI/API/runtime logic. |
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
| Reverse proxy uses Nginx | FULL | 100% | `deploy/nginx.conf` and `deploy/README.md` are present. |
| HTTPS termination is supported | FULL | 100% | Nginx deployment guidance includes a TLS-enabled server block. |
| Serve application only over HTTPS | PARTIAL | 50% | Deployment guidance includes HTTPS, but the sample Nginx config keeps the TLS block commented until real certificates are available. |
| Code comments and annotations use English | FULL | 100% | Current backend code and frontend source comments follow this convention. |
| UI text uses Chinese | FULL | 100% | Verified frontend-owned Vue titles, subtitles, placeholders, buttons, modal copy, and seeded custom command labels/descriptions. Technical abbreviations such as `JVM`, `TOTP`, `QR`, and `OP` are intentionally retained. |

## 5. Decision outcomes and remaining risks

### Accepted decision outcomes

1. Performance metrics were removed.
   - The backend no longer contains `ServerMetricsService`, `ServerRuntimeMetrics`, or `ServerMetricsDto`.
   - `ServerSnapshotDto` and frontend `ServerSnapshot` no longer contain `metrics`.
   - `ServerOverview.vue` no longer renders CPU, memory, or network speed.

2. Asset changes prompt restart but do not auto-restart.
   - Asset suspend/resume still marks `restartRecommended`.
   - `ServerOverview.vue` and `ManagerControlPanel.vue` expose the restart prompt and manual restart control.

3. Auth remains token-based and stateless.
   - The backend keeps token generation, expiry, exclusive-token login, and logout invalidation.
   - Spring Security remains `SessionCreationPolicy.STATELESS`; no HTTP session lifecycle is required.

4. Custom commands remain freeform console commands.
   - Managers can enter arbitrary server console commands.
   - Saved commands persist command text plus a UI remark for later invocation.

5. Frontend UI copy is Chinese.
   - Directory, login, registration, workspace, overview, manager controls, create-server modal, and seeded command labels/descriptions were verified.

### Remaining non-decision risk

1. HTTPS-only serving is still a deployment-policy gap.
   - The deployment docs support Nginx HTTPS termination, but the sample `deploy/nginx.conf` keeps the TLS block commented until real certificates are available.

## 6. Final conclusion

After applying `decisions-for-report.md`, the implementation aligns with the accepted product shape: public visitor data, manager-only controls, 2FA manager login, token logout, manual restart prompts after asset changes, freeform/saved Minecraft console commands, and Chinese frontend UI copy.

The original runtime performance metrics requirement is intentionally removed rather than partially implemented. With that decision applied, the main remaining design-level caveat is production HTTPS enforcement in deployment configuration.
