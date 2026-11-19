[![GitHub](https://img.shields.io/badge/GitHub-Repository-181717?style=flat-square&logo=github)](https://github.com/WHX114514/Next-Boundary-Cornerstone)
[![MCMOD](https://img.shields.io/badge/MC%E7%99%BE%E7%A7%91-26602-2B8AE2?style=flat-square)](https://www.mcmod.cn/class/26602.html)
[![Bilibili](https://img.shields.io/badge/Bilibili-%E6%BC%94%E7%A4%BA%E8%A7%86%E9%A2%91-FB7299?style=flat-square&logo=bilibili&logoColor=white)](https://www.bilibili.com/video/BV1i7dnBoExP/)
[![License: AGPL-3.0](https://img.shields.io/badge/License-AGPL--3.0-blue?style=flat-square)](https://github.com/WHX114514/Next-Boundary-Cornerstone/blob/master/LICENSE)

<details>
<summary><b>🌐 Click here for English description (点击展开英文版)</b></summary>

# Next Boundary Cornerstone

The foundation mod of the **Next Boundary** series. It brings the 360° full zero-gravity movement mechanics from the now-defunct tactical shooter *Boundary* into Minecraft.

You can play with it right away as a standalone mod: just grab the **Basic Backpack** from the Creative tab, equip it in your chestplate slot, and you're floating. It also serves as the core library for future addons (such as our upcoming Industry mod).

### Showcase Videos (Bilibili)
- [Zero-Gravity Movement Overview](https://www.bilibili.com/video/BV1i7dnBoExP/)
- [Firing, Shell Ejection & Zero-G Recoil](https://www.bilibili.com/video/BV15eJE6hE3T/)

### Features
- **Full 6DOF Zero-G Flight**: Pitch, yaw, and roll on all 3 axes with inertia, wall adhesion, and rotation limits in tight spaces.
- **Free Look**: Hold a key to look around freely without rotating your character's body. Third-person view also supports a locked free-look mode.
- **Multiplayer Ready**: Synced over server packets with custom zero-G hitboxes (AABB by default, switchable to OBB in server config). *Note: Due to high-frequency client rotation packets, other players' name tags might jitter slightly. It does not affect gameplay.*
- **Mod Integrations**:
  - **TaCZ**: Zero-gravity weapon handling, sight alignment, and recoil drift.
  - **Photon**: Realistic explosion particle effects.
  - Basic compatibility with vanilla and modded projectiles.

### Default Controls
Active when wearing the **Basic Backpack** (all rebindable in Options -> Controls):
- `W / A / S / D`: Strafe forward / backward / left / right
- `Space` / `Left Ctrl`: Move up / down (*Left Ctrl is also bound to Boost by default; feel free to change it*)
- `Q / E`: Roll left / right
- `H`: Manual brake
- `Tab + Inventory / Drop key`: Open inventory or drop items (avoids conflict with `Q / E` roll keys)
- `C`: Hold for Free Look; `Left Shift`: Toggle Locked Free Look (third-person only)

*Flight physics, speeds, and braking rates can be customized in `config/next-boundary.toml`.*

### Versions & Requirements
- **NeoForge 1.21.1 (Main Version)**: The primary version with all the latest features, fixes, and smoothest performance.
  - Requires NeoForge `21.1.238+`
  - Required client-side: **LDLib2** (`2.2.42+`)
  - Optional: **TaCZ** (`1.2.0-hotfix+` for NeoForge 1.21.1), **Photon**
- **Forge 1.20.1 (TaCZ Port)**: Ported specifically for the 1.20.1 ecosystem and TaCZ gunfights.
  - *Known limitation*: Due to older engine limits, third-person camera rotation may stutter slightly. First-person view remains smooth.

### License & Credits
- Released under **AGPL-3.0**. Full license available on [GitHub](https://github.com/WHX114514/Next-Boundary-Cornerstone/blob/master/LICENSE).
- Part of the camera roll logic references enjarai's [Do A Barrel Roll](https://github.com/enjarai/do-a-barrel-roll) (LGPL-3.0). See [CREDITS](https://github.com/WHX114514/Next-Boundary-Cornerstone/blob/master/CREDITS).

</details>

---

# 下一个边境-基石 (Next Boundary Cornerstone)

《下一个边境》系列的基础前置模组，把已关服的太空射击游戏《边境》（Boundary）里那套 360° 无重力操作搬进了 Minecraft。

单独装上就能直接玩：在创造模式物品栏里拿一个「简易移动背包」套在胸甲槽，就能进入失重状态；同时也给后续的附属模组（比如工业模组）提供底层支持。

### 演示视频（B站）
- [无重力操作简介](https://www.bilibili.com/video/BV1i7dnBoExP/)
- [无重力开火的抛壳与后座](https://www.bilibili.com/video/BV15eJE6hE3T/)

### 大致功能
- **六自由度无重力移动**：俯仰、偏航、横滚（Roll）三个轴都能随意转，带惯性漂浮、贴墙吸附以及狭窄空间转身限制。
- **自由视角**：可以按住按键转头环视四周而不带动身体；第三人称下还支持固定机位自由视角。
- **多人联机**：做好了服务端同步与失重碰撞箱适配（受击碰撞箱默认 AABB，可以在服务端配置里切 OBB）。*注：因为身体旋转发包频率较高，联机时其他玩家头顶的名字牌可能会有一点轻微抖动，不影响正常游玩。*
- **模组联动**：
  - **TaCZ**：适配了失重持枪瞄准、弹道起点修正以及开火后坐力推力。
  - **Photon**：失重爆炸特效。
  - 原版及常见投射物基础兼容。

### 默认按键
穿戴「简易移动背包」后生效（均可在游戏按键设置中修改）：
- `W / A / S / D`：前后移动与左右平移
- `空格` / `左 Ctrl`：上升 / 下降（*左 Ctrl 默认同时绑定了冲刺，觉得别扭可以改掉*）
- `Q / E`：向左 / 向右滚转（Roll）
- `H`：手动制动减速
- `Tab + 原版背包/丢弃键`：打开背包或丢东西（避免和 Q/E 滚转键冲突）
- `C`：按住进入自由视角；`左 Shift`：第三人称固定自由视角

*飞行速度、滚转平滑度、制动阻尼等物理参数可在 `config/next-boundary.toml` 中调节。*

### 版本说明与前置需求
- **NeoForge 1.21.1（主版本）**：主要维护版本，新功能和修复均优先更新此版本。
  - NeoForge 要求：`21.1.238+`
  - 客户端必需前置：**LDLib2**（`2.2.42+`）
  - 可选联动：官方版 **TaCZ** NeoForge 1.21.1（`1.2.0-hotfix+`）、**Photon**
- **Forge 1.20.1（TACZ 适配移植版）**：专为 1.20.1 模组环境及 TaCZ 枪械游玩移植。
  - *已知局限*：受限于旧版本实现，第三人称视角旋转可能会有轻微卡顿，第一人称完全流畅。

### 开源协议与致谢
- 基于 **AGPL-3.0** 协议开源，详见 [GitHub LICENSE](https://github.com/WHX114514/Next-Boundary-Cornerstone/blob/master/LICENSE)。
- 视角横滚逻辑部分参考学习了 enjarai 的开源模组 [Do A Barrel Roll](https://github.com/enjarai/do-a-barrel-roll)（LGPL-3.0），致谢说明见 [CREDITS](https://github.com/WHX114514/Next-Boundary-Cornerstone/blob/master/CREDITS)。
