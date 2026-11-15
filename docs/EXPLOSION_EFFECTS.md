# Forge 1.20.1 爆炸视觉接口

ExplosionEffects.highExplosive() 返回基石注册的高爆粒子预设。它只负责视觉，伤害、地形交互与声音由调用者处理。Forge 版本使用原版粒子引擎播放与 master 共用的闪光、火花、碎片和烟雾贴图，不依赖 Photon 2 或 LDLib2，也不读取 Photon 2 的 .fx 预设。

在服务端发送一次预设：

```java
serverLevel.sendParticles(ExplosionEffects.highExplosive(), x, y, z, 1, 0, 0, 0, 0);
```

粒子预设在注册完成后可用。64 格外忽略，24 格内且粒子品质为“全部”时使用较完整的效果，其余情况减少粒子数量。“最少”品质使用原版爆炸粒子。每个客户端世界每 tick 最多触发两次预设，避免密集爆炸产生过多粒子。

粒子会随所在世界卸载；断开连接时清空预设的世界引用与限流计数。
