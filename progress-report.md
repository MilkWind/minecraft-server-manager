# Progress Report

## Current Strategy

The implementation strategy has changed:

- Build the remaining functions first
- Do not stop on permission or test blockers
- Leave verification, build checks, and gradual runtime testing for later manual passes

This report marks implemented functions with `✅` even if they have not been tested yet.

## Implemented Functions

### Backend Foundation

- ✅ API response envelope
- ✅ global exception handling
- ✅ SQLite schema and bootstrap seed flow
- ✅ server config persistence
- ✅ custom command persistence
- ✅ bearer-token manager session handling

### Backend Auth

- ✅ manager login endpoint
- ✅ manager logout endpoint
- ✅ current session endpoint
- ✅ password verification flow
- ✅ TOTP-based verification flow
- ✅ exclusive session replacement on login
- ✅ auth service dependency cleanup to reduce integration coupling
- ✅ scheduled expired-session cleanup

### Backend Server Management

- ✅ public server list API
- ✅ public server snapshot API
- ✅ manager server snapshot API
- ✅ manager full log API
- ✅ manager log copy UI flow
- ✅ power start/stop/restart API
- ✅ raw console command API
- ✅ server config update API
- ✅ managed server create API
- ✅ custom command create/update/delete support
- ✅ player OP/DEOP/BAN support
- ✅ broadcast/private message support
- ✅ asset suspend/resume support

### Backend Runtime

- ✅ server `.jar` discovery from server root
- ✅ process start with configured JVM arguments
- ✅ process stop/restart flow
- ✅ console stdin command writing
- ✅ stdout/stderr merged log pumping
- ✅ runtime log buffering
- ✅ runtime chat extraction
- ✅ join/leave player tracking
- ✅ scheduled running-server `list` refresh
- ✅ live snapshot assembly from runtime + database state
- ✅ runtime metrics collection replacing hardcoded snapshot metrics
- ✅ runtime metrics cached in runtime state
- ✅ parsed runtime status transitions synchronized back to persisted server status
- ✅ restart recommendation state tracked in backend runtime after asset changes
- ✅ restart recommendation exposed through shared snapshot data and consumed by manager UI

### Frontend Foundation

- ✅ top-level app entry
- ✅ router structure for server directory and workspace pages
- ✅ shared API helper
- ✅ shared local storage helper
- ✅ session composable
- ✅ server directory composable
- ✅ server snapshot composable

### Frontend Pages and Components

- ✅ app shell component
- ✅ login modal with TOTP input
- ✅ create managed server modal
- ✅ server overview component
- ✅ manager control panel
- ✅ server directory page
- ✅ server workspace page

### Frontend Manager UI Functions

- ✅ manager login flow wiring
- ✅ visitor snapshot polling flow
- ✅ manager snapshot polling flow
- ✅ power action UI wiring
- ✅ console command UI wiring
- ✅ player action UI wiring
- ✅ message send UI wiring
- ✅ asset toggle UI wiring
- ✅ asset change restart recommendation UI
- ✅ server config edit UI wiring
- ✅ custom command create/edit/delete/execute UI wiring
- ✅ managed server create UI wiring
- ✅ managed server create modal flow
- ✅ workspace page wiring for server config update actions
- ✅ workspace page wiring for custom command create/update/delete/execute actions

## Partially Implemented or Likely Needs Follow-up

- ⚠ frontend page/component compatibility with every untouched import path is not fully verified
- ⚠ backend metrics are implemented, but current network metrics still use zero/fallback values
- ⚠ Minecraft log parsing is improved but still may miss some server-specific output variants
- ⚠ runtime and auth behavior have been implemented without a final compile/test pass

## Not Yet Finished

- ❌ full manual integration verification
- ❌ backend compile/test verification
- ❌ frontend build verification after the latest rewrites
- ❌ remaining polish for UX details and edge-case handling discovered during later testing

## Current Problems / Risks

- Maven repository permission issues still block reliable backend verification in this environment
- Shell/source inspection has been intermittently failing with sandbox startup errors
- Because of the two issues above, several frontend and backend rewrites were done by replacing files directly instead of iterating with normal read-build-fix loops
- The app now appears structurally complete in code, but runtime compatibility still needs a later manual test pass

## Recommended Next Step

When resuming work, the best next step is:

1. keep the code as-is
2. run gradual manual testing
3. record concrete breakages
4. fix integration issues one by one

That is now more valuable than broad feature building, because most core functions have already been implemented in code.
