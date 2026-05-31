# Minecraft Server Manager Detailed Implementation Plan

## 1. Current Baseline

The repository is no longer at an empty scaffold stage. The current implementation already includes:

- Spring Boot backend structure with feature-based packages
- Vue 3 + TypeScript frontend with Vue Router
- SQLite schema for managers, sessions, servers, and custom commands
- Bearer-token manager authentication
- Public and manager server APIs
- Managed server creation and configuration
- Minecraft process launch and console command writing
- Asset suspend and resume by moving files inside the server root
- Frontend visitor and manager pages using polling

The remaining work should focus on completing the real runtime behavior, hardening the management flow, and finishing end-to-end quality.

## 2. Delivery Strategy

Implement the remaining work in five passes:

1. Stabilize backend runtime and process lifecycle
2. Complete backend metrics and runtime data quality
3. Refine frontend manager and visitor experience
4. Add tests and operational validation
5. Prepare deployment and launch checklist

This order keeps the backend data model stable before polishing UI behavior.

## 3. Phase 1: Backend Runtime Stabilization

### 3.1 Goal

Make managed Minecraft processes reliable enough that frontend polling always reflects the actual running state.

### 3.2 Tasks

1. Review and tighten `ServerProcessService` lifecycle management.
   - Ensure one running process per `serverId`
   - Ensure restart cannot leave duplicate reader threads
   - Ensure stop waits for graceful shutdown, then forces exit if needed
   - Ensure stdin writer and background executors are closed on exit

2. Add scheduled runtime refresh for running servers.
   - Use a Spring scheduled task
   - Iterate only servers marked `ONLINE` or actively running in the runtime registry
   - Periodically send Minecraft `list` command
   - Refresh runtime player state from real server output

3. Improve server output parsing.
   - Support common Minecraft log line prefixes
   - Distinguish raw log lines from public-safe chat lines
   - Improve player join/leave detection
   - Improve `list` output parsing for common vanilla/Forge/Fabric variants

4. Normalize runtime state updates.
   - Keep chat, logs, players, and last-seen timestamps in one consistent flow
   - Prevent stale players from remaining online after server stop or restart
   - Reset transient state correctly when a process exits

5. Tighten status synchronization.
   - Keep database `server_config.status` aligned with process state transitions
   - Mark startup failure and unexpected process exit clearly
   - Ensure snapshot generation reads the runtime registry first, then persisted state

### 3.3 Deliverables

- Reliable start/stop/restart behavior
- Automatic player-list refresh while servers are online
- More accurate player/chat/log state in manager and visitor snapshots

## 4. Phase 2: Backend Metrics and Data Completeness

### 4.1 Goal

Replace placeholder server metrics with real measurements that fit the lightweight deployment model.

### 4.2 Tasks

1. Introduce a metrics collection approach.
   - Prefer a lightweight Java system metrics library such as OSHI if compatible with the current stack
   - If per-process network metrics are too expensive or unreliable, document a simpler approximation for v1

2. Implement metrics collection for each managed server process.
   - CPU usage
   - Memory usage
   - Network speed or a documented fallback value if precise per-process network usage is not practical

3. Extend runtime state or snapshot assembly to carry fresh metrics.
   - Store latest metrics in runtime memory
   - Timestamp metric collection
   - Avoid blocking API responses on expensive collection

4. Validate public-safe data boundaries.
   - Public snapshot must expose only allowed data
   - Manager snapshot must include full logs and management metadata
   - Confirm no manager-only data leaks through public DTO assembly

5. Finish server metadata quality.
   - Improve version detection if currently static or missing
   - Confirm mods, datapacks, and resource packs are scanned consistently from real directories
   - Filter asset lists to avoid disabled-directory noise or unrelated files

### 4.3 Deliverables

- Real metrics in `ServerSnapshotDto`
- Cleaner asset and version information
- Stable public versus manager data separation

## 5. Phase 3: Backend Security and Auth Completion

### 5.1 Goal

Close the gap between the design requirement and the current authentication scaffold.

### 5.2 Tasks

1. Upgrade manager authentication to actual 2FA.
   - Add TOTP secret storage for manager accounts
   - Add login request fields and validation flow
   - Verify session is not considered authenticated until password and TOTP both pass

2. Improve session handling.
   - Define token expiration policy
   - Invalidate prior session when a manager logs in again if exclusive token policy is required
   - Remove or expire stale sessions

3. Harden security responses.
   - Standardize unauthorized and forbidden API error payloads
   - Avoid leaking sensitive auth details in messages

4. Review manager-only endpoint coverage.
   - Confirm every manager API checks authentication and target server existence consistently
   - Confirm public endpoints never call manager-only DTO builders

### 5.3 Deliverables

- Real 2FA flow
- Cleaner session lifecycle
- Backend security behavior aligned with `design-complete.md`

## 6. Phase 4: Frontend Feature Completion

### 6.1 Goal

Bring the current UI from functional scaffold to a polished management console that fully matches the design.

### 6.2 Tasks

1. Clean remaining UI strings and encoding issues.
   - Ensure all visible text is Chinese
   - Remove mojibake or mixed-language residue in untouched files

2. Align login UX with final auth flow.
   - Extend `LoginModal.vue` for TOTP input
   - Show clear session-expired handling
   - Refresh session state cleanly after login/logout

3. Improve visitor dashboard quality.
   - Better empty states for no players, no chat, no assets
   - Clear offline state when the process is not running
   - Show metrics freshness and fallback messaging if metrics are unavailable

4. Improve manager control clarity.
   - Distinguish safe actions from destructive actions visually
   - Show restart recommendation after asset changes
   - Give success/failure feedback for console commands and player actions
   - Prevent duplicate submissions during pending actions

5. Refine routing and data refresh behavior.
   - Ensure route changes between servers fully reset stale view state
   - Tune polling intervals for visitor and manager pages
   - Pause or reduce polling when the page is hidden if desired

6. Keep `animal-island-vue` as the primary UI component layer.
   - Reuse design system components consistently
   - Preserve current visual language rather than mixing in ad-hoc controls

### 6.3 Deliverables

- Clean Chinese UI
- Complete manager login flow
- Better operational feedback on both visitor and manager pages

## 7. Phase 5: Testing and Validation

### 7.1 Backend Tests

Add or expand tests in `backend-server/src/test/java` for:

- Auth success and failure cases
- Session invalidation and logout
- Public endpoint access without authentication
- Manager endpoint rejection without token
- Server creation and config validation
- Custom command CRUD rules
- Asset suspend/resume file movement behavior
- Snapshot assembly logic
- Runtime status transitions where practical

Where real Minecraft process execution is hard to test directly, isolate parser and service logic into focused unit tests.

### 7.2 Frontend Validation

Run:

- `pnpm build` in `frontend-client`

Perform manual checks for:

- Visitor route loading
- Manager login/logout
- Snapshot polling updates
- Server create flow
- Power action UX
- Asset toggle UX
- Custom command management

### 7.3 Runtime Manual Validation

With at least one real local Minecraft server directory:

1. Register the server from the UI
2. Start the server
3. Confirm logs stream into manager view
4. Confirm chat extraction appears in visitor view
5. Confirm `list`-based online player refresh works
6. Suspend one mod or datapack and verify filesystem movement
7. Restart and confirm state reflects the change

## 8. Phase 6: Deployment Preparation

### 8.1 Backend

- Externalize production manager credentials/bootstrap strategy
- Confirm SQLite file location and backup strategy
- Confirm writable permissions for managed server directories
- Tune JVM launch options and process timeouts

### 8.2 Frontend

- Verify Vite build output for production
- Confirm API base path and reverse-proxy compatibility

### 8.3 Reverse Proxy

- Add Caddy configuration for frontend and backend routing
- Enforce HTTPS
- Ensure manager auth tokens are accepted only over secure deployment

### 8.4 Operations Checklist

- Initial manager account setup
- 2FA enrollment flow
- Database initialization
- Add server root directories
- Smoke test all manager controls after deployment

## 9. Recommended Implementation Order by File Area

### Backend first

1. `server/runtime/ServerProcessService.java`
2. `server/runtime/ServerRuntimeState.java`
3. `server/runtime/ServerRuntimeRegistry.java`
4. `server/service/ServerCatalogService.java`
5. `server/service/ServerStatusService.java`
6. `server/service/ServerManagementService.java`
7. auth and security classes for 2FA/session improvements

### Frontend second

1. `src/composables/useServerSnapshot.ts`
2. `src/components/ManagerControlPanel.vue`
3. `src/components/ServerOverview.vue`
4. `src/components/LoginModal.vue`
5. `src/views/ServerWorkspacePage.vue`
6. `src/views/ServerDirectoryPage.vue`

## 10. Suggested Milestone Breakdown

### Milestone A: Runtime usable

- Process lifecycle stable
- Scheduled `list` refresh implemented
- Snapshot data no longer obviously stale

### Milestone B: Metrics and auth complete

- Real metrics available
- 2FA implemented
- Session flow hardened

### Milestone C: UI polished

- Chinese UI cleaned
- Better feedback and empty states
- Manager workflow feels complete

### Milestone D: Ready to deploy

- Tests added
- Manual validation completed
- Reverse proxy and production config documented

## 11. Main Risks

- Minecraft log formats vary across server types and versions
- Accurate per-process network metrics may be difficult on Windows without extra complexity
- Java process management and filesystem locking can behave differently across environments
- TOTP implementation affects both schema and login UX, so it should not be deferred too late
- Maven wrapper execution may still require local environment fixes before full backend verification

## 12. Immediate Next Steps

The most efficient next sequence is:

1. Implement scheduled `list` refresh and improve runtime log parsing
2. Replace placeholder metrics with real collection
3. Add 2FA to the manager auth flow
4. Polish frontend UX around login, offline state, and asset restart guidance
5. Add backend tests and rerun frontend build
