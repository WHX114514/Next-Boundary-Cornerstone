# 高爆特效 API

基石 0.0.6-test 提供 `cn.rbq108.nextboundarycornerstone.api.ExplosionEffects.highExplosive()`。
请在粒子注册完成后的游戏逻辑里调用。返回值可用于原版爆炸包和粒子包，特效会根据每位客户端的距离、粒子设置和同屏负载降级。

## 与爆炸一起播放

在服务端命中时，把同一个粒子传给原版爆炸的小型和大型特效参数。调用方选择爆炸威力、声音和方块交互模式。

```java
var effect = ExplosionEffects.highExplosive();
level.explode(projectile, null, null, x, y, z, 2.0F, false,
        Level.ExplosionInteraction.NONE, effect, effect, SoundEvents.GENERIC_EXPLODE);
```

## 单独播放

```java
level.sendParticles(ExplosionEffects.highExplosive(), x, y, z, 1, 0, 0, 0, 0);
```

也可用 `/particle next_boundary_cornerstone:high_explosive ~ ~1 ~ 0 0 0 0 1 force` 预览。
这一用法只播放画面，使用原版粒子包的送达规则。

## 编辑与客户端

基石客户端可单独安装 Photon Editor 2.2.8 和 LDLib2 2.2.42。开发环境将这两个模组放进 `run/mods`，用
`bash gradlew runClient` 启动，进入创造模式单人世界后运行 `/photon_editor`。
源工程位于 [tools/photon](../tools/photon)，导出资源位于
[assets/next_boundary_cornerstone/fx](../src/main/resources/assets/next_boundary_cornerstone/fx)。
两档工程使用相同的基石贴图。导出更新后需重载客户端资源。
同一客户端最多同时播放四个 Photon 爆炸。已有近景爆炸周围三格内的新爆点优先使用简化版，
以限制半透明烟雾的重叠。

联机时 Photon 由各客户端自行安装。基石在客户端将 Photon 2.2.8 的命令通道设为可选，
因此带 Photon 的客户端可以连接只运行基石的服务端。未安装 Photon 的客户端会使用轻量的原版爆点。
附属模组依赖基石并调用上述接口即可，Photon 和 LDLib2 只参与基石的可选客户端渲染。

## 视觉层次

近景版 `high_explosive` 按以下顺序组织十层，烟雾先绘制：

1. `06 Fire-lit rising smoke`：火光照亮的烟团逐渐转为暖灰，上升、膨胀并缓慢消散。
2. `02 Billowing fireball`：翻滚的火球由白热、金黄变为橙红，并向外扩散。
3. `02b Copper heat bloom`：爆心保留短暂的铜红热晕。
4. `01 White-hot flash`：短促的白热闪光突出命中瞬间。
5. `01b Star flare`：随机旋转的星芒增加闪光层次。
6. `03 Ground shock ring`：水平冲击环迅速扩张至约十格；目前位于爆点高度，尚未做地面射线定位。
7. `03b Shock shell`：面向镜头的淡蓝冲击壳补充空中轮廓。
8. `04 Gold streaks`：金色拉丝向外飞散，受重力影响形成下垂弧线。
9. `05 Trailing ember arcs`：带辉光拖尾的火星飞散、落地并弹跳，尾端逐渐变细变淡。
10. `07 Rising cinders`：细小余烬随热流缓慢升起。

简化版 `high_explosive_lite` 保留烟雾、火球、热晕、闪光、星芒、冲击环和金色拉丝七层，
去掉冲击壳、拖尾火星和上升余烬。烟雾降至 4 个且关闭噪声，火球降至 5 个，拉丝降至 16 个；
部分寿命和尺寸缩小，冲击环扩张至约七格。客户端距离判定、同屏预算与降级逻辑保持原样。

辉光依赖 Photon 的 Bloom（配置 `enable_bloom`），高亮层使用 `MULTIPLICATIVE` HDR。
关闭 Bloom 后仍可播放，只是没有辉光。所用贴图为 `blast_glow`、`blast_spark`、`blast_smoke`、
`blast_fire`、`blast_flare`、`blast_ring`、`blast_ember` 和 `blast_trail`。

弹体三角破片及 `blast_shard.png` 已从基石特效移除，后续由附属模组按弹型提供。
可选破片资源 ID 的接口接入属于后续工作，本次不改变爆炸包和 API。

### 重新生成

生成器位于 `tools/photon/gen`，仅依赖 JDK（Java 21 或更新版本）。在该目录执行：

```powershell
javac -d . Nbt.java Gen.java
java -cp . Gen ..\..\.. orig\high_explosive.fx
```

输出包括两档 `.fx`（gzip NBT）、对应 `.fxproj`（未压缩 NBT）和六张程序贴图；
近景版 10 个发射器，简化版 7 个。固定随机种子保证同一输入可重复生成。
生成器会检查原始模板名称和被覆盖的字段，拒绝将导出目录或已生成的特效当作模板。
`orig/` 原始备份和编译产物均不入库；保留原始备份，不要用生成结果覆盖它。

新检出仓库没有 `orig/` 时，可从重做前的提交恢复模板。在 `tools/photon/gen` 执行：

```powershell
$templateRoot = (Resolve-Path ..\..\..).Path
New-Item -ItemType Directory -Force orig | Out-Null
$templateArchive = Join-Path (Get-Location) 'orig\templates.zip'
git -C $templateRoot archive --format=zip --output=$templateArchive 39ea55863813b0329268a367ef668098e47d2bbd src/main/resources/assets/next_boundary_cornerstone/fx tools/photon/high_explosive.fxproj tools/photon/high_explosive_lite.fxproj
Expand-Archive -LiteralPath $templateArchive -DestinationPath orig\snapshot -Force
Copy-Item orig\snapshot\src\main\resources\assets\next_boundary_cornerstone\fx\*.fx orig\
Copy-Item orig\snapshot\tools\photon\*.fxproj orig\
```

在编辑器中手动修改 `.fxproj` 后，需要重新导出 `.fx` 并重载客户端资源。
再次运行生成器会覆盖工程、导出资源和程序贴图中的手动修改。
