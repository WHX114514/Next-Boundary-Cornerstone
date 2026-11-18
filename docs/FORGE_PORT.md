# 0.0.6 Forge 1.20.1 移植

基于 master 的 39ea558（0.0.6-test-26.11.15）移植，目标为 Minecraft 1.20.1、Forge 47.4.18、Java 17。

2026-11-16 修正版为 0.0.6-test-26.11.16：IntelliJ 启动项在生成时固定 Java 17，并在运行前准备 Forge 运行时文件；启动堆内存为 256 MB、上限 1536 MB。暂停菜单设置行在原版 GridLayout 定位前插入，与整个菜单一起排列，不再事后挤动按钮。

同步的功能包括自由视角与固定头部视角、贴面吸附、分段移动和受击碰撞盒、OBB 可选命中模式、受限转身与头部眼位、挖掘速度修复、Tab 背包切换、投掷物方向和起点修复，以及按开火触发的 TaCZ 失重后坐力。

平台适配：

- 玩家姿态通过 Forge capability 保存和恢复；保留旧的 PlayerRotationCapability 类型供附属模组使用，新代码可通过 PlayerRotationAttachment.get(player) 查询。兼容 0.0.4 的四元数 NBT，临时骨架与几何缓存不写入 NBT。
- 使用 Forge SimpleChannel 传输姿态、服务端约束与旁观者配置。协议已升级，客户端与服务端需同时更新。
- TaCZ 默认对接官方 Forge 1.20.1 的 1.2.0-hotfix，可通过 -PtaczRuntime=true 加入开发运行环境；不打入基石 JAR。`-PtaczRuntimeVersion=3NpuJ7TJ` 可切回 1.1.7-hotfix2。
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

`4yAbMXbz` 为官方 Forge TaCZ 1.2.0-hotfix；该轮换弹修复验证时默认仍为 1.1.7-hotfix2，下面的弹道修复将默认值切换至 1.2。`playerAnimatorRuntime` 只将官方 Forge Player Animator 1.0.2-rc1 加入开发运行环境。两者均不内置到基石发布包。

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

## 失重 TaCZ 弹道重复旋转修复（2026-11-16）

0.0.4 的 TaCZ 修正原本在子弹入场时撤销原版 yaw/pitch，再应用失重四元数。`39ea558` 新增的通用投射物修正通过 `9037984` 移植到 Forge 后，普通 TaCZ 子弹已在发射时按失重视角计算方向，旧入场处理又转换一次，造成偏弹。对照官方 1.1.7-hotfix2、1.2.0 和 1.2.0-hotfix，散布分发及子弹方向计算方法没有变化。

普通散布继续使用通用 `ProjectileLaunchMixin`，无需修改。TaCZ 的 `Vector2d` 自定义散布重载绕过原版发射方法，在现有可选 Mixin 中增加入口适配：由 TaCZ 计算相对角度下的局部散布速度，再调用共用的 `FlightAim.rotate`。旧入场处理仅减去继承的玩家速度，保留既有去惯性行为；落地时不减 Y 速度。眼位、伤害衰减起点、曳光、后坐力及玩家 tick 逻辑保持原有行为。

新增服务端回归通过真实枪械散布分发和自定义发射重载生成子弹，在入场后验证最终速度。52 个场景覆盖四种姿态、落地/离地、静止/移动、零散布/随机散布/自定义散布，以及退出失重后的普通重力对照。固定随机种子比较同一姿态的静止与移动弹道，保留原版随机散布产生的弹速波动。修改前该回归已失败：预期速度 `(0, 0, 3)`，入场后实际约为 `(1.195, -2.516, -1.114)`。

后台客户端 QA 增加侧翻/倒立时的四次真实 ADS/腰射，在服务端入场事件前后记录速度，检查方向、同步瞄准状态和去惯性结果。旧 QA 的单次 `aim(true)` 会被 TaCZ 按键轮询取消；现在通过游戏内 KeyMapping 状态保持开镜，并确认服务端瞄准进度为 1、腰射为 0。继续逐 tick 断言窗口不可见且未聚焦，不操作桌面键鼠。

```sh
# 默认 TaCZ 1.2.0-hotfix；旧版验证追加 -PtaczRuntimeVersion=3NpuJ7TJ
bash gradlew runGameTestServer -PtaczRuntime=true --no-parallel
bash gradlew runForgeClientQa -PtaczQa=true -PtaczRuntime=true -PtaczQaCases=4 -PqaHitboxMode=AABB --no-parallel
```

服务端日志见 `build/tacz-trajectory-before.log`（修复前失败）、`build/tacz-trajectory-after.log`（1.2 通过）、`build/tacz-trajectory-legacy.log`（1.1.7 服务端通过；其后的首次客户端尝试暴露了上述 QA 开镜保持问题）。最终客户端日志为 `build/tacz-trajectory-client-1.2.log` 和 `build/tacz-trajectory-client-legacy.log`；分组报告保存在 `build/trajectory-results/`。

本次最终验证（Java 17，macOS arm64）：

| 检查 | 结果 |
| --- | --- |
| 修复前新增弹道回归 | 普通零散布入场后方向断言失败，确认可复现 |
| TaCZ 1.2.0-hotfix / OBB 独立服务端 | 18 项 GameTest 通过，包括 52 个弹道场景、9 个眼位场景及原有 70 个通用投射物场景 |
| TaCZ 1.1.7-hotfix2 / OBB 独立服务端 | 同样 18 项 GameTest 通过，52 个弹道场景通过 |
| TaCZ 1.2 + Player Animator / AABB 隐藏客户端 | 4 组换弹、侧翻/倒立的 2 次 ADS 和 2 次腰射通过，服务端 ADS 进度为 1、腰射为 0 |
| TaCZ 1.1.7，不装 Player Animator / OBB 隐藏客户端 | 同样 4 组换弹及 4 次实射通过，服务端瞄准状态和实际弹道通过 |
| 不安装 TaCZ | 18 项 GameTest 通过（TaCZ 两项按未安装跳过），隐藏客户端完整基础验收通过 |
| 默认依赖下 build / jarJar | 通过；六组数学回归共 13,490 项断言通过 |
| 发布包 | Java class 61、32 个 refmap 条目、内置 MixinExtras；不含 QA、GameTest 或 TaCZ 实现类 |

完整构建及无 TaCZ 验证日志为 `build/tacz-trajectory-final-build.log`，发布包审计为 `build/tacz-trajectory-release-audit.json`。本次未使用玩家完整整合包或独立服务器双客户端进行验收。

## 失重死亡与进入末地崩溃修复：hotfix2

玩家报告 `crash-2026-11-16_22.26.55-client.txt` 与 `crash-2026-11-16_22.33.04-server.txt` 的异常均为 `Missing player rotation capability`。死亡发生在旧客户端玩家的 `Camera.tick → getEyeHeight`；主世界进入末地发生在服务端玩家流体检测的 `getBoundingBox → SegmentedPhysics.box`。两者均因生命周期过渡时强制读取已经失效的旋转能力而崩溃，后者还暴露出恢复后能力仍缺失的问题。

Forge 的 `reviveCaps()` 只恢复实体的外层访问权限，无法恢复本模组已失效的 `LazyOptional`。绑定玩家的 provider 现在在实体恢复访问后，为同一数据对象创建新的 optional，原先交给外部的 optional 保持失效；无玩家绑定的独立 provider 仍保持原有永久失效行为。克隆处理只临时恢复原本不可用的旧实体，并用 `try/finally` 恢复其有效性状态。

统一增加存活、未移除及能力可用性检查，覆盖分段眼位/碰撞和相关客户端 tick、远端渲染、姿态发送及网络处理入口。过渡时使用原版眼位和碰撞路径，停止旧玩家采样/发送；正常玩家仍通过严格的 `get` 读取，不创建临时默认旋转对象掩盖缺失。临时骨架和缓存不跨克隆复制。NBT、网络协议和 TaCZ 弹道行为未修改。

新增三项服务端 GameTest：失效玩家的眼位/碰撞安全回退；连续三轮能力失效与恢复（数据对象身份保持、旧引用保持失效）；死亡与非死亡克隆（旧能力有效/无效共四种情况）。修改前这三项均复现失败，修改后通过。

后台真实客户端验证通过：穿基础背包进入失重后死亡并立即重生，随后对死亡旧玩家调用真实 `Camera.setup/tick`；通过真实末地传送门方块执行两轮主世界→末地→主世界，返回时通过游戏 API 关闭终末之诗。每次到达均检查客户端和服务端能力及服务端复合碰撞箱恢复，逐 tick 断言窗口不可见且未聚焦，不使用桌面键鼠。

```sh
bash gradlew runGameTestServer --no-parallel
bash gradlew runForgeClientQa -PlifecycleQa=true --no-parallel
bash gradlew build jarJar runGameTestServer -PtaczRuntime=true --no-parallel
```

原始回归日志为 `build/player-lifecycle-before.log`，服务端验证为 `build/player-lifecycle-validation.log`，完整客户端结果为 `build/player-lifecycle-client.log`，最终构建及 TaCZ 回归为 `build/player-lifecycle-final.log`。本次在项目 Forge 47.4.18 / Java 17 的隔离环境验证；玩家报告使用 Forge 47.4.23，尚未运行原完整整合包或独立服双客户端验收。客户端和服务端均应更新到 hotfix2。

最终 `build / jarJar`、六组数学回归（13,490 项断言）及安装 TaCZ 时的全部 21 项 GameTest 通过，包含原有 52 个 TaCZ 弹道场景。不安装 TaCZ 的生命周期客户端，以及安装 TaCZ 1.2 + Player Animator 的生命周期客户端均通过真实死亡重生和四次末地传送。有效旧实体的克隆配对直接验证本处理器；完整事件总线另行检查状态复制，避免将第三方监听器对旧实体有效性的修改归于本处理器。

## 失重镜头与 TaCZ 原生动效适配（2026-11-17）

相机保留垂直防翻转思路，但不再在 `horiz=0.001` 附近切回原版 yaw。原版 yaw 已用于手部输入累计，与失重真实视角可能不同，切换会造成姿态跳变。渲染现在从归一化四元数的前向和上向基向量恢复等价角度，俯仰使用 `atan2`，以双精度中间计算避免极点精度损失；不会把这些渲染角度写回玩家输入。

基础视角在最高事件优先级确定。可选 TaCZ Mixin 调用一次原始世界镜头动画，采样相同俯仰、零翻滚下的相对旋转，再完整搬到当前镜头局部坐标系。保留原生动画的幅度、曲线与开火滚转，不修改 FOV、手部动画、后坐力数值或弹道；普通重力和非本地镜头直接走原始调用。

`CameraRotationTests` 检查随机姿态、非单位四元数、符号等价及正负垂直附近的 42,006 项往返断言。可选后台 QA 通过原始 renderer 方法生成独立参考，绕过适配 Mixin；检查 100 组镜头事件、2,000 步连续扫描，以及真实换弹和 ADS/腰射。可选探针不进入发布包：

```sh
JAVA_TOOL_OPTIONS='-Dcornerstone.qa.camera=true' bash gradlew runForgeClientQa -PtaczQa=true -PtaczRuntime=true -PtaczQaCases=4 -PplayerAnimatorRuntime=true -PqaHitboxMode=AABB --no-parallel
```

修复前输入每步约 `0.00038°`，跨垂直判断阈值时基础画面跳约 `0.119°`，固定 TaCZ 俯仰动效下跳约 `0.854°`。修复后连续扫描最大输出步长约 `0.00044°`，与输入步长一致；100 组样本的最大局部动效误差约 `0.000022°`，普通重力对原始 TaCZ 方法的偏差为 0。真实 AK 换弹局部动效误差由约 `1.015°` 降至约 `0.000013°`。

原始 QA 保存在 `build/camera-qa/`；修复后分组结果在 `build/camera-fix-qa/`。测试使用隔离隐藏窗口，不操作桌面键鼠；玩家完整整合包中的其他随机抖动仍需现场确认。

最终验证：TaCZ 1.2 + Player Animator / AABB、TaCZ 1.1.7 / OBB 的镜头对照、连续扫描、真实换弹与四次 ADS/腰射均通过；1.1.7 独立服务端 21 项 GameTest（含 52 个弹道场景）通过。不安装 TaCZ 的独立服务端 21 项及隐藏客户端基础验收通过。完整 `build` / `jarJar` 和七组数学测试共 55,496 项断言通过。

测试包版本为 `0.0.6-test-26.11.17-hotfix1`。日志为 `build/tacz-camera-fix-client.log`、`build/tacz-camera-fix-legacy.log` 和 `build/tacz-camera-fix-final-build.log`；结果汇总与发布包审计为 `build/camera-fix-qa/summary.json`。

## 失重通用发射与牵引兼容（2026-11-17，hotfix2）

公开世界朝向统一复用 `FlightAim`，手部输入累计独立保存；公共速度写入接入原有推进缓存，取消伪飞行和对公共输入的清空。未新增五个样本模组的生产适配器。测试版本、实现边界、独立运行对照、专服防飞行验证与复现命令见 [通用兼容验收说明](GENERIC_FLIGHT_COMPATIBILITY.md)。

## TaCZ 第一人称枪模抖动回归（hotfix3）

`8e34322` 将玩家公开角度改为真实世界朝向，供通用发射入口使用；但 TaCZ 两条第一人称渲染路径仍将该角度与屏幕轴的 `xBob/yBob` 相减。跨世界欧拉角分支时，枪模因此突跳，四元数镜头本身保持连续。与修改前 `e669ea8` 的同序列对照确认：yaw 跨界最大枪模步长从约 `0.06°` 增至 `35.94°`，跨俯仰极点增至约 `18°`。

新增可选 `TACZGunSwayMixin`，仅在失重能力有效时让这两处摆动读取现有 `HandSway`，不改公开世界角度、相机、弹道或牵引逻辑。同时修正原版手持渲染的两处读取时序：`LocalPlayer.getViewXRot` 使用当前帧角度，yaw 仅在乘坐时插值。TaCZ 的摆动幅度、后续根节点变换及动画继续使用原方法。

新增 opt-in 后台回归，实际调用 TaCZ wrapper 和基类渲染方法，在原始两次摆动旋转后采样矩阵；探针不替换角度读数。每条路径覆盖 yaw 跨界、俯仰极点、侧翻混合转动、侧翻静止、普通重力五组输入及五个 partial tick，共 450 个样本，与 `e669ea8` 记录的变换逐分量比较，另外检查逻辑瞄准和基础镜头未变。资源中的历史记录来自修复前对照，不按修复公式生成。探针和数据均不进入发布包。

```sh
bash gradlew runForgeClientQa -PtaczRuntime=true -PgunModelQa=true -PqaDirectory=build/gun-model-fix-qa/tacz12-model
bash gradlew runForgeClientQa -PtaczRuntime=true -PtaczRuntimeVersion=3NpuJ7TJ -PgunModelQa=true -PqaHitboxMode=OBB -PqaDirectory=build/gun-model-fix-qa/tacz117-model
bash gradlew runForgeClientQa -PtaczRuntime=true -PtaczQa=true -PtaczQaCases=4 -PcameraQa=true -PplayerAnimatorRuntime=true -PqaDirectory=build/gun-model-fix-qa/tacz12-live
```

实际结果（Java 17 / macOS arm64，隐藏且不聚焦的客户端）：

| 验收 | 结果 |
| --- | --- |
| TaCZ 1.2.0-hotfix / AABB；1.1.7-hotfix2 / OBB | 各 450 个真实枪模变换样本通过，与历史记录的四元数分量最大误差均为 0；枪模最大步长恢复到约 `0.0671°`，静止为 0，基础镜头及逻辑瞄准不变 |
| 两版本实际换弹、ADS/腰射及镜头动画 | 各四组换弹、四次实射、100 组镜头对照与连续扫描通过；1.2 同时安装 Player Animator |
| TaCZ 1.2 服务端 | 22 项 GameTest 通过，含 52 个弹道、9 个眼位和 70 个通用投射物场景，以及药水/弓/弩入口 |
| 不安装 TaCZ | 31 条公共输入/发射检查、22 项 GameTest 及原有设置、登录、失重、第三人称渲染、粒子和暂停菜单后台验收通过；TaCZ 专项按缺失跳过 |
| 构建及发布包 | `build / jarJar`、七组共 55,496 条数学断言通过；Java class 61，新增角度读取的两条 SRG 映射存在，`all.jar` 内置 MixinExtras，不含 QA、历史样本或 TaCZ 实现类 |

结果和日志保存在 `build/gun-model-fix-qa/`，包审计与 SHA-256 见 `release-audit.json`。临时游戏目录、测试世界和旧诊断探针已清理，正式回归源码保留。修改仅在 `1.20-forge`，版本 `0.0.6-test-26.11.17-hotfix3`，未提交或推送。未使用玩家完整整合包做视觉验收。

## 通用第一人称摆动适配（0.0.6-hotfix5）

LR 的近战武器和 TaCZ Fixes 的左手枪各自实现了第一人称渲染，都用公开世界朝向减去屏幕轴摆动值。上一版修复覆盖了 TaCZ 本体的两条路径。扩展渲染仍会在失重视角跨界时出现约 36° 的旋转跳变。

在 `GameRenderer` 调用共用手持渲染入口时，将本地失重玩家的当前及历史摆动值临时转换到世界角度。原版和 Forge 手持事件中的渲染器保留各自的插值、动画和变换。作用域结束后恢复原值，包含嵌套调用和异常退出。公开瞄准角度仍是真实世界朝向，原版手持动画的屏幕轴取值及自由视角变换保留。

LR 0.4.4 和 TaCZ Fixes 2.3.0-hotfix2 通过临时 init script 加入隔离运行环境。常规编译、开发运行和发布包无需这两个扩展。使用 TaCZ 1.2.0-hotfix，通过真实手持入口记录 LR 弯刀及 Fixes 双持左手 Glock 的 450 个样本，与 `e669ea8` 的正常数据相比最大旋转误差约 0.00000154°。静止手部变换相对模型根节点的矩阵分量最大误差约 0.00000250。另采样 133 帧挥砍、左右手开火与换弹，服务端确认每侧各一发入场，双手换弹均完成。验收范围是内置模型，玩家完整整合包和自定义武器资源未纳入。

- TaCZ 1.2 与 1.1.7 各通过 450 个枪模样本、四组换弹、四次开镜或腰射，以及镜头对照和极点连续扫描。TaCZ 1.2 同时安装 Player Animator。各版本均通过 22 项服务端 GameTest，其中包括 52 个弹道与 9 个眼位场景。
- 基石独立运行通过 AABB/OBB 各 31 个检查、175 tick 飞行轨迹对照、死亡重生、两轮末地往返及原生设置与渲染。五款扩展的姿态与重力切换矩阵分别通过 295 和 186 项检查。实际 TCP 独立服通过 260 tick 失重悬停及恢复重力后的原版浮空踢出。最先使用的 localhost 端口握手超时，修复前和新版在 25581 端口都通过。
- `build / jarJar`、55,496 项数学断言、无 TaCZ 的 22 项 GameTest 通过。`all.jar` 内置 MixinExtras，新增入口的混淆映射已核对，包内无 QA 类或扩展模组。结果及 SHA-256 保存在 `build/hotfix5-qa/`。
