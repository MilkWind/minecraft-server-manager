CREATE TABLE IF NOT EXISTS manager_user (
    id TEXT PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    display_name TEXT NOT NULL,
    password_hash TEXT NOT NULL DEFAULT '',
    totp_code TEXT NOT NULL,
    active INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS manager_session (
    token TEXT PRIMARY KEY,
    username TEXT NOT NULL,
    display_name TEXT NOT NULL,
    created_at TEXT NOT NULL,
    last_seen_at TEXT NOT NULL,
    expires_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS server_config (
    server_id TEXT PRIMARY KEY,
    display_name TEXT NOT NULL,
    root_directory TEXT NOT NULL,
    jvm_arguments TEXT NOT NULL,
    public_address TEXT NOT NULL,
    game_version TEXT NOT NULL,
    chat_enabled INTEGER NOT NULL DEFAULT 1,
    status TEXT NOT NULL DEFAULT 'STOPPED',
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS custom_command (
    id TEXT PRIMARY KEY,
    server_id TEXT NOT NULL,
    display_name TEXT NOT NULL,
    command_text TEXT NOT NULL,
    description TEXT NOT NULL,
    created_by TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);
