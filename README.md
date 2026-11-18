# 下一个边境-基石 (Next Boundary Cornerstone)

**简体中文** | [English](README_EN.md)

[![Modrinth](https://img.shields.io/modrinth/dt/next-boundary-cornerstone?style=flat-square&logo=modrinth&label=Modrinth&color=00AF5C)](https://modrinth.com/mod/next-boundary-cornerstone)
[![Modrinth Version](https://img.shields.io/modrinth/v/next-boundary-cornerstone?style=flat-square&logo=modrinth&label=Version)](https://modrinth.com/mod/next-boundary-cornerstone/versions)
[![Minecraft](https://img.shields.io/modrinth/game-versions/next-boundary-cornerstone?style=flat-square&label=Minecraft)](https://modrinth.com/mod/next-boundary-cornerstone/versions)
[![Loader](https://img.shields.io/badge/Loader-NeoForge%201.21.1%20%7C%20Forge%201.20.1-orange?style=flat-square)](https://modrinth.com/mod/next-boundary-cornerstone/versions)
[![License](https://img.shields.io/github/license/WHX114514/Next-Boundary-Cornerstone?style=flat-square&label=License)](LICENSE)
[![MC百科](https://img.shields.io/badge/MC%E7%99%BE%E7%A7%91-26602-2B8AE2?style=flat-square)](https://www.mcmod.cn/class/26602.html)
[![Bilibili](https://img.shields.io/badge/Bilibili-%E6%BC%94%E7%A4%BA%E8%A7%86%E9%A2%91-FB7299?style=flat-square&logo=bilibili&logoColor=white)](https://www.bilibili.com/video/BV1i7dnBoExP/)

《下一个边境》系列的基础模组，把已关服的游戏《边境》（Boundary）里那套 360° 无重力操作搬进了 Minecraft。

单独装也能直接玩：在创造模式物品栏里拿一个「简易移动背包」穿到胸甲槽，就能进入失重状态；同时也给后续的附属模组（比如工业模组）提供底层支持。

## 相关链接

- Modrinth：https://modrinth.com/mod/next-boundary-cornerstone
- MC百科：https://www.mcmod.cn/class/26602.html
- B站演示视频：
  - [无重力操作简介](https://www.bilibili.com/video/BV1i7dnBoExP/)
  - [无重力开火的抛壳与后座](https://www.bilibili.com/video/BV15eJE6hE3T/)

## 大致功能

- **六自由度无重力移动**：俯仰、偏航、横滚三个轴都能随便转，带惯性漂浮、贴墙吸附和狭小空间转身限制。
- **自由视角**：可以按住快捷键转头看四周，第三人称还支持固定自由视角。
- **多人联机**：做了服务端同步和失重碰撞箱适配（受击碰撞箱默认 AABB，可在服务端配置里切 OBB）。因为旋转同步频率比较高，联机时其他玩家头顶的名字牌可能会有一点显示小瑕疵，不影响正常游玩。
- **模组兼容**：装了 TaCZ 可以体验失重持枪、弹道修正和开火后坐力，装了 Photon 会有爆炸特效；原版和其他模组的常见投射物也做了基础兼容。

## 默认按键

穿戴「简易移动背包」后生效，按键都可以在游戏里的按键设置里改：

- `W / A / S / D`：前后左右平移
- `空格` / `左 Ctrl`：上移 / 下移（左 Ctrl 默认也绑着冲刺，觉得别扭可以自己改）
- `Q / E`：向左 / 向右滚转
- `H`：手动制动
- `Tab + 原版背包/丢弃键`：打开背包或丢弃物品（防止和 Q/E 滚转冲突）
- `C`：按住进入自由视角；`左 Shift`：第三人称固定自由视角

速度、滚转手感、制动倍率这些参数可以在 `config/next-boundary.toml` 里调。

## 分支与环境

仓库目前有两个主要分支：

- **`master`（主分支，Minecraft 1.21.1 / NeoForge）**
  - 主力维护版本，新功能和修复优先往这边更。
  - 需要 NeoForge `21.1.238+`，客户端必装 **LDLib2**（`2.2.42+`）。
  - 可选兼容：官方版 **TaCZ** NeoForge 1.21.1（`1.2.0-hotfix+`）、**Photon**（客户端爆炸特效）。
- **`1.20-forge`（移植分支，Minecraft 1.20.1 / Forge）**
  - 为 1.20.1 生态和 TaCZ 做的移植版。
  - 已知问题：受旧版本限制，第三人称旋转视角时可能会有点卡顿，第一人称正常。

详细更新记录见 [CHANGELOGS.md](CHANGELOGS.md)。

## 开源协议与致谢

- 本项目基于 **AGPL-3.0** 协议开源，详见 [LICENSE](LICENSE)。
- 部分视角横滚逻辑参考了 enjarai 的 [Do A Barrel Roll](https://github.com/enjarai/do-a-barrel-roll)（LGPL-3.0），详见 [CREDITS](CREDITS)。
