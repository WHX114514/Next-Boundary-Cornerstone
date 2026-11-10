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
