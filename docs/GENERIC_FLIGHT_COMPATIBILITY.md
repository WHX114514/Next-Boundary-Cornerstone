# Forge 1.20.1 失重通用兼容

本次只修改 `1.20-forge`。生产代码不引用 Rehooked、Hookshot、Advanced Hook Launchers、Spartan Weaponry 或 Immersive Engineering；可选依赖与反射夹具只用于 QA，不进入发布包。

## 问题与修复边界

- 原来鼠标累计角度兼任手部摆动与玩家逻辑朝向。翻滚后，它与四元数视线不同，读取 `getLookAngle`、`getViewVector(1)`、pitch/yaw 的模组会得到不同方向。现在公开世界角度和视线统一来自 `FlightAim`；手部屏幕轴累计独立放在 `HandSway`，只替换原版手部渲染及 bob 平滑的读取。
- 沿用通用投射物入口及 TaCZ 原有适配，不增加第二套发射方向计算。显式使用 `xRotO/yRotO` 的发射保留其上一帧世界方向，避免再次旋转。自定义网络发射键在发送前先同步有变化的姿态，复用既有数据包；没有增加网络字段。
- 原来按调用栈包名过滤速度写入，会遗漏 `push`、`lerpMotion` 等委托路径。现在在 Vec3 setter 尾部捕获最终世界速度，继续使用现有推进缓存和缩放系数；只在原版特定维护 setter 与本模组回写时暂时屏蔽捕获，不屏蔽整个 tick/travel 或其中的第三方回调。
- 保留公开按键、键盘输入及移动输入字段，只抑制原版自身的重复推进、跳跃、蹲伏与疾跑消费。取消强制设置生存飞行、飞行速度为零的行为，保留真正的创造/旁观及其他来源权限；旧版由本模组授予的权限仍会撤回。
- 服务端仅让已启用失重的玩家豁免原版浮空检查，保留其他移动与碰撞检查。原有失重 FOV 单独保留，不依赖伪造飞行状态。

不修改第三方投射物的 tick、重力、阻力、散布、伤害、回收、寻路或惯性策略。绕开这些公共方向/速度入口的独立物理系统，以及主动禁止失重的模组，仍可能需要专门适配；本修复不声称覆盖所有模组。

## 可复现 QA

Java 17、Forge 47.4.18、macOS arm64。客户端窗口逐 tick 断言不可见且未聚焦，不操作桌面键鼠。真实物品使用、网络发射键、使用/松开与牵引 API 均在后台执行。

| 测试样本 | 版本 | 覆盖 |
| --- | --- | --- |
| Rehooked | 1.8.3，Curios 5.14.1 | 铁钩发射；横向/向上/向下牵引；跳脱惯性；背包和无背包强制失重 |
| Hookshot - From Zelda | 1.20.1-1.1.4 | 真实发射方向、移动射手与重力切换 |
| Advanced Hook Launchers | 10.1.3.0，ForgeEndertech 11.1.11.2 | 原生发射键；实际可用发射器；原生散布与重力切换 |
| Spartan Weaponry | 3.2.1 | 飞刀、标枪、回旋镖；持续下坠、返回/回收 |
| Immersive Engineering | 10.2.0-183 | 充能电磁炮与装弹左轮的真实使用/松开、入场后弹道 |

```sh
# 六种失重姿态，含侧翻、倒立、混合旋转及正负极点附近
bash gradlew runForgeClientQa -PcompatQa=true -PcompatQaMods=rehooked,hookshot,advanced,spartan,immersive
# 移动射手、普通重力、退出失重；另检查持续下坠和回旋返回
bash gradlew runForgeClientQa -PcompatQa=true -PtransitionQa=true -PcompatQaMods=rehooked,hookshot,advanced,spartan,immersive
# 单独只装基石及必需依赖
bash gradlew runForgeClientQa -PcompatQa=true -PqaHitboxMode=AABB
bash gradlew runForgeClientQa -PcompatQa=true -PqaHitboxMode=OBB
bash gradlew runForgeClientQa -PlifecycleQa=true
bash gradlew runForgeClientQa
bash gradlew build jarJar runGameTestServer
```

客户端可用 `-PqaDirectory=...` 隔离世界和报告。不同 TaCZ 版本的 Gradle QA 应串行运行，以免共用编译目录在 JVM 运行期间被重写。

真实专服测试先运行 `bash gradlew runServer -PdedicatedQa=true`，就绪后再运行 `bash gradlew runForgeClientQa -PdedicatedQa=true -PqaDirectory=build/general-compat-qa/dedicated-client`。该 opt-in 服务端仅监听 `127.0.0.1:25579`，`allow-flight=false`，使用专用 build 目录，生成 QA EULA/配置并自动退出。它先检查 260 tick 生存失重悬停，再移除背包，故意持续悬停以验证普通重力浮空踢出仍有效。

## 本次结果

- 未修改基线 `e669ea8` 的同一组公共入口测试记录 23 条失败；修复后的完整六姿态兼容矩阵 272 条断言通过，移动与重力切换矩阵 181 条通过。
- 原版小于 `0.003` 的速度分量归零行为保留，牵引位移断言按原版规则计算，未为通过测试放宽实际牵引误差。
- 真实 TCP 专服：260 tick 生存失重悬停通过，`mayfly=false/flying=false/allow-flight=false`；退出失重后记录 65 tick 原版浮空状态并被正常踢出。
- TaCZ 1.2.0-hotfix / AABB、1.1.7-hotfix2 / OBB：四组换弹、四次 ADS/腰射、100 组镜头样本及连续扫描均通过；两个版本服务端各 22 项 GameTest 通过。最后一项蹲姿修正后另行复跑 Rehooked 103 条断言与 TaCZ 1.2 实射，均通过。
- 清理兼容模组环境后，只装基石、Forge、MixinExtras 和测试驱动：AABB/OBB 各 31 条公共入口及输入断言通过；真实死亡重生、两轮主世界→末地→主世界、原生设置/登录/暂停/第三人称渲染等独立验收通过。运行时模组列表已核对，不含可选兼容模组。
- 与未修改基线比较 175 tick 操作输入序列：AABB/OBB 位移分量最大差 `5.96e-8` 格，推进缓存最大差 `2.98e-8`，FOV 完全一致；包含推进、惯性、手动刹车及翻滚输入阶段。
- 最终 `build / jarJar`、七组数学测试共 55,496 条断言、无 TaCZ 的 22 项 GameTest 通过，其中 TaCZ 专项按缺失跳过，通用投射物实际验证 70 个发射场景和药水/弓/弩物品入口。
- 发布包 `0.0.6-test-26.11.17-hotfix2`：Java class 61，44 项 Mixin、38 类 refmap 映射；`all.jar` 内置 MixinExtras，不含 QA、GameTest 或任何兼容样本模组实现。文件名中的 `e669ea80` 为本次工作起点，修改尚未提交、未推送。

结果与发布包 SHA-256 保存在 `build/general-compat-qa/results/`，最终构建日志为 `build/general-compat-qa/final-build.log`。已移除临时下载、测试世界、反汇编探针及基线副本，后台 QA 游戏进程全部退出；保留报告、必要测试源码与正常 Gradle 依赖缓存。

辅助钩爪样本的自动验证覆盖真实发射，牵引/跳脱的完整动作断言以 Rehooked 为主；没有使用玩家的完整整合包，也未验证独立服双玩家协作钩爪行为。
