# 0.0.6 Forge 1.20.1 移植

基于 master 的 39ea558（0.0.6-test-26.11.15）移植，目标为 Minecraft 1.20.1、Forge 47.4.18、Java 17。

2026-11-16 修正版为 0.0.6-test-26.11.16：IntelliJ 启动项在生成时固定 Java 17，并在运行前准备 Forge 运行时文件；启动堆内存为 256 MB、上限 1536 MB。暂停菜单设置行在原版 GridLayout 定位前插入，与整个菜单一起排列，不再事后挤动按钮。

同步的功能包括自由视角与固定头部视角、贴面吸附、分段移动和受击碰撞盒、OBB 可选命中模式、受限转身与头部眼位、挖掘速度修复、Tab 背包切换、投掷物方向和起点修复，以及按开火触发的 TaCZ 失重后坐力。

平台适配：

- 玩家姿态通过 Forge capability 保存和恢复；保留旧的 PlayerRotationCapability 类型供附属模组使用，新代码可通过 PlayerRotationAttachment.get(player) 查询。兼容 0.0.4 的四元数 NBT，临时骨架与几何缓存不写入 NBT。
- 使用 Forge SimpleChannel 传输姿态、服务端约束与旁观者配置。协议已升级，客户端与服务端需同时更新。
- TaCZ 继续使用 Forge 1.20.1 的 1.1.7-hotfix2，可通过 -PtaczRuntime=true 加入开发运行环境；不打入基石 JAR。
- 设置窗口采用原版组件。附属页面返回 List<AbstractWidget>，详见 CLIENT_PREFERENCES.md。
- 高爆视觉使用 Forge 原生粒子实现，保留 master 的贴图；不依赖 Photon 2 或 LDLib2。
- 1.20.1 没有原版 SCALE 属性；运行时使用标准玩家尺寸。几何数学测试仍覆盖不同 scale。
- 未引用的 Blockbench 物品模型源文件移至 tools/models，避免原版资源加载器因设计稿中的任意角度报错；背包物品图标与穿戴模型保持各自原有的加载方式。

验证命令：

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTestServer
.\gradlew.bat -PtaczRuntime=true runGameTestServer
.\gradlew.bat runForgeClientQa
```

GameTest 使用 build/forge-game-test，客户端验收使用 build/forge-client-qa/game，不操作日常 run 目录的存档。测试源集不打入发布包。发布使用 build/libs 下带 all 后缀、内置 MixinExtras 的 Forge JAR。

## 本次验证结果（2026-11-15）

| 验证 | 结果 |
| --- | --- |
| Java 17 编译、重新混淆和打包 | 通过 |
| 眼位、自由视角、分段盒、OBB、复合碰撞、贴面移动数学测试 | 6 组，12,970 项断言通过 |
| 默认 AABB、未安装 TaCZ 的独立服务端 | 17 项 GameTest 通过 |
| OBB、安装 Forge TaCZ 的独立服务端 | 17 项 GameTest 通过，包括 9 种子弹起点情形 |
| 不安装 LDLib2 的独立设置页 | 配置工厂、键位、扩展分页、重复 ID、滚动、切页状态、重开、完成和 Esc 通过 |
| 安装 Forge TaCZ 的客户端测试世界 | 登录同步、背包失重、骨架采样、第三人称背包和调试盒、爆炸粒子、暂停菜单入口通过 |
| 发布包检查 | Java class 61、生产 refmap、内置 MixinExtras 正常；不包含测试模组 |

这些检查覆盖自动化场景；附属模组的完整整合包仍需按其 Forge 版本和接口适配情况联调。
