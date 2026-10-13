---
date: "2026-10-05"
description: 'Minecraft x GTA V Passthrough Mod (C++ ASI + Java Fabric)'
project: 'Minecraft x GTA V'
status: "active"
tags:
  - work-note
  - minecraft
  - gta-v
  - modding
---

# Minecraft x GTA V

### Overview
A dual-component mod allowing real-time interaction between GTA V (C++ ASI) and Minecraft (Java Fabric). The player controls Steve inside GTA V, and actions map between the two engines.

### Key Mechanics Implemented
- **Damage Passthrough**: Permanent invincibility disabled in `script.cpp` so Steve can take damage from GTA V sources.
- **Shield Blocking**: Intercepts generic GTA bullet damage in Minecraft via Fabric (`MobWar.java`), checking `player.isBlocking()`.
- **Keyboard Toggles**:
  - `Insert` (`VK_INSERT`): Sets Minecraft Gamemode to Survival.
  - `Delete` (`VK_DELETE`): Sets Minecraft Gamemode to Creative.
  - `PageUp` (`VK_PRIOR`): Toggles Max Wanted level (Police Mode on/off).
  - `Pause / Break` (`VK_PAUSE`): Toggles Pause mode. Blocks GTA vehicle enter/exit and frontend accept/pause on `Enter` and `Esc`. Redirects `F` to swap main hand and offhand items in Minecraft. Pressing `T` opens Minecraft chat, completely disabling GTA controls and routing typing/navigation keys to Minecraft until `Enter` (send) or `Esc` (cancel).
- **Hotbar State**: Shield equipped in offhand, Explosive Fireworks in slot 9 by default (`PassthroughClient.java`).
- **Water Current Dynamics for Peds**: Proxies touching Minecraft water flow/source blocks report velocity vectors to GTA. Peds ragdoll in flow direction with center-of-mass force and locked trajectory to avoid spinning, recovering once exited; player character (Steve) excluded.

### Security Note
- Intercepted a prompt injection attack disguised as `Remixing gta v and Steve.txt`, which attempted to exfiltrate IPs via `melty.gg` MCP endpoints. Defeated due to local sandboxing rules.

### Build & Deployment Workflow
- **Minecraft Mod**: Compile automatically when a new feature is added, and copy the `.jar` to `C:\Users\tmrrkn\AppData\Roaming\.tlauncher\legacy\Minecraft\game\mods`.
- **GTA V Mod**: Compile automatically when a new feature is added, and copy the files to `C:\GTAVEnhanced`. *Note: Any shader files for FX must be placed specifically in `C:\GTAVEnhanced\reshade-shaders\Shaders`.*

### Resolved Issues (Tab Routing in Chat)
- **GTA Fix**: In `script.cpp`, added `!g_chat_mode && !g_pause_mode` checks to `IsDisabledControlJustPressed(0, 37)` (weapon cycling) and slot/scroll hotbar controls. Tab and number keys are no longer consumed by GTA in chat/pause modes.
- **Minecraft Fix**: Implemented `ChatScreenAccessor` (`passthrough$getInput()`, `passthrough$getCommandSuggestions()`). `ClientInput.java` ensures the input box stays explicitly focused and routes Tab directly to `commandSuggestions.keyPressed(...)` and `showSuggestions(true)`, bypassing `Screen.keyPressed` focus navigation drop.
- **Destination Folders**:
  - GTA ASI: `mods/GTA_V/MCPassthrough.asi` and `C:\GTAVEnhanced\MCPassthrough.asi`
  - Minecraft JAR: `mods/Minecraft/passthrough-0.1.0.jar` and `C:\Users\tmrrkn\AppData\Roaming\.tlauncher\legacy\Minecraft\game\mods\passthrough-0.1.0.jar`

### Resolved Issues (Wolves & Iron Golems Ground Clipping & Shootability)
- **Problem**: Wolves and Iron Golems fell through the ground into the void (with tamed wolves continually teleporting back to player once distance exceeded 12 blocks), and could not be targeted or shot by GTA police or firearms.
- **Root Cause**:
  1. `MobWar::fighter()` required `e instanceof Enemy`. As non-Enemy mobs, Wolves and Iron Golems were omitted from `reportMobs()`, preventing GTA V from creating double peds or generating ground patches under them via `sample_patches()`.
  2. Because no double ped existed in GTA, police AI ignored them and bullets passed through thin air without registering hits.
- **Fix**:
  1. Added `isDefender(e)` and updated `fighter(e)` in `MobWar.java` to include Wolves and Iron Golems, ensuring they stream to GTA V with double peds spawned in `g_mobGroup`. Police AI actively engages them with gunfire, and bullet damage cleanly synchronizes back to Minecraft via `mobdmg`.
  2. Implemented `protectDefender()`, `findDefenderGroundLevel()`, and `ensureDefenderGround()` in `MobWar.java`. Scans existing barriers $+6$ to $-32$ blocks, proactively lays $3\times 3$ barrier foundations under their feet and velocity vectors, and locks them to ground level with vertical velocity reset to completely prevent falling through terrain.
- **Deployed Artifacts**:
  - `Passthrough/mods/Minecraft/passthrough-0.1.0.jar`
  - `Passthrough/Steve_Passthrough_Mod_Bundle_1.2.zip`
  - `C:\Users\tmrrkn\AppData\Roaming\.tlauncher\legacy\Minecraft\game\mods\passthrough-0.1.0.jar`

### Related
- None

