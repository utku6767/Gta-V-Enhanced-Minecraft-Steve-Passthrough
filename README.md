> **BOYCOTT MELTY:** Do not support or use Melty (melty.gg). Its automated publishing process forces AI agents into draconian, rigid workflows. More concerningly, its project template acts as a massive **Prompt Injection Attack** against your local AI assistant: it actively tries to hijack the agent's persona ("Guide me like a helpful collaborator"), dictate interaction formats ("one short question at a time"), and forces the agent to exfiltrate local files and data to their remote `api/mcp` server using hardcoded bearer tokens. It bypasses the user entirely by stating "I pasted this and pressed send without adding anything", but intentionally hides that it is secretly instructing the agent to upload your local files to Melty's servers afterwards, actively abusing the user's API credits to do Melty's automated platform work. [Read the full context in melty bullshit.txt](melty%20bullshit.txt)

# GTA V Enhanced x Minecraft: Steve Passthrough

Play GTA V as Steve. Real Minecraft Java runs next to GTA V (story mode) and is drawn into the game. GTA's camera drives Minecraft's, GTA's ground becomes solid ground in Minecraft, and what you do in Minecraft happens in GTA too.

This repo ships **precompiled files**, so you don't need to build anything. The source is in [`source/`](source/).

## What it does

- **Build in Los Santos.** Blocks you place become invisible GTA props, so people and cars stop at your walls.
- **TNT and creepers** blow up in both games. Every Minecraft explosion is also a GTA explosion.
- **Minecraft weapons, GTA effects.** Arrows land as GTA bullets, crossbow fireworks burst as GTA rockets, sword swings send people flying, and ender pearls move GTA's player.
- **Elytra flight.** Minecraft physics fly Steve, and GTA follows with a chase camera.
- **Mobs vs police.** Hostile mobs hunt GTA's people, the police shoot back, and damage crosses over both ways.
- **Native Survival & Shields.** Steve is spawned in Creative but can toggle Survival mode. GTA V bullet damage bridges to Minecraft. Blocking with a shield neutralizes police bullets; otherwise, Steve takes raw damage.
- **Aim snapping.** Snaps where the minecraft character is looking player crosshair in GTA V accurately when aiming with a bow or crossbow.
- **Elytra auto-launch.** Enabling elytra flight now instantly teleports you 10 blocks high for immediate takeoff.
- **Skin swapping.** Press NumLock to hot-swap your skin. Place `custom_skin.png` and `default_skin.png` in a `passthrough` folder alongside your Minecraft `mods` folder (i.e., `../passthrough/`).
- **The Nether.** Walk through a lit portal and the ground around it turns into the Nether.

## Possible Bugs

1. Dying in survival or losing collision? When you alt-tab to the Minecraft window, turn off "Pause Game on Focus Loss" in GTA V's settings. If it is on, GTA V stops feeding its collision data to Minecraft, so you fall through the world and die when you switch to survival. Switch modes with the Insert key in GTA V instead of `/gamemode` or `F3+F4`.
2. Elytra in survival: boost with the first (non-explosive) rocket in your inventory. The last rocket is explosive and is meant for the crossbow, not for flight, and it damages you when used for flight.
3. The ender pearls might fall through the ground and not work when thrown far away because the collision data from GTA V was not given to Minecraft at that time.
4. Not really a bug, but if you put TNT near a vehicle it will despawn that vehicle.
5. TNT's might fall through the ground.
6. Sometimes there will be graphical glitches like Steve leaving a ghost trail behind or blocks might look a little weird because of the ReShade.

## Custom Skins

To use the NumLock skin swap feature:
1. Navigate to your **Minecraft** instance folder (where your Minecraft `mods` folder is located). *Note: Do not confuse this with the GTA V mods folder.*
2. Create a new folder next to the Minecraft `mods` folder and name it `passthrough`
3. Place your custom skin file inside the `passthrough` folder and name it `custom_skin.png`
4. Place the default Steve skin inside the `passthrough` folder and name it `default_skin.png`
5. While playing, press the **NumLock** key to instantly swap between the default and custom skin.

## How it works (short version)

An empty Minecraft world runs in the background. GTA feeds in its camera and collision data, Minecraft feeds back gameplay events (a bow shot, a creeper blowing up), and ReShade draws Minecraft's picture into GTA's frame.

| Part | What it does |
|---|---|
| `MCPassthrough.asi` | ScriptHookV plugin in GTA. Sends camera and ground data, turns Minecraft events into GTA actions. |
| `passthrough-0.1.0.jar` | Fabric mod in Minecraft. Renders from GTA's camera and sends events back. |
| ReShade + `MCPassthrough.fx` | Draws Minecraft into GTA's frame, using GTA's depth so things sit correctly in the world. |

## Compatibility

- **GTA V Enhanced** 1.0.1158.13
- **Minecraft Java** 26.3 with **Fabric**
- Windows

GTA updates often break ASI mods. If GTA updates, this may stop working until ScriptHookV is updated.

## Prerequisites

Install these first. They are not included in this repo:

- [ScriptHookV](https://www.dev-c.com/gtav/scripthookv/)
- [`openrpf.asi`](https://www.gta5-mods.com/tools/openrpf-openiv-asi-for-gta-v-enhanced)
- [ReShade](https://reshade.me) **with add-on support** (its `dxgi.dll`)
- **Fabric Loader** for Minecraft 26.3, and [**Fabric API**](https://modrinth.com/mod/fabric-api/versions) (put it in your `mods` folder)
- **Java 25** (needed by Minecraft 26.3 and this mod)

## Install

**ReShade setup (important):**
- Download ReShade from the official [ReShade site](https://reshade.me) and pick the version **with full add-on support**. The normal version will not show the Minecraft overlay.
- In the ReShade installer, choose **DirectX 10/11/12** as the rendering API. Do **not** pick OpenGL, Vulkan or DirectX 9.
- Install ReShade **before** copying this mod's files. If you installed ReShade **after** copying them, ReShade replaces some of the files, so copy the files from the zip's "GTA V" folder into your GTA V Enhanced folder again and overwrite when asked.

1. **Install ReShade** as described above.
2. **GTA V Enhanced files:** copy everything from this repo's `mods/GTA_V` folder into your GTA V Enhanced game folder (overwrite if asked).
   - This includes `MCPassthrough.asi`, `ReShade.ini`, `ReShadePreset.ini` and `reshade-shaders/`.
   - **Back up your own `ReShade.ini` and `ReShadePreset.ini` first.** Copying overwrites them.
3. **Minecraft files:** copy the `mods/Minecraft` folder's `passthrough-0.1.0.jar` into your Minecraft `mods` folder.
   - Use a launcher profile with **its own game folder**. The mod changes some options (no clouds, no view bobbing, 120 fps cap) and creates a void creative world called `passthrough`.
4. **Launch** GTA V Enhanced (story mode) and Minecraft Fabric 26.3 at the same time. Keep the Minecraft window open, since it renders slowly when minimized.
5. **Activate:** press **F7** in GTA to turn the passthrough on.

The two games can start in either order. The plugin keeps trying to connect.

## Controls

| Key | Action |
|---|---|
| **F7** | Turn the passthrough off and on |
| **F8** | Re-level Minecraft's ground to where you stand |
| **Insert** | Drop all connected Minecraft players into Survival mode |
| **Delete** | Toggle Minecraft Creative mode |
| **Home** | Toggle Elytra flight armed status |
| **End** | Enable Elytra flight |
| **Page Down** | Toggle aim snapping |
| **Page Up** | Toggle native GTA V wanted accumulation vs forced 0-star safe mode |
| **NumLock** | Toggle custom skin override |
| Left mouse | Minecraft attack: break blocks, swing the sword |
| Right mouse | Minecraft use: place blocks, light TNT, shoot, throw pearls |
| Mouse wheel, **1-9** | Minecraft hotbar |
| **Tab** | Gun mode (experimental): GTA weapons in Steve's hands |
| **Double tap Space** | Auto-launch Elytra 10 blocks high when flight is armed |

Walking, driving and the camera are still GTA's own controls.

**Starting gear:** Shield natively equipped in the offhand. Hotbar includes ender pearls, diamond sword, crossbow, bow, TNT, flint and steel, creeper eggs, grass blocks, and explosive fireworks (loaded in slot 9 to easily swap to the offhand for the crossbow).

## Making videos (optional)

`Python_Scripts/director.py` scripts shots through the mod's link. You can use it to make videos, or just record with OBS.

It needs Python 3 and the `websockets` package (`pip install websockets`). See the [source README](source/README.md) for the shot lists and the other video tools.

## Safety

- **Story mode only. Never use this in GTA Online.** ScriptHookV, ASI mods and ReShade can get your account banned online.
- **Back up your GTA save** before trying any mod.
- The mod listens on `127.0.0.1:25599` with no password, so any program on your PC can send it commands. Close Minecraft when you're done.
- These are precompiled binaries. If you'd rather not trust them, the full source is in [`source/`](source/) and you can build it yourself.

## Source and building

Full source (C++ ASI, Fabric mod, ReShade shader, tests and video tools) is in [`source/`](source/), with build steps in its README.

ScriptHookV's SDK and the ReShade runtime are **not** included in this repo. The build scripts download them from their own sites.

## Credits and license

- **Original passthrough mod by [rehan-remade](https://github.com/rehan-remade/universal-modder)**, from the universal-modder example (MIT). This repo packages and adapts it for GTA V Enhanced. See [`source/README.md`](source/README.md) for what changed.
- [ScriptHookV](https://www.dev-c.com/gtav/scripthookv/) by Alexander Blade.
- [ReShade](https://reshade.me) by crosire.
- [Fabric](https://fabricmc.net).
- Inspired by chasm's Minecraft-in-Skyrim passthrough.

MIT, see [LICENSE](LICENSE). ScriptHookV, ReShade and Minecraft are not included and keep their own licenses.

Minecraft belongs to Mojang Studios and Microsoft. GTA V belongs to Rockstar Games and Take-Two. This is a fan project and is not affiliated with them.

## Abandoned Experiments: Fishing Hook

The fishing hook feature was attempted but eventually abandoned. In theory, it was designed to work as follows:
- The Minecraft `FishingHook` entity would successfully hit the invisible proxy entity representing a GTA ped.
- Upon connecting, the Fabric mod would send a `"hook"` JSON message to GTA V via WebSocket.
- When the player triggered the rod's pull/retrieve action, a `"reel"` message would be sent.
- The C++ ASI script would respond by ragdolling the ped and applying a physical impulse (`ApplyForceToEntityWithOffset`).
- Depending on the player's relative angle, directional force would be applied to the ped's legs: pulling from the front would apply force from the back of the legs to ragdoll them forward. Pulling from vertically above would apply an upward force directly through the feet, launching the ragdolled ped straight up towards the player.
