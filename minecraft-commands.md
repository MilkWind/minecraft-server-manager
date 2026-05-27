Here are the standard **Minecraft Java Edition server commands** (compatible with Vanilla, Spigot, Paper, Purpur and most popular server cores) for your requirements, including correct syntax, usage and regular console/chat output:

---

### 1. View online players
| Item | Details |
|------|---------|
| **Command** | `/list` (in-game) or `list` (server console, no slash) |
| **Optional flags** | `/list uuids` → also display UUIDs of all online players |
| **Regular output** |
| Console | `There are 2 out of maximum 20 players online: Notch, Dinnerbone` |
| In-game chat | `§6There are 2 out of maximum 20 players online: §rNotch, Dinnerbone` |

---

### 2. Grant / revoke operator (OP) permissions
#### Grant OP
| Item | Details |
|------|---------|
| **Command** | `/op <player_name>` |
| **Example** | `/op Notch` |
| **Regular output** |
| Console | `Opped Notch` |
| In-game chat (to operator) | `§7Made Notch a server operator` |
| In-game chat (to target player) | `§7You are now op!` |

#### Revoke OP (DEOP)
| Item | Details |
|------|---------|
| **Command** | `/deop <player_name>` |
| **Example** | `/deop Notch` |
| **Regular output** |
| Console | `De-opped Notch` |
| In-game chat (to operator) | `§7Made Notch no longer a server operator` |
| In-game chat (to target player) | `§7You are no longer op!` |

---

### 3. Ban players
#### Ban player account (by username)
| Item | Details |
|------|---------|
| **Command** | `/ban <player_name> [optional ban reason]` |
| **Example** | `/ban Griefer123 Intentional grief and theft` |
| **Regular output** |
| Console | `Banned player Griefer123: Intentional grief and theft` |
| Target player kick screen | `You are banned from this server.\nReason: Intentional grief and theft` |

#### Ban player by IP address
| Item | Details |
|------|---------|
| **Command** | `/ban-ip <player_name / IP address>` |
| **Example** | `/ban-ip 192.168.1.100` |
| **Regular output** | `Banned IP address 192.168.1.100` |

#### Unban player
| Item | Details |
|------|---------|
| **Command** | `/pardon <player_name>` (account unban) / `pardon-ip <IP>` (IP unban) |
| **Regular output** | `Unbanned player Griefer123` |

---

### Notes:
1. All commands require operator level 4 permission (or console access) to execute
2. Bedrock Edition uses nearly identical syntax, with only minor differences in output formatting
3. For modded servers (Forge/Fabric) or permission plugins like LuckPerms, commands remain fully compatible