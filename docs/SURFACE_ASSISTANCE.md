# 失重表面辅助

失重玩家的碰撞箱接触静态方块后，会获得有限的吸附和贴壁制动。推进仍使用身体朝向和现有六自由度输入：持续按身体的上升键（默认空格），或转向后向表面外推进，可以挣脱；冲刺会减弱辅助。自由观察、手动翻滚及后坐力沿用现有逻辑。

吸附不要求装备磁靴，也不限制方块材质。首次接触前不施加吸力或接近阻尼。碰撞箱任意侧均可捕获，不自动旋转身体或镜头；模型姿态不代表碰撞箱发生旋转。

## 配置

`next-boundary-surface-server.toml` 是 NeoForge SERVER 配置，连接时由服务器同步给客户端。默认位于游戏/服务器目录的 `config` 文件夹；世界的 `serverconfig` 可以提供该世界的覆盖配置。原有 `next-boundary.toml` 中的推进参数仍决定基础速度和加速度。

| `Surface_Assistance` 配置 | 默认值 | 含义 |
| --- | --- | --- |
| `Enabled` | `true` | 启用失重表面辅助 |
| `StrengthRatio` | `0.65` | 吸附加速度占当前基础推进加速度上限的比例 |
| `NormalDamping` | `0.5` | 每 tick 的离壁阻尼系数 |
| `StopTicks` | `6` | 标准贴壁速度下的近似松键停车时间 |
| `SprintMultiplier` | `0.15` | 冲刺时吸力和切向制动倍率 |

离壁阻力受本 tick 实际向外推进增量的 90% 限幅，包含斜向推进和临近期望速度时的小加速度。没有推进能力时，辅助也不凭空提供动力。仅按上升等法向输入时，切向漂移仍会主动停车。

接触容差为 0.0001 格；已接触表面可在 0.35 格范围内保持。超过范围后有 5 tick 重新捕获冷却。支撑缺失时立即暂停辅助，连续两 tick 缺失则清除记忆；同平面跨方块接缝优先保持法线。传送/服务器位置纠正、断线、退出失重、死亡/重生、换维度、骑乘及航空学把手抓取均清除吸附状态。

第一版处理静态方块碰撞形状。实体和世界边界参与正常惯性碰撞同步，但不会触发吸附。移动飞船局部坐标、自动绕外墙角、自动站正和专用行走动画不包含在此版本中。

## 回归与运行时验收

在项目根目录使用 Java 21：

```sh
bash gradlew surfaceTest build
bash gradlew runGameTestServer
```

`surfaceTest` 是独立的数值/几何回归，覆盖六向接触、向内碰撞清除、接缝、冷却、停车、斜推挣脱、弱冲刺辅助和原有推进/手动制动。`check` 和 `build` 会执行它。GameTest 专服使用隔离的 `build/surface-game-test`，验证实际 SERVER 配置加载以及真实半砖、楼梯、流体和接缝碰撞形状。

真实客户端入口仅包含在测试 source set 中，默认执行全自动后台验收：

```sh
bash gradlew runSurfaceClient
```

该命令只使用 `build/surface-client-qa`：准备任务禁用该隔离目录中的 FML 早期窗口，测试专用 Mixin 在正式窗口创建前设置不聚焦、不显示提示。测试不会调用系统键鼠或桌面控制，也不主动激活窗口。它自动新建验收世界、装备背包，布置 UP、DOWN、EAST、WEST、SOUTH、NORTH 六个表面的初始位置和身体姿态。

默认输入由独立的 `cornerstone_surface_qa` 测试模组在 `KeyPollingHandler.isPressed` 入口注入，绕过窗口焦点检测；六面的捕获、松键停车与持续上升挣脱仍经过正常生产 tick 和实际方块碰撞。只有初始位置、身体姿态和切向漂移由 fixture 布置，挣脱阶段不会直接改坐标或最终速度。随后还检查服务器传送包清理吸附、退出及恢复失重、后坐力只消费一次。

成功后自动写入 `build/surface-client-qa/surface-result.json` 并退出。后台结果必须同时具有 `passed=true`、`inputMode="simulated"`、六项 `capture-and-parking-*`、六项 `simulated-input-escape-*` 和四项生命周期检查，日志为 `SURFACE_CLIENT_QA_PASS inputMode=simulated checks=16`。其中 `physicalInputVerified=false` 明确表示**没有验证真实键盘输入或主观手感**。

### 前台验收交接提示词

下面的任务需要操作游戏窗口，应交给另一个已获得用户前台操作授权的 AI。后台模式通过后仍需此类验收来检查物理键位、重绑、自由观察和主观手感。前台运行通过 `-PsurfacePhysicalInput=true` 关闭模拟输入和后台窗口提示。

```text
请验收基石模组的实际键位和失重贴壁手感。本任务需要前台游戏窗口操作，请确认当前会话已有用户对前台操作的授权。

项目：/Users/xiaoyu/IdeaProjects/next_boundary_cornerstone
请保留现有工作树修改，不提交、不推送，不改生产代码。

1. 阅读 docs/SURFACE_ASSISTANCE.md 和 src/test/client/java/cn/rbq108/nextboundarycornerstone/qa/SurfaceClientQa.java。
2. 确认没有同一验收客户端正在运行，再执行 bash gradlew runSurfaceClient -PsurfacePhysicalInput=true。此模式不注入模拟键位，需要真实键盘输入。
3. fixture 自动创建隔离世界并按 UP、DOWN、EAST、WEST、SOUTH、NORTH 顺序布置。每次在 build/surface-client-qa/logs/latest.log 看到 SURFACE_QA_READY normal=... inputMode=physical 后，激活游戏窗口，长按空格约一秒并松开，等待对应 SURFACE_QA_ESCAPED。
4. 逐面完成真实按键。不要用直接改坐标、输入变量或绕过物理键盘轮询代替此检查；fixture 自身的坐标和姿态设置仅用于初始布置。
5. 核对 build/surface-client-qa/surface-result.json 的 passed=true、inputMode=physical、physicalInputVerified=true、六项 capture-and-parking-* 和六项 real-space-key-escape-*，以及日志 SURFACE_CLIENT_QA_PASS inputMode=physical checks=12。
6. 另在普通 runClient 中实际检查 WASD、空格、自由观察、翻滚、手动制动、冲刺弱辅助、方向键重绑和修饰键组合。记录撞壁、松键停车与挣脱的实际手感；这部分不能由模拟输入结果代替。
7. 使用独立服务器和两个客户端检查位置同步、服务端配置开关与参数同步、重生及换维度；单人集成服务器的测试不能证明双客户端验证完成。
8. 报告实际完成的检查、结果文件路径和缺失项。仅到达 READY 只证明当前面捕获与停车，不能宣称挣脱通过。遇到锁屏、失焦或超时须明确记录未完成。
```

生产 JAR 不包含测试入口、测试结构或调试接口。独立服务器双客户端的移动同步、主观手感及其他物理模组兼容性仍需要相应环境下的实际验收。
