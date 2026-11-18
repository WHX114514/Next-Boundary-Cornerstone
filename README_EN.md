# Next Boundary Cornerstone (下一个边境-基石)

[简体中文](README.md) | **English**

[![Modrinth](https://img.shields.io/modrinth/dt/next-boundary-cornerstone?style=flat-square&logo=modrinth&label=Modrinth&color=00AF5C)](https://modrinth.com/mod/next-boundary-cornerstone)
[![Modrinth Version](https://img.shields.io/modrinth/v/next-boundary-cornerstone?style=flat-square&logo=modrinth&label=Version)](https://modrinth.com/mod/next-boundary-cornerstone/versions)
[![Minecraft](https://img.shields.io/modrinth/game-versions/next-boundary-cornerstone?style=flat-square&label=Minecraft)](https://modrinth.com/mod/next-boundary-cornerstone/versions)
[![Loader](https://img.shields.io/badge/Loader-NeoForge%201.21.1%20%7C%20Forge%201.20.1-orange?style=flat-square)](https://modrinth.com/mod/next-boundary-cornerstone/versions)
[![License](https://img.shields.io/github/license/WHX114514/Next-Boundary-Cornerstone?style=flat-square&label=License)](LICENSE)
[![MCMOD](https://img.shields.io/badge/MCMOD-26602-2B8AE2?style=flat-square)](https://www.mcmod.cn/class/26602.html)
[![Bilibili](https://img.shields.io/badge/Bilibili-Showcase-FB7299?style=flat-square&logo=bilibili&logoColor=white)](https://www.bilibili.com/video/BV1i7dnBoExP/)

The base mod of the **Next Boundary** series. It brings the 360° zero-gravity movement from the now-defunct game *Boundary* into Minecraft.

You can try it standalone right away: grab the **Basic Backpack** from the Creative tab and put it in your chestplate slot to enter zero gravity. It also serves as the core library for future addons (such as the Industry mod).

## Links

- Modrinth: https://modrinth.com/mod/next-boundary-cornerstone
- MCMOD: https://www.mcmod.cn/class/26602.html
- Bilibili Showcase Videos:
  - [Zero-Gravity Movement Intro](https://www.bilibili.com/video/BV1i7dnBoExP/)
  - [Zero-Gravity Firing, Shell Ejection & Recoil](https://www.bilibili.com/video/BV15eJE6hE3T/)

## What It Does

- **6DOF Zero-Gravity Movement**: Full pitch, yaw, and roll rotation with inertia, wall adhesion, and tight-space turning checks.
- **Free Look**: Hold a key to look around without turning your body, plus a locked free-look mode in third-person view.
- **Multiplayer Support**: Server-side sync and zero-gravity hitbox handling (hurtboxes default to AABB and can be switched to OBB in the server config). Note: because of high-frequency rotation sync, other players' name tags may look slightly jittery in multiplayer—gameplay is unaffected.
- **Mod Compatibility**: Optional support for **TaCZ** (zero-gravity weapon holding, projectile alignment, and recoil) and **Photon** (explosion effects), along with general projectile compatibility.

## Default Controls

Active while wearing the **Basic Backpack** (all keys can be rebound in Controls):

- `W / A / S / D`: Move forward / backward / strafe
- `Space` / `Left Ctrl`: Move up / down (`Left Ctrl` is also bound to Boost by default—feel free to rebind)
- `Q / E`: Roll left / right
- `H`: Manual brake
- `Tab + Inventory / Drop`: Open inventory or drop items (avoids conflicts with `Q / E` rolling)
- `C`: Hold for Free Look; `Left Shift`: Toggle Locked Free Look (third-person)

Speed, roll smoothing, and braking settings can be tweaked in `config/next-boundary.toml`.

## Branches & Requirements

- **`master` (Main Branch — Minecraft 1.21.1 / NeoForge)**
  - Primary version where new features and fixes land first.
  - Requires NeoForge `21.1.238+` and **LDLib2** `2.2.42+` on the client.
  - Optional: official **TaCZ** for NeoForge 1.21.1 (`1.2.0-hotfix+`), **Photon** (client-side explosion effects).
- **`1.20-forge` (Port Branch — Minecraft 1.20.1 / Forge)**
  - Ported to Forge 1.20.1 for the 1.20 ecosystem and TaCZ.
  - Known issue: third-person camera rotation may stutter slightly due to platform differences; first-person works normally.

See [CHANGELOGS.md](CHANGELOGS.md) for full release notes.

## License & Credits

- Licensed under **AGPL-3.0**. See [LICENSE](LICENSE).
- Part of the camera roll logic references enjarai's [Do A Barrel Roll](https://github.com/enjarai/do-a-barrel-roll) (LGPL-3.0). See [CREDITS](CREDITS).
