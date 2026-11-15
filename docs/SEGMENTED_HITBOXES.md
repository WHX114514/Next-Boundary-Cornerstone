# 失重模式分段物理碰撞与受击盒：AABB / OBB

失重玩家的受击模型使用 16 个小盒，默认使用 AABB，也可由服务器切换为 OBB：

| 部位 | 数量 | 分段方式 |
| --- | ---: | --- |
| 头部 | 1 | 8 × 8 × 8 像素小方块 |
| 躯干 | 3 | 上、中、下，每段 8 × 4 × 4 像素 |
| 左右手臂 | 各 3 | 每段 4 × 4 × 4 像素，细手臂皮肤宽度为 3 像素 |
| 左右腿 | 各 3 | 每段 4 × 4 × 4 像素 |

尺寸沿用玩家模型的 1/16 像素单位、0.9375 渲染缩放及实体 scale 属性。
骨架转动时，计算小块中心与轴对齐半尺寸，生成各小块的 AABB 包络。
默认模式的命中计算使用 AABB 射线检测，没有独立部件实体或额外的实体 tick。
斜向旋转时包络会略大于实际模型，因此这是一种分段近似。

## 物理碰撞与转身

失重运动碰撞使用 10 个分段 AABB：头部 1、躯干 3、双腿各 3。左右手臂共 6 个盒不参与移动、转身让位、贴附接触、可碰撞实体及世界边界的运动阻挡；受击仍保留完整 16 个盒。腿部中段正常参与运动碰撞。
`getBoundingBox()` 返回复合盒：其外包络只供空间索引使用，原版体素形状查询和服务端移动包校验使用小盒子的实际并集。
方块碰撞迭代器直接用各小盒检查复杂方块形状，不为每次探测构建整个人体的体素网格。
其他确实需要 `VoxelShape` 的调用使用缓存的平衡并集合并，不在每加一个小盒时反复执行体素简化。
移动按原版轴顺序裁剪所有小盒，保留沿墙方向的速度；空隙不会再被原来的 1.2 格大方块挡住。
表面辅助按实际小盒查找接触，不以外包络中的空气作为支撑。
分段物理启用时，姿态规划负责按实际运动小盒执行最小让位及浅层穿入恢复，客户端停用原版按 `1.2` 格宽度取角点的脱困推力，避免它与贴壁吸附反复相互拉扯。

- AABB 受击模式：完整受击对象保留 16 块，运动对象选择其中 10 块并复用同一批 AABB 几何引用，不重新计算保留部分。
- OBB 受击模式：运动仍是 10 块 AABB，另外保留完整 16 块 OBB 用于精确命中。

转身、改变姿态或摆动双腿前，检查运动小盒的中间姿态扫掠范围，并在附近搜索最小让位位移。手臂动作不增加扫掠步数，也不触发让位。
扫掠余量同时计入缩放后的身体尺寸和未随实体缩放的渲染支点偏移；漂浮标记切换、游泳动画从零开始及归零时，包含位移跳变两侧的轨迹。
让位范围限制为半格乘实体缩放，搜索节点数有上限；让位路径也须可通行，不能直接挪到墙另一侧。
无法完成时保留之前安全的身体、腿部姿态，把期望头部方向转换到原来的颈部坐标。六个手臂盒继续跟随手臂动画并保留受击，不随身体受阻而冻结。
头部同样受墙面限制，空间不足时只转到安全角度。身体受阻后自动使用普通自由视角的输入限位：第一人称相对身体左右各 110°、上下各 90°，自动相对桶滚限制 60°；头部模型左右/上下各 90°、桶滚各 60°，墙面还可能进一步限制实际头部动作。
沿用普通 C 视角已有的第三人称区别：水平视角可环绕并按 360° 循环，俯仰仍受限。玩家显式开启的“固定头部自由视角”保留原有不受这套限位约束的相机四元数旋转。
自动受限状态与手动 C/Shift 状态分别保存，不开启固定头部锁定，也不改写玩家的切换开关。鼠标输入本身按限位累积，反向输入立即生效，不积压超限转动量。准星、第一人称手部和 `FreeLookAPI` 会识别自动自由视角。
自动受限时，第一人称中心圆圈左侧增加 `Z ±角度°` 的相对滚转读数，与原有偏航、俯仰指示并列。手动 C 和固定头部自由视角不显示此新增读数。
受限期间，在原版 Action Bar（原“自由视角”提示位置）持续显示“空间受限无法转身”，随额外生命值行自动上移；退出受限立即消失，不保留过期提示。该提示支持第一、第三人称，手动模式沿用原有提示。
每 tick 重试，空间恢复后退出自动受限状态，身体继续跟随视角；真实传送、离开失重或断开连接也会清除自动状态。
分段碰撞玩家保留已通过碰撞裁剪的微小移动，避免原版丢弃位移平方小于 `1e-7` 的让位，把可完成的转身误判为受限；普通实体的原版阈值不变。
服务器复核收到的姿态，必要时回传受限姿态和位置纠正；不会禁用原版服务端移动检查来掩盖不同步。
正常让位由客户端预测、服务器复核，不作为传送处理，也不会把旧的四肢动画回灌给本地玩家。

## 选择模式（以服务器为准）

NeoForge 会在实例的 `config/next-boundary-hitbox-server.toml` 中生成：

```toml
[Zero_Gravity_Hitboxes]
Mode = "AABB"
```

将 `Mode` 改为 `"OBB"` 启用旋转盒；`"AABB"` 保留当前分段轴对齐模式。
该文件注册为 **SERVER** 配置，客户端加入服务器时由 NeoForge 同步服务端值，不读取客户端自己的同名文件来决定联机命中模式。
单人游戏使用集成服务器配置。可在 `<世界目录>/serverconfig/next-boundary-hitbox-server.toml` 放置世界专用配置，覆盖实例的 `config/` 默认值。
修改后重启服务器并让玩家重新连接，保证整场会话使用相同设置。

OBB 模式复用完全相同的 16 段骨架、尺寸、姿态和同步数据，保存每段的实际朝向。
候选查询仍使用便宜的 AABB 包络，最终射线、盒内判断、交互距离及 TACZ 头部判断转到 OBB 局部坐标计算。
投射物的命中余量扩展 OBB 的局部半尺寸，不把旋转盒退化成大 AABB。
OBB 的包络角落不算命中；相比 AABB 需要更多运算。尚未对两种受击模式做性能对比，不承诺具体帧率差异。
几何和常用余量结果沿用缓存，切换模式时缓存失效。
OBB 的配置、数学、构建及调试绘制均位于 `body/OOBB/`，文件夹名按项目约定保留为 `OOBB`。

客户端每 tick 使用独立的 PlayerModel 采样原版动画，包含游泳、挥手和持物姿态；
即使第一人称下不绘制身体也会采样。身体使用 trunk 平滑后的旋转，头部共用渲染的头身隔离公式。
在原有旋转包中同步骨架，服务端据此计算受击盒；远端玩家渲染使用同步的骨架并插值。
失重包 224 字节（不含网络包头），每秒 20 次；退出包不携带骨架。
骨架、世界坐标盒及常用射线膨胀结果会缓存，骨架不保存到玩家 NBT。

原版近战选取和投射物通过 ProjectileUtil 的两个射线入口使用分段检测。
候选查询覆盖完整身体，最终命中只由各小块决定。
服务端交互距离按最近小块计算。
TACZ 的独立弹道入口也接入分段检测，爆头按头部小块判断；
失重玩家在此入口使用当前同步骨架，不套用 TACZ 原有直立大盒子的延迟补偿。
TACZ 编译依赖为官方 NeoForge 1.21.1 版 `1.2.0-hotfix`（Modrinth 项目 `SzzJttH8`，版本 `tJiZL0He`），保持可选依赖。失重时，子弹在构造结束时将位置和伤害衰减起点同时设为完整头部眼睛坐标；生成事件只修正弹速方向，不再旋转位置差。第一人称曳光偏移交由官方版的完整相机四元数处理，移除了旧的额外翻滚和三倍缩放。
已通过构建、现有测试及安装官方版的隔离客户端检查；新增服务端测试覆盖旋转、漂浮、独立转头、大世界坐标、生成事件和伤害起点。客户端测试确认翻滚时枪口偏移读取保持原值；尚未逐枪包做人工射击观感验收。
其他绕开这两个入口、自行检测大盒子的模组需要单独适配。

普通重力、旁观者的穿墙状态、乘坐实体时沿用原版物理路径；离开失重后清除受限姿态并恢复尺寸。
普通重力或恢复原版控制的观察者模式使用原版命中盒。
退出失重时清除骨架缓存；服务端核对发包玩家 UUID、实际失重状态，并拒绝非法骨架数值。
网络协议版本为 `compound-physics-8`，联机双方需要同时更新，以使用一致的运动模型、受限头部规则、眼睛射线起点和窒息检测。

## 眼睛视点

失重视点使用 `body/BodyEyes` 从已接受的头部骨骼计算，取消旧正方体 `size * 0.6` 的低视点。锚点位于头部内部的眼睛高度（模型局部 Y 为 `-3.5 / 16`，水平居中），保留相机近裁剪面贴墙所需的余量。站立、漂浮、身体翻滚和独立转头均经过与头部模型相同的变换；不改变身体旋转中心或碰撞箱。

第一人称相机、`getEyePosition()`、插值版 `getEyePosition(partialTick)`、`getEyeY()` 和当前 `getEyeHeight()` 共用眼睛锚点。方块选取等采用这些接口的交互射线因此跟随眼睛，客户端渲染使用骨架插值，服务器使用当前已接受的姿态。第三人称先设置眼睛锚点，再执行原版拉远和避墙检测。普通重力沿用原版视点，AABB / OBB 模式行为一致。

眼睛偏移按姿态与渲染插值缓存，只计算头部变换；不构建额外碰撞并集，不增加网络包字段。倒置时当前眼高是相对实体原点的世界 Y 偏移，可能为负；需要完整三维起点的接入方应使用 `getEyePosition()`。

方块内部遮罩和窒息检测也不能继续使用旧实体宽度：旧探针从身体中心横向伸出约 0.48 格，超过实际头部约 0.234 格的半宽，脸、背或侧面靠墙时会误报。`EyeBlockChecks` 改用实际眼睛附近半径 `0.005 * scale` 的小探针；客户端遮罩取当前帧相机位置，服务器窒息取当前眼睛位置。眼睛确实埋进遮挡方块时仍显示遮罩，进入会窒息的实际碰撞形状仍能触发窒息；普通重力保持原版检测。

游戏内按 **F3+B** 查看：AABB 模式显示黄色头部、青色躯干和双腿，六个只受击的手臂盒显示紫色。
OBB 模式额外用**橙色**显示物理 AABB，黄色、青色仍显示受击 OBB。
不再绘制代表实体包络的大白方块，避免把空间索引误认为实际碰撞范围。
显示的是当前 tick 的实际命中盒；渲染中的人物使用帧插值，快速运动时可能看到轻微时间差。

## 部位标签与其他设备接入

`body/BodyBoxTag` 定义 16 个固定标签，顺序与原有完整骨架一致。每个标签有 `id()`、`part()`、`segment()`、`jointIndex()` 和 `mask()`。

- 部位：`HEAD`、`TORSO`、`LEFT_ARM`、`RIGHT_ARM`、`LEFT_LEG`、`RIGHT_LEG`。
- 分段：头为 `WHOLE`，其余部位为 `UPPER` / `MIDDLE` / `LOWER`；`LOWER` 是远离躯干的一端。
- 完整标签如 `LEFT_ARM_MIDDLE`、`RIGHT_LEG_LOWER`，字符串 ID 为 `left_arm_middle`、`right_leg_lower`。
- `ALL_MASK` 选择全部 16 块；`ARMS_MASK` 选择六块手臂；`MOVEMENT_MASK` 选择默认运动的 10 块；`mask(Part)` 选择一个完整部位。

其他设备可对失重玩家使用世界坐标 AABB 做部位查询：

```java
import cn.rbq108.nextboundarycornerstone.body.BodyBoxTag;
import cn.rbq108.nextboundarycornerstone.body.collision.SegmentedPhysics;

int hands = BodyBoxTag.ARMS_MASK;
boolean touchesArms = SegmentedPhysics.intersectsParts(player, deviceWorldBox, hands);

int feet = BodyBoxTag.LEFT_LEG_LOWER.mask() | BodyBoxTag.RIGHT_LEG_LOWER.mask();
boolean touchesFeet = SegmentedPhysics.intersectsParts(player, deviceWorldBox, feet);
```

接口使用完整缓存的 AABB 模型，因此即使手臂不参与默认运动，设备仍可识别它们；普通重力玩家和空选择返回 false。它是检测接口，设备自己的推挤、抓取等响应由设备实现，不会自动改变全局运动规则。

`player.getData(main.PLAYER_ROTATION).aabbModel(player)` 提供完整标签模型；`pieces()` 与 `tags()` 按索引对应，`pieces(mask)` 返回选中的几何引用。运动对象只含 `BodyBoxTag.MOVEMENT`，移动、膨胀、扫掠变换保留标签。受击 AABB / OBB 对象均提供 `tags()`；OBB 的 `pieces()` 是其 AABB 包络，精确朝向仍由 `orientedPieces()` 提供。

标签是固定共享元数据，筛选使用整数位掩码；不逐帧生成字符串，不增加同步包字段。第三方绕过统一碰撞入口的物理系统需自行调用接口适配。

## 验证

- `gradlew.bat hitboxTest`：16 块分段、接缝、三轴转动、游泳、独立手臂、细手臂、实体缩放、最近命中、空隙漏射、射线起点在盒内、缓存及非法四元数。
- `gradlew.bat obbTest`：OBB 包络角落漏射、局部余量、最近命中、遮挡后的爆头判断、三轴旋转、游泳、缩放及大世界坐标精度。
- `gradlew.bat physicsTest`：小盒间空隙、六轴高速移动、薄墙、沿墙滑动、复合形状变换、转身让位、夹墙拒绝让位、中间旋转路径、只转头及空间恢复；另含微小缩放、漂浮姿态双向切换的逐段轨迹覆盖和漂浮时的头部世界方向。
- `gradlew.bat freeLookTest`：普通自由视角的输入累积限位、反向输入、第三人称水平循环、头部限角和竖直俯仰的四元数提取。
- `gradlew.bat bodyEyesTest`：独立头部 OBB 参照下的眼睛位置、翻滚、漂浮、缩放、独立转头和骨架插值。
- `gradlew.bat check`：包含 AABB、OBB、物理碰撞和原有表面辅助测试。
- `gradlew.bat runGameTestServer --no-parallel`：按测试服务端实际文件选择模式；验证两条真实射线入口、包络角落的模式差异、完整头部、候选过滤、缓存、退出失重和同步包，以及真实移动包穿过包络空隙、一格宽通道、头部顶棚、转身让位和夹墙只转头。分别使用 AABB / OBB 文件运行以覆盖两种模式。
- `gradlew.bat runSurfaceClient`：隐藏窗口、模拟按键的客户端 QA，检查第一人称采样、服务端收到骨架、六面吸附和既有退出/恢复行为。
- `gradlew.bat runGameTestServer -PtaczRuntime=true --no-parallel`：在开发运行时加载官方 TaCZ，执行真实子弹构造及生成事件的头部起点回归；未指定此参数且未安装 TaCZ 时跳过该项。
- `gradlew.bat runHitboxVisualClient -PtaczRuntime=true -PhitboxVisualScenario=tacz -PhitboxVisualDir=.local-artifacts/tacz-origin --no-parallel`：隐藏客户端验证第一人称翻滚时枪口偏移不会重复旋转或放大。`taczRuntime` 只用于选择开发运行时依赖，不将 TaCZ 打入本模组 JAR。
- `gradlew.bat runHitboxVisualClient -PhitboxVisualScenario=physics --no-parallel`：后台真实客户端检查墙边转身让位、窄通道只转头、普通自由视角真实鼠标输入限位、跨多个第一人称渲染帧的稳定性、手动固定头部模式及移除障碍后自动恢复，并保存帧缓冲截图。与普通截图任务相同，可通过实际世界 SERVER 配置分别运行 AABB / OBB；不发送桌面键鼠输入。
- `gradlew.bat runHitboxVisualClient -PhitboxVisualScenario=collision-hud --no-parallel`：复用真实墙边/窄通道物理场景，保存受限正、负滚转 HUD、移除障碍恢复后和手动 C 自由视角的截图，用于核对滚转读数和底部提示只在自动受限状态显示。
- `gradlew.bat runHitboxVisualClient -PhitboxVisualScenario=posture --no-parallel`：真实客户端穿脱背包，检查站立→漂浮→站立时客户端、服务器和物理姿态一致，并截图保存。
- `gradlew.bat runHitboxVisualClient -PhitboxVisualScenario=eyes --no-parallel`：隐藏客户端逐帧核对第一人称实际相机与插值眼睛起点，覆盖站立、翻滚、独立转头、漂浮，并验证第三人称四格拉远和后方石墙遮挡；保存实际帧缓冲截图与 `eye-frames.json`。
- `gradlew.bat runHitboxVisualClient -PhitboxVisualScenario=eye-wall --no-parallel`：隐藏客户端验证脸、背、侧面靠墙、贴墙转头和翻滚时实际眼睛仍在墙外且无遮罩/窒息误判；另将眼睛真实放入石头，验证两种检测仍生效，保存截图和 `eye-wall-frames.json`。
- `gradlew.bat physicsProfile -PphysicsProfileOutput=.local-artifacts/physics-performance/profile --no-parallel`：手动微基准，记录旋转人体并集和窄处转身的耗时、每调用分配量及 JFR；不加入普通 `check`。
- `gradlew.bat runHitboxVisualClient -PhitboxVisualScenario=performance --no-parallel`：隐藏客户端按相同输入运行高空静止、转视角和叶子墙附近转身，固定 120 FPS 上限、关闭垂直同步，每场景预热 2 秒并采样约 5 秒，输出 `performance.json` 和 `performance.jfr`。测试结果是隔离环境的数据，不代表任意整合包帧率。
- `gradlew.bat runHitboxVisualClient --no-parallel`：隐藏窗口、中文、实际第三人称帧缓冲截图，不发送桌面键鼠输入。可用 `-PhitboxVisualScenario=rolled` 或 `side` 定点取景，`-PhitboxVisualDir=<产物目录>` 隔离世界，`-PhitboxExpectedMode=OBB` 仅声明预期断言，不强制选择模式。在产物目录放置 `server-config.toml`，夹具会将其复制成真实的世界 SERVER 配置，用于验证它覆盖相反的实例默认值。

核心尺寸及分段数量在 `SegmentedHitboxGeometry`，共同的姿态数据结构在 `SegmentedHitboxPose`。
物理并集、运动裁剪和转身规划位于 `body/collision/`。本次没有合并或改变 OBB 部件数量。
