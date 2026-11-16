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

## TaCZ 换弹兼容修复：0.0.6-test-26.11.16-hotfix1

11 月 16 日报告的客户端崩溃发生在骨架采样：`Invalid hitbox joint pivot`。原先所有关节都要求 `|X/Z| <= 8`，而 TaCZ 1.2.0-hotfix 自带的步枪换弹动画左臂 Z 位移可达 `-8.375`，正常动画即可超过旧限制。相关采样/校验逻辑与 1.21.1 master 一致；此前 Forge 验收使用 TaCZ 1.1.7-hotfix2，未覆盖真实持枪换弹。

本修复扩展有界关节位移，增加模型重置和逐关节采样回退，并将第一人称手臂绘制与完整人物骨架回写隔离。Forge 协议升级为 `compound-physics-forge-9`，客户端、服务器需同时更新，不改变姿态包字段。

真实第三人称回归还发现 JOML 1.10.5 的 `getEulerAnglesZYX` 在提取 X 角时使用了错误的分母符号，导致回写后的模型旋转偏离采样四元数。骨架回写改用独立的 ZYX 分解，包含 Y 轴正负 90° 的等价姿态处理；480 组普通、越过 90° 和临界角往返断言覆盖该问题。

新增后台客户端场景（Java 17）：

```sh
bash gradlew runForgeClientQa -PtaczQa=true -PtaczRuntime=true -PtaczRuntimeVersion=4yAbMXbz -PplayerAnimatorRuntime=true -PqaHitboxMode=AABB --no-parallel
bash gradlew runForgeClientQa -PtaczQa=true -PtaczRuntime=true -PtaczRuntimeVersion=4yAbMXbz -PplayerAnimatorRuntime=true -PqaHitboxMode=OBB --no-parallel
```

`4yAbMXbz` 为官方 Forge TaCZ 1.2.0-hotfix；不传 `taczRuntimeVersion` 保留 1.1.7-hotfix2。`playerAnimatorRuntime` 只将官方 Forge Player Animator 1.0.2-rc1 加入开发运行环境。两者均不内置到基石发布包。

场景通过真实客户端枪械接口执行 AK47/Glock 的战术与空仓换弹，覆盖第一/第三人称、普通/细手臂、重力切换，逐 tick 检查骨架并读取实际手臂渲染结果；另检查真实瞄准开火、服务端收到 16 个受击盒和 10 个运动盒。报告为 `build/forge-client-qa/client-report.txt`，场景记录为 `tacz-scenarios.txt`，游戏帧缓冲截图位于 `tacz-frames/`。

QA 禁用 Forge early window，GLFW 窗口不可见、禁止获取焦点，并屏蔽显示/激活调用；每 tick 断言不可见且未聚焦。测试仅通过游戏 API 驱动，不使用 Computer Use、桌面键鼠或窗口激活。测试模组及探针不进入发布包。

最终验证（2026-11-16，macOS arm64，Oracle JDK 17.0.12）：

| 检查 | 结果 |
| --- | --- |
| 默认 1.1.7-hotfix2 编译依赖下的 `build`、`jarJar`、测试夹具编译 | 通过 |
| 眼位、自由视角、分段盒、OBB、复合碰撞、贴面移动数学回归 | 六组共 13,490 项断言通过，包含新增网络校验、逐关节回退和 480 组旋转往返 |
| 独立服务端 AABB / TaCZ 1.2.0-hotfix | 17 项 GameTest 通过，包含 9 种 TaCZ 子弹起点场景 |
| 独立服务端 OBB / TaCZ 1.1.7-hotfix2 | 17 项 GameTest 通过，日志确认 `HITBOX_SERVER_MODE_PASS mode=OBB` |
| TaCZ 1.2.0-hotfix + Player Animator，AABB 客户端 | 32 组真实换弹通过，左右手各检查 3,357 次，完整人物骨架检查 728 次 |
| TaCZ 1.2.0-hotfix + Player Animator，OBB 客户端 | 32 组真实换弹通过，左右手各检查 3,375 次，完整人物骨架检查 728 次；另覆盖翻滚和自由视角 |
| TaCZ 1.1.7-hotfix2，不装 Player Animator | 4 组基础换弹、瞄准开火、服务器骨架检查通过 |
| TaCZ 1.2.0-hotfix，不装 Player Animator | 4 组基础换弹、瞄准开火、服务器骨架检查通过 |
| 完全未安装 TaCZ 的后台客户端 | 设置页、登录、失重、物理骨架、第三人称背包/调试绘制、爆炸粒子和暂停菜单检查通过 |
| 发布包审计 | Java class 61，生产 refmap 完整，内置 MixinExtras；不含 QA、测试类或 TaCZ 依赖 |

两种模式均在真实换弹过程中采样到左臂 `Z=-8.375`，通过原先会崩溃的边界，并验证瞄准开火成功、服务端保留 16 个受击段和 10 个运动段。TaCZ 场景逐 tick 断言窗口隐藏且未获取焦点；实际游戏帧缓冲截图已检查，没有使用 Computer Use 或桌面键鼠输入。

分组报告与截图保存在 `build/forge-client-qa/results/{aabb,obb,legacy,without-animator,standalone}/`；完整构建日志为 `build/final-verification.log`，发布包审计为 `build/release-audit.json`。`-PtaczQaCases=4` 可运行基础兼容场景，默认仍为完整 32 组。

以上为隔离环境结果。尚未使用玩家原整合包中的全部扩展模组回归，也未执行独立服务器双客户端实机验收；姿态包编解码、服务端复核已由 GameTest 和集成服务器场景覆盖。
