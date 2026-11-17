# TaCZ 失重弹道修复（1.21.1 NeoForge）

当前对接官方 TaCZ 1.2.0-hotfix（`tJiZL0He`）。通用投射物发射入口已经应用失重视角，旧 `ServerBulletHandler` 又在子弹入场时撤销原版角度并旋转，导致普通散布路径重复转换。

修复仅涉及两处生产代码：普通子弹继续走通用发射逻辑，旧入场处理只保留去除玩家惯性；TaCZ 自定义 `Vector2d` 散布入口生成局部速度后复用 `FlightAim.rotate`。保留落地时不扣 Y 速度的规则；眼位、伤害衰减起点、曳光和后坐力代码不变。无新增 Mixin 注册项、公开 API 或网络字段。

服务端回归由 Forge 分支移植，覆盖 52 个实际子弹入场后的弹道场景：普通零散布、随机散布、自定义散布，侧翻、倒立、混合旋转、越过 90° 俯仰，以及静止/移动、落地/离地和退出失重。固定随机种子比较静止与移动弹道，保留原版散布产生的速度波动。

Java 21 验证命令：

```sh
bash gradlew build runGameTestServer -PtaczRuntime=true --no-parallel
bash gradlew runGameTestServer --no-parallel
```

2026-11-16 验证结果：

- 修改前新增回归失败：零散布预期速度 `(0, 0, 3)`，入场后约为 `(1.195, -2.516, -1.114)`。
- 修改后 TaCZ 独立服务端全部 17 项 GameTest 通过，包括新增 52 个弹道场景、原有眼位和通用投射物回归。
- 未安装 TaCZ 的独立服务端全部 17 项通过，TaCZ 两项按未安装跳过。
- `build` 通过，六组数学回归共 13,490 项断言通过。发布包为 Java class 65，不含 QA、GameTest 或 TaCZ 实现类。
- 隐藏客户端现有 TaCZ 场景通过：第一人称曳光偏移不重复旋转/缩放，后坐力及延迟抛壳回归通过，完成实际帧缓冲截图。

日志分别为 `build/tacz-trajectory-neoforge-before.log`、`build/tacz-trajectory-neoforge-after.log` 和 `build/tacz-trajectory-neoforge-standalone.log`。客户端现有曳光偏移与后坐力 QA 日志为 `build/tacz-trajectory-neoforge-client.log`；客户端实际 ADS/腰射的新回归仍仅在 Forge 分支运行，未使用玩家完整整合包或独立服双客户端验收。

## 镜头连续性及原生动效适配（2026-11-17）

同步 Forge 分支 e669ea8 已验证的镜头修复，适配 NeoForge 的事件类型与构造器：

- 从归一化四元数的前向、上向基向量恢复镜头角度，使用双精度中间计算及 `atan2` 俯仰；垂直附近不再回退到用于手部输入的原版 yaw。
- 基础镜头先确定，随后仅对失重本地镜头将 TaCZ 在未翻滚参考系中的原始动画相对旋转搬到当前局部坐标系。保持原始动画曲线、幅度和开火滚转，不改 FOV、手部动画、后坐力数值或弹道；普通重力走原始调用。
- 新 Mixin 仅注册在客户端列表，未安装 TaCZ 时禁用。不修改网络协议。

Java 21 的 `build` 和 42,006 项角度往返断言通过；包含原有数学回归共 55,496 项断言。安装 TaCZ 的独立服务端全部 17 项 GameTest 通过。

隐藏客户端通过真实 TaCZ renderer 和事件总线执行 100 组普通/失重镜头对照及 2,000 步垂直连续扫描：普通路径与原始函数偏差为 0，失重局部动效最大偏差约 `0.000022°`，连续输入最大输出步长约 `0.00044°`，与输入步长一致。原有曳光、后坐力及延迟抛壳检查也通过。对照直接调用原始 renderer 方法，绕过适配 Mixin；没有修改枪械资源或关闭视角摇晃。

```sh
bash gradlew build compileClientQaJava runGameTestServer -PtaczRuntime=true --no-parallel
# 使用单独 QA 游戏目录，并预置 config/fml.toml 的 earlyWindowControl=false
JAVA_TOOL_OPTIONS='-Dapple.awt.UIElement=true -Djava.awt.headless=true' bash gradlew runHitboxVisualClient -PtaczRuntime=true -PhitboxVisualScenario=tacz -PhitboxVisualDir=build/camera-neoforge-fix-client --no-parallel
```

测试包版本为 `0.0.6-test-26.11.17-hotfix1`。结果为 `build/camera-neoforge-fix-client/camera-qa-matrix.json`、`camera-qa-sweep.json`；日志为 `build/camera-neoforge-fix-build.log`、`build/camera-neoforge-fix-client.log`。本轮没有重复 NeoForge 客户端实际开火/换弹全过程采样或玩家完整整合包验收。

无 TaCZ 的 17 项 GameTest（TaCZ 两项按未安装跳过）和隐藏客户端基础场景通过，日志为 `build/camera-neoforge-fix-standalone.log`。发布包审计为 `build/camera-neoforge-fix-audit.json`：Java class 65，新镜头 Mixin 仅在客户端注册，不含 QA、测试类或 TaCZ 本体。

## 第一人称枪模抖动回归（hotfix3）

master 的通用发射兼容同样把玩家公开角度改为世界朝向，而 TaCZ 的基类与 wrapper 第一人称渲染仍将其与屏幕轴 `xBob/yBob` 相减。移植 Forge 的最小修复：新增可选 `TACZGunSwayMixin`，两条渲染路径复用现有 `HandSway`；同时让原版手持渲染保留 `LocalPlayer` 的取值时序（pitch 取当前帧，yaw 仅在乘坐时插值）。公开世界角度、相机、弹道及通用牵引逻辑不变。

后台回归也移植到 NeoForge 事件和 1.21.1 建世界 API。它直接调用两条真实 TaCZ 渲染路径，在摆动旋转后读取矩阵；探针不替换角度读数。覆盖 yaw 跨界、俯仰极点、混合翻滚、静止和普通重力，共 450 个样本，与 Forge 修复前正常版本 `e669ea8` 的记录比较。已核对 1.21.1 的原生 view getter 与 1.20.1 具有相同取值时序。

```sh
bash gradlew runFlightCompatibilityClient -PtaczRuntime=true -PgunModelQa=true -PqaDirectory=build/gun-model-121-fix-qa/after
# 实际换弹、四次 ADS/腰射及子弹入场后的方向/速度
bash gradlew runFlightCompatibilityClient -PtaczRuntime=true -PqaDirectory=build/gun-model-121-fix-qa/live
bash gradlew build runGameTestServer -PtaczRuntime=true
# 不安装 TaCZ
bash gradlew runFlightCompatibilityClient runGameTestServer -PqaDirectory=build/gun-model-121-fix-qa/standalone
```

本次实际验证（Java 21，NeoForge 21.1.238，TaCZ `tJiZL0He`）：

- 修改前相同回归在 `yaw-wrap / step 3 / partial 0` 失败，四元数分量偏差约 `0.3091`；修改后两条真实渲染路径的 450 个样本全部通过，与历史正常样本的最大分量误差为 0。基础镜头及逻辑瞄准不变，普通重力对照通过。
- 实际换弹及两次 ADS、两次腰射通过，检查服务端真实瞄准状态、子弹加入世界后的方向/速度与去惯性行为；连同公共入口共 48 条断言通过。曳光、后坐力、100 组镜头对照和 2,000 步连续扫描通过。
- 安装和不安装 TaCZ 的服务端各 19 项 GameTest 通过；无 TaCZ 的隐藏客户端 31 条公共功能断言通过，TaCZ 专项按未安装跳过。
- `build` 与七组共 55,496 项数学断言通过。发布包为 `0.0.6-test-26.11.17-hotfix3`、Java class 65；新 Mixin 仅在客户端注册且可选启用，不含 QA、历史样本或 TaCZ 实现类。

日志、结果和包审计见 `build/gun-model-121-fix-qa/`。后台窗口不可见且不聚焦，不使用桌面键鼠；未验证玩家完整整合包。已清理隔离游戏目录、测试世界及临时反汇编文件，保留正式回归源码与报告。修改仅在 master，未提交或推送。
