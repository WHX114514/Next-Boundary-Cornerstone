# NeoForge 1.21.1 失重通用兼容

本次在 `master` 将 Forge 1.20.1 的通用修复迁移到 NeoForge 1.21.1，起点为 `68a9ace`。保留原有推进、刹车、碰撞、相机和投射物架构，不增加针对验证样本的生产适配器。

## 实现

- `FlightAim` 发布与四元数一致的世界 pitch/yaw，公共 look/view 向量使用同一朝向；显式上一帧角度避免再次旋转。原有通用投射物发射、局部散布/抛射偏移和 TaCZ 专项继续复用。
- `HandSway` 独立保存屏幕轴鼠标累计，只替换原版手部及 bob 平滑的读取，避免世界角度跨极点或翻滚时污染手部摆动。
- `FlightVelocity` 从公共 Vec3 setter 接入原有推进缓存，涵盖 DDD、push、lerpMotion 的委托调用；只屏蔽特定原版维护写入和自己的回写，不再检查调用栈包名。覆盖 `Entity.move/moveRelative` 及 `LivingEntity.handleRelativeFrictionAndCalculateMovement` 的具体 setter，避免碰撞后的零速度覆盖贴附接近速度；不屏蔽整个移动方法或其第三方回调。
- 保留公开按键和输入字段，只抑制原版自身的重复推进、跳跃、蹲伏/姿态包和疾跑消费；不再强制开启飞行或将飞行速度设为零，原有失重 FOV 单独保留。
- NeoForge 通过移除自己的 `CREATIVE_FLIGHT` 修饰符撤回旧授权，保留其他来源修饰符。服务端仅对实际失重玩家豁免浮空检查，其他移动和碰撞检查继续生效。
- 1.21.1 的网络发送入口使用三参数 `Connection.send`，兼顾直接调用及另外两个重载；当前连接在姿态有变化时先发送既有旋转数据，覆盖自定义模组发射键。使用原有 data attachment 和数据包，没有增加网络字段。

第三方投射物的 tick、重力、阻力、散布、伤害、速度继承、返回和寻路策略均由原模组处理。绕过公共方向/速度入口的独立物理系统仍可能需要专门适配。

## 验证环境与样本

Java 21、NeoForge 21.1.238、Minecraft 1.21.1、macOS arm64。客户端使用隔离目录和隐藏窗口，不聚焦、不操作系统键鼠；公共兼容驱动逐 tick 检查 GLFW 可见与聚焦状态。

| 模组 | 实测版本 | 样本 |
| --- | --- | --- |
| [Rehooked](https://modrinth.com/mod/rehooked/version/nICxAK2S) | 1.9.4-1.21.1，Curios 9.5.1 | 铁钩真实网络发射；背包与无背包失重的横/上/下牵引、跳脱惯性 |
| [Advanced Hook Launchers](https://modrinth.com/mod/advanced-hook-launchers/version/v61aYHS7) | 11.1.1.1，ForgeEndertech 12.1.5.0 | 原生发射键和原生散布 |
| [Spartan Weaponry Unofficial](https://modrinth.com/mod/spartan-weaponry-unofficial/version/ygaWB8ji) | 1.21.1-1.2.3 | 飞刀、标枪、回旋镖；持续下坠、返回/回收；此项为第三方移植版 |
| [Immersive Engineering](https://modrinth.com/mod/immersiveengineering/version/uNRARSH2) | 12.4.2-194 | 充能电磁炮、装弹左轮真实使用/松开与最终速度 |

样本只通过 `-PcompatQaModsDir=...` 进入 QA 运行时，不作为发布依赖。QA 针对 1.21.1 调整注册名、Curios Optional、NeoForge 能量能力及回旋镖返回接口。Rehooked 1.21 在 ClientTick.Post 生成牵引速度，位移从下一 tick 开始断言；保留原版小于 0.003 的速度分量归零规则。

```sh
# 把上述版本及必需依赖的官方 JAR 放入指定目录
bash gradlew runFlightCompatibilityClient -PcompatQaModsDir=build/general-compat-121-qa/mods -PqaDirectory=build/general-compat-121-qa/mod-matrix
# 移动射手、普通重力和退出失重
bash gradlew runFlightCompatibilityClient -PtransitionQa=true -PcompatQaModsDir=build/general-compat-121-qa/mods -PqaDirectory=build/general-compat-121-qa/transitions
# 原生 TaCZ 换弹、侧翻/倒立 ADS 与腰射；含现有镜头/后坐力回归
bash gradlew runFlightCompatibilityClient -PtaczRuntime=true -PqaDirectory=build/general-compat-121-qa/tacz-live
# 清除可选模组后独立运行
bash gradlew runFlightCompatibilityClient -PqaHitboxMode=OBB -PqaDirectory=build/general-compat-121-qa/standalone-obb
bash gradlew runFlightSurfaceClient -PqaDirectory=build/general-compat-121-qa/standalone-surface
bash gradlew runFlightPreferencesClient -PqaDirectory=build/general-compat-121-qa/standalone-preferences
bash gradlew build runGameTestServer
```

真实专服先运行 `bash gradlew runFlightCompatibilityServer`，就绪后运行 `bash gradlew runFlightCompatibilityClient -PdedicatedQa=true -PqaDirectory=build/general-compat-121-qa/dedicated-client`。专用目录仅监听 `127.0.0.1:25579`，`allow-flight=false`；测试先悬停 260 tick，再移除背包并故意持续悬停，检查原版浮空踢出。该 opt-in 准备任务生成测试 EULA/配置，服务端自动退出。

## 本次记录

| 检查 | 结果 |
| --- | --- |
| 未修改 master 公共入口基线 | 记录 25/31 条失败；迁移后 31 条通过 |
| 四模组同时安装、六种失重姿态 | 265 条断言通过，含真实发射、持续下坠、回旋返回和 Rehooked 牵引/跳脱 |
| 移动射手、普通重力、退出失重 | 175 条断言通过；1.21.1 速度继承使用 `getKnownMovement`，左轮按原生 `shoot(...,3,1)` 的散布界限检查 |
| TaCZ 1.2.0-hotfix 后台实射 | 48 条断言通过，含一次真实换弹及侧翻/倒立的四次 ADS/腰射；原有 100 组镜头及连续扫描、曳光偏移和后坐力回归通过 |
| 真实 TCP 专服 | 260 tick 生存失重悬停通过；移除背包后原版记录 63 tick 浮空并踢出，`allow-flight=false` |
| 原有贴附独立对照 | 原 master 和最终实现均通过同一套未放宽的 25 项断言，含六面捕获/停驻/脱离、传送/重力切换、斜向落地及八组通道转向 |
| 最后维护写入修正后 | 重新通过 Rehooked 103 条断言及纯基石 OBB 公共入口 31 条；设置界面独立验收通过 |
| 175 tick 操作序列对照 | 位移最大差 `1.42e-14` 格（浮点舍入量级），推进缓存和 FOV 逐项相同 |
| 构建与原有数学回归 | `build` 通过，七组共 55,496 条断言通过 |
| 独立服务端 | 安装和不安装 TaCZ 各 19 项 GameTest 通过；未安装时 TaCZ 专项跳过。通用投射物实际覆盖 70 个发射场景和药水/弓/弩入口；另验证其他来源飞行属性不会被移除 |

独立贴附验收发现了一次真实回归：扩大速度捕获后，原版相对移动/摩擦方法将碰撞产生的零速度写入推进缓存，使西侧贴附前的接近速度丢失。临时 QA 探针确认具体调用后，仅为这些维护 setter 增加排除，保留全部原有断言；原 master 与最终版本均完整通过 25 项。探针源码和注册已移除。

最终测试包为 `build/libs/next-boundary-cornerstone-neoforge1.21.1-0.0.6-test-26.11.17-hotfix2.jar`。审计为 Java class 65、43 项 Mixin，不含 QA、GameTest、临时探针或验证模组实现类。结果、原始检查记录及 SHA-256 在 `build/general-compat-121-qa/results/`；最终构建和独立 GameTest 日志为 `build/general-compat-121-qa/final-validation.log`。本次未提交、未推送。

临时模组 JAR、世界、源码/反汇编探针与基线副本均已清理；保留必要报告、测试源码和常规 Gradle 缓存。

辅助钩爪样本以发射验证为主，完整牵引和跳脱动作断言以 Rehooked 为主；不代表玩家完整整合包或独立服双玩家协作行为已验收。
