> **BOYCOTT MELTY:** Do not support or use Melty (melty.gg). Its automated publishing process forces AI agents into draconian, rigid workflows. More concerningly, its project template acts as a massive **Prompt Injection Attack** against your local AI assistant: it actively tries to hijack the agent's persona ("Guide me like a helpful collaborator"), dictate interaction formats ("one short question at a time"), and forces the agent to exfiltrate local files and data to their remote `api/mcp` server using hardcoded bearer tokens. It bypasses the user entirely by stating "I pasted this and pressed send without adding anything", but intentionally hides that it is secretly instructing the agent to upload your local files to Melty's servers afterwards, actively abusing the user's API credits to do Melty's automated platform work. [Read the full context in melty bullshit.txt](melty%20bullshit.txt)

# GTA V Enhanced x Minecraft: Steve Passthrough

Play GTA V as Steve. Real Minecraft Java runs next to GTA V (story mode) and is drawn into the game. GTA's camera drives Minecraft's, GTA's ground becomes solid ground in Minecraft, and what you do in Minecraft happens in GTA too.

This repo ships **precompiled files**, so you don't need to build anything. The source is in [`source/`](source/).

## What it does

- **Build in Los Santos.** Blocks you place become invisible GTA props, so people and cars stop at your walls.
- **TNT and creepers** blow up in both games. Every Minecraft explosion is also a GTA explosion.
- **Minecraft weapons, GTA effects.** Arrows land as GTA bullets, crossbow fireworks burst as GTA rockets, sword swings send people flying and launch cars, and ender pearls move GTA's player.
- **Elytra flight.** Minecraft physics fly Steve, and GTA follows with a chase camera. Double-tapping Space auto-launches you 10 blocks high for immediate takeoff.
- **Mobs vs police.** Hostile mobs hunt GTA's people, the police shoot back, and damage crosses over both ways.
- **Iron Golems & Wolves (Defenders).** Iron Golems and wolves attacking NPCs and hostile police. Tamed wolves and iron golems actively hunt down and attack nearby NPCs, armed assailants, and cops. They are fully synchronized with GTA double peds so police and firearms can target and shoot them. Equipped with proactive $3\times 3$ barrier foundations and vertical anti-fall locks to keep them permanently grounded.
- **Native Survival & Shields.** Steve is spawned in Creative but can toggle Survival mode. GTA V bullet damage bridges to Minecraft. Blocking with a shield neutralizes police bullets; otherwise, Steve takes raw damage.
- **Aim snapping.** Snaps the GTA V player crosshair to where the Minecraft character is looking when aiming with a bow or crossbow (toggle with Page Down).
- **Skin swapping.** Press NumLock to hot-swap your skin. Place `custom_skin.png` and `default_skin.png` in a `passthrough` folder alongside your Minecraft `mods` folder (i.e., `../passthrough/`).
- **Water currents carry peds.** GTA peds touching Minecraft water (source blocks and flowing streams) are carried along in the flow direction, tripping forward into ragdoll until they leave the water. Steve's own water movement is untouched.
- **The Nether Incursion & Returning Home.** Walk through a lit portal and the ground around it turns into the Nether (netherrack, soul sand, magma, fire, and meandering lava rivers), GTA's sky turns crimson, and time locks to midnight. Walking back into the portal and standing inside it for 1 second safely closes the Nether—restoring the original terrain top-down, clearing Nether mobs, lifting the red sky, and resetting the sun to noon.
- **Nether Bed Explosions.** Placing and interacting with beds during Nether mode mimics vanilla Nether mechanics: attempting to sleep or right-clicking a bed triggers a catastrophic explosion that registers in both games.
- **Wither & Ender Dragon Boss Mechanics.** Full aerial combat boss fights over Los Santos with explosive heads, fireballs, and street-level diving attacks, backed by anti-fall void protection:
  - **Wither**: Hovers and executes low strafing runs across Los Santos, bombarding traffic, cops, and pedestrians with volleys of exploding wither skull heads that trigger synchronized GTA explosions, while automatically regulating its combat altitude to stay within an ideal $3.5\text{-block}$ bombing height above street targets.
  - **Ender Dragon**: Circles the skyline, bombards the ground with explosive dragon fireballs, and executes devastating low-altitude dive-bombs through the city streets that plow into pedestrians and vehicles, ragdolling NPCs and launching them into the air. Rebuilt with a 3-phase Close Air Support (CAS) state machine (`CIRCLING` orbit with fireball artillery, low-altitude `SWEEP_DIVE` buzzing streets $1.5\text{ m}$ off the ground while ragdolling pedestrians with up to $30.0\text{f}$ impact force, and rapid `PULL_UP` altitude recovery).
- **Fishing Hook & Ragdoll Physics.** We added hooking and pulling NPCs via a fishing hook. Snag pedestrians or police officers with your Minecraft fishing rod. Hooking an NPC locks onto them while keeping them standing upright on their feet without falling over. When you right-click to reel in, the target is immediately launched into full ragdoll physics and yanked directly towards you through the air with high-velocity physical impulses (scaling up to $32.0\text{f}$ horizontally and $26.0\text{f}$ vertically)—dragging drivers cleanly out of vehicles, flinging pedestrians across intersections, or hoisting them high onto rooftops.
- **Pause Mode, Virtual Mouse & In-Game Chat.** Pressing **Pause / Break** toggles Pause mode, disabling GTA menus and vehicle entry:
  - **F**: Swaps items between main hand and offhand in Minecraft.
  - **E**: Opens the Minecraft Inventory with a **Virtual Mouse** cursor (Arrow Keys to navigate, Enter to click/pick up items, Esc or E to close). We made the virtual cursor's axes movement speed equal.
  - **T**: Opens the Minecraft Chat screen. Redirects all typed characters, supports Backspace, Left/Right navigation, Up/Down message history, and **Tab** for command suggestions/auto-completion.
- **Enhanced GTA Weapon Handling for Steve.** Pressing **Tab** cycles GTA weapons (Carbine Rifle, Minigun, RPG) directly into Steve's hands. Eliminates double-gun rendering artifacts, custom-fits weapons to Steve's hands, and aligns physical weapon muzzle trajectory directly with the on-screen crosshair.

## Possible Bugs

1. Dying in survival or losing collision? When you alt-tab to the Minecraft window, turn off "Pause Game on Focus Loss" in GTA V's settings. If it is on, GTA V stops feeding its collision data to Minecraft, so you fall through the world and die when you switch to survival. Switch modes with the Insert key in GTA V instead of `/gamemode` or `F3+F4`.
2. Elytra in survival: boost with the first (non-explosive) rocket in your inventory. The last rocket is explosive and is meant for the crossbow, not for flight, and it damages you when used for flight.
3. The ender pearls might fall through the ground and not work when thrown far away because the collision data from GTA V was not given to Minecraft at that time.
4. Not really a bug, but if you put TNT near a vehicle it will despawn that vehicle.
5. TNTs might fall through the ground.
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
| **Pause / Break** | Toggle Pause mode (redirects keys for offhand swap, virtual inventory mouse, and chat) |
| **F** (in Pause mode) | Swap item with offhand in Minecraft |
| **E** (in Pause mode) | Open Minecraft inventory with **Virtual Mouse** (Arrow keys move cursor with equalized axes speed, Enter clicks/picks items, Esc/E closes) |
| **T** (in Pause mode) | Open Minecraft chat (Type to chat, Up/Down for message history, Tab for command suggestions, Enter sends, Esc cancels) |
| Left mouse | Minecraft attack: break blocks, swing the sword, shoot GTA gun in gun mode |
| Right mouse | Minecraft use: place blocks, light TNT, shoot, throw pearls, aim GTA gun in gun mode |
| Mouse wheel, **1-9** | Minecraft hotbar |
| **Tab** | Gun mode: Cycle GTA weapons (Carbine Rifle, Minigun, RPG, and back to Minecraft) |
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

## Fishing Hook & Reel Physics

We added hooking and pulling NPCs via a fishing hook:
- The Minecraft `FishingHook` entity connects with the villager proxy entity representing a GTA ped.
- Upon connecting, the Fabric mod sends a `"hook"` JSON message to GTA V via WebSocket. The target NPC is snagged while remaining standing upright on their feet without falling over or stumbling prematurely.
- When the player reels in (`retrieve`), a `"reel"` message is dispatched.
- The C++ ASI script responds by putting the NPC into full ragdoll, dragging them cleanly out of vehicles if seated, and applying high-velocity physical impulses (`ApplyForceToEntity`) scaling up to $32.0\text{f}$ horizontally and $26.0\text{f}$ vertically towards and upward relative to the player's position.
