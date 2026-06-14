# Progress Report

Date: 2026-05-30

## Current Status

The backend and frontend implementation are largely complete. Frontend build and backend test validation have now passed. The remaining work is manual/runtime validation, plus any fixes discovered during that work.

## Implemented Functions

### Backend

- [x] API envelope and global exception handling
- [x] SQLite schema and bootstrap seed logic
- [x] Manager authentication and session flow with bcrypt + TOTP
- [x] Expired-session cleanup
- [x] Public server APIs
- [x] Manager server APIs
- [x] Runtime process start/stop/restart
- [x] Log pump for server output
- [x] Chat/player parsing from runtime output
- [x] Scheduled `list` refresh
- [x] Live snapshot assembly with runtime metrics
- [x] Asset suspend/resume handling
- [x] Restart recommendation state tracking

### Frontend

- [x] Vue Router and application shell
- [x] Login modal with TOTP
- [x] Create-server modal
- [x] Directory page
- [x] Workspace page
- [x] Session composable
- [x] Storage composable
- [x] API composable
- [x] Directory composable
- [x] Snapshot composable
- [x] Manager control panel
- [x] Config editor
- [x] Custom command execution UI
- [x] Log copy UI
- [x] Asset toggle UI
- [x] Restart recommendation UI
- [x] Auth-loss handling
- [x] Session-expiry recovery
- [x] Selected-server persistence
- [x] Visibility-aware polling pause/resume

## Needs Validation

These items are implemented in code but still need build/test/manual verification:

- Browser smoke test for visitor flow
- Browser smoke test for manager login flow
- Real Minecraft server lifecycle checks
- Asset suspend/resume behavior on a live server
- Log parsing and player detection against live output
- TOTP login verification
- Reverse proxy and deployment validation with Nginx

Verified in this pass:

- [x] Frontend type-check and production build
- [x] Backend tests
- [x] Public server list API
- [x] Public server snapshot API
- [x] Manager login API with bcrypt password + TOTP
- [x] Manager current-session API with bearer token
- [x] Manager server snapshot API with bearer token
- [x] Unauthorized manager snapshot rejection
- [x] Invalid login rejection
- [x] Snapshot metrics JSON contract matches frontend fields

Still pending:

- Browser smoke test for visitor flow
- Browser smoke test for manager login flow
- Real Minecraft server lifecycle checks
- Asset suspend/resume behavior on a live server
- Log parsing and player detection against live output
- Logout/token-revocation verification with a fresh TOTP
- Reverse proxy and deployment validation with Nginx

## Still Unfinished

The following work is still not fully verified or may need follow-up changes after validation:

- Network metric accuracy is still conservative and may need refinement
- Any runtime behavior gaps that only appear during live Minecraft server validation

## Recommended Next Step

1. Perform browser smoke tests.
2. Validate Minecraft runtime behavior against a live server.
3. Verify Nginx reverse proxy and deployment behavior.
