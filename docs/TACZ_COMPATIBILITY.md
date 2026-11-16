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
