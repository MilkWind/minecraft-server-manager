# Minecraft Server Manager Design

## 1. Overview

This project is a web-based Minecraft server manager for a small deployment with two servers.

The system provides:

- A visitor view for public server information
- A manager view for authenticated server administration
- One frontend instance
- One backend instance

The product target is a lightweight dashboard, not a large-scale hosting platform.

## 2. Goals

- Manage multiple Minecraft servers from one web application
- Expose safe public information to visitors
- Expose administrative controls only to authenticated managers
- Keep deployment simple and low-cost
- Use a design suitable for a small number of servers and viewers

## 3. Non-Goals

- Large-scale multi-tenant hosting
- High-frequency telemetry storage
- Complex workflow approval systems
- Historical analytics platform
- Shell-level remote execution from the web UI

## 4. Users and Roles

### Visitor

Visitors can:

- View online players
- View loaded mods, datapacks, and resource packs
- View the server game version
- View server chat messages
- View server performance data: CPU usage, memory usage, network speed

Visitors cannot perform any control action.

### Manager

Managers can:

- Use all visitor features
- View full server logs
- Start, stop, and restart a server
- OP and DEOP players
- Ban players
- Send messages to all players or specific players
- Suspend or resume mods, datapacks, and resource packs, then restart the server
- Define and send custom commands to the Minecraft server console

All managers are trusted operators and can manage all servers.

## 5. High-Level Architecture

The frontend and backend are separated.

- Reverse proxy: Caddy
- Frontend: Vue
- Frontend routing: Vue Router
- Frontend UI library: `animal-island-vue`
- Backend: Spring Boot
- Security core: Spring Security with 2FA
- ORM: MyBatis Plus
- Database: SQLite

Deployment shape:

- One frontend instance
- One backend instance
- Multiple Minecraft server processes managed by the backend

## 6. Routing Model

The application uses route paths that include:

- `serverId`
- `clientType`, such as visitor or manager

Example route intent:

- Visitor route: view server information for one server
- Manager route: access server management after authentication

Important rule:

- Route structure is only a navigation model
- Route structure is not a security boundary
- Backend authorization must be enforced on every manager API request for the target `serverId`

Frontend directives that hide manager UI are only for user experience. They do not provide security.

## 7. Authentication and Authorization

### Authentication

Manager access requires 2FA.

The manager login flow is:

1. Manager opens a manager route for a target server
2. Manager completes authentication and 2FA
3. Backend creates a session-bound authentication token
4. Frontend uses that token for manager API requests

### Session and Token Rules

- Each manager has an exclusive token
- The token is valid only while the authenticated session is alive
- Logout invalidates the current token
- If the session changes or becomes invalid, the current token is deprecated
- After token invalidation, the manager must log in again

### Authorization

Authorization rules:

- Every manager API must validate authentication
- Every manager API must validate authorization for the target `serverId`
- Visitor APIs must expose only public data
- Manager-only data and actions must never be protected by frontend logic alone

## 8. Backend Responsibilities

The backend is responsible for:

- Authentication and authorization
- Server discovery and configuration lookup
- Polling or collecting server runtime information
- Reading server output
- Sending commands to Minecraft server consoles
- Exposing visitor APIs
- Exposing manager APIs
- Persisting manager auth data and custom command metadata

The backend is the only trusted authority for permissions.

## 9. Frontend Responsibilities

The frontend is responsible for:

- Rendering visitor and manager pages
- Routing by `serverId` and client type
- Displaying live server information
- Displaying manager controls after successful authentication
- Hiding manager-only UI from visitors for usability

The frontend must not assume that hidden UI equals protected access.

## 10. Data Storage

SQLite is used because the system only manages two servers and stores limited persistent data.

Persistent data includes:

- Manager authentication-related data
- 2FA-related data
- Custom command definitions
- Custom command metadata
- Server metadata if needed

The system does not target heavy writes for logs, telemetry, or analytics storage.

## 11. Server Integration Model

Each Minecraft server process is started directly using its server `.jar` file.

When adding a managed server, the manager specifies a **server root directory** that contains the server `.jar`, configuration files, and world data. The backend reads the directory to discover the `.jar` and manage the server process.

Managers can configure **JVM parameters** (such as `-Xmx`, `-Xms` for memory allocation) through the UI for each server. These parameters are stored as server metadata and applied when the backend launches the `.jar` process.

The manager system integrates with the server process by:

- Reading console output or generated runtime information
- Sending commands to the Minecraft server console input

Custom command safety rule:

- Custom commands only target Minecraft server console input
- Custom commands must not execute shell commands or OS-level commands

This keeps custom commands within the Minecraft server command surface.

## 12. Logging and Chat Handling

Manager view:

- Managers can view full raw server logs

Visitor view:

- Visitors can only view chat messages
- Chat messages are extracted from server output
- System logs must be filtered out before visitor display

Operational assumption:

- Managers are trusted and allowed to inspect full logs
- Raw logs are not exposed to visitors

## 13. Metrics and Refresh Strategy

Displayed metrics include:

- CPU usage
- Memory usage
- Network speed

Transport strategy:

- REST APIs with polling

Reason:

- The target deployment is small
- Only two servers are expected
- Simpler polling is preferred over WebSocket or SSE for this version

Polling frequency should be conservative to avoid unnecessary load.

## 14. Functional Design

### Visitor Features

- Show online players
- Show loaded mods
- Show loaded datapacks
- Show loaded resource packs
- Show server version
- Show filtered chat messages
- Show server performance metrics

### Manager Features

- All visitor features
- 2FA-protected access
- Full server log view
- One-click copy for logs
- Start server
- Stop server
- Restart server
- OP player
- DEOP player
- Ban player
- Send message to all players
- Send message to specific player
- Suspend selected mods, datapacks, or resource packs
- Resume selected mods, datapacks, or resource packs
- Restart server after asset state changes
- Create server-specific custom commands
- Execute server-specific custom commands
- Configure JVM parameters (memory allocation, etc.) for each server
- Add a new managed server by specifying its server root directory

## 15. Custom Command Model

Custom commands are:

- Defined per server
- Configured by managers
- Stored with metadata in SQLite
- Executed by writing to the Minecraft server console

Suggested metadata fields:

- Command ID
- Server ID
- Display name
- Actual command text
- Description
- Created by manager ID
- Created time
- Updated time

Even though all managers are trusted, the backend should still validate:

- Command belongs to the selected server
- Manager is authenticated
- Manager request targets an allowed server

## 16. API Design Rules

Visitor APIs:

- Read-only
- Server-specific
- No authentication required
- Must return only public-safe data

Manager APIs:

- Require authenticated manager token
- Require 2FA-completed session
- Must validate `serverId` on every request
- Can read manager-only data
- Can trigger control actions

State-changing manager APIs include:

- Start, stop, restart
- OP, DEOP, ban
- Message send
- Asset suspend and resume
- Custom command create, update, delete, execute

## 17. Reverse Proxy and Deployment

Caddy handles reverse proxy responsibilities.

Expected responsibilities include:

- Public entry point
- Route forwarding to frontend and backend
- HTTPS termination

Recommended deployment principle:

- Serve the application only over HTTPS

## 18. UI and Language

- Code comments and annotations use English
- UI text uses Chinese

This is an accepted project convention.

## 19. Design Constraints

- Simple deployment
- Small number of servers
- Small number of concurrent users
- Low operational complexity
- Trusted manager group

These constraints justify:

- SQLite
- Polling over push updates
- Single frontend and backend instances

## 20. Final Design Position

This design is intentionally lightweight and optimized for a small trusted deployment.

The critical implementation rules are:

- Backend authorization is enforced per request and per server
- Manager features are never protected only by hidden frontend UI
- Custom commands are restricted to Minecraft console input only
- Visitor access is read-only and limited to public-safe information
- Full logs are manager-only
- SQLite and polling are acceptable because only two servers are expected
