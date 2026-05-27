# Progress Report

## What Has Been Done

- Set up the backend and frontend scaffold.
- Added backend auth, session, API envelope, and exception handling.
- Added SQLite schema and seed/bootstrap flow for managers and server records.
- Added Minecraft server process start/stop/restart support.
- Added runtime state tracking for logs, chat, and online players.
- Added asset suspend/resume support by moving files inside the server root.
- Reworked server snapshot assembly to use live DB + runtime state instead of cached refresh-time snapshots.
- Added scheduled player-list refresh for running servers.
- Fixed several encoding/corruption issues in backend config and snapshot text.
- Added a root implementation plan document.

## What I Want To Do Next

- Replace placeholder metrics with real collection.
- Finish the manager 2FA flow.
- Improve runtime log parsing for more Minecraft server output variants.
- Add backend tests for auth, server actions, and runtime behavior.
- Finish frontend polish and manual verification.
- Re-run backend build validation after the repository access issue is resolved.

## Current Problems

- Maven compilation is blocked by filesystem permission errors in the configured repository path:
  - `D:\development-package\maven\maven-repository\org\springframework\boot\spring-boot-maven-plugin\4.0.6`
- Because of that, backend compile verification is still incomplete.
- Metrics are still hardcoded placeholders.
- 2FA is not fully implemented yet.
- Frontend build and manual checks have not been revalidated after the latest backend runtime changes.

## Finished Goals

- Backend runtime state model exists.
- Backend can assemble live snapshots from runtime + database state.
- Scheduled player-list refresh is implemented.
- Process shutdown cleanup is improved.
- Root implementation plan document exists.

## Unfinished Goals

- Real metrics collection.
- Full 2FA auth flow.
- Backend compile/test verification.
- Frontend polish and end-to-end verification.
- Better Minecraft log/player parsing coverage.
