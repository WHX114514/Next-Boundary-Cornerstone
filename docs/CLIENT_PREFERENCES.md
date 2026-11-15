# Forge 1.20.1 客户端偏好设置

暂停菜单和模组列表都提供“下一个边境”设置入口。窗口使用原版 Screen、Button 和 AbstractWidget，无需 LDLib2 或其他界面前置。完成和 Esc 返回父界面，单人游戏内保持暂停。

附属模组在仅客户端的 FMLClientSetupEvent.enqueueWork 中调用 PreferencePages.register(id, title, order, factory)。页面按 order、资源 ID 排序；重复 ID 会抛出异常。

factory 返回新的 List<AbstractWidget>。控件位置相对于页面内容左上角，内容宽度最多 274 个界面单位；宿主处理分页和滚动。窗口内切页保留控件实例、输入值和每页滚动位置，重新打开窗口创建新的控件。

```java
event.enqueueWork(() -> PreferencePages.register(
    new ResourceLocation("my_addon", "preferences"),
    Component.literal("附属设置"), 200,
    () -> List.of(Button.builder(Component.literal("设置项"), button -> {
        // 保存附属模组自己的配置。
    }).bounds(0, 0, 260, 20).build())));
```

这是 Forge 分支的原版控件接口，NeoForge 分支返回 LDLib2 UIElement 的工厂需要在附属模组 Forge 移植时调整。注册表与页面 ID 的约定保持一致。

执行 gradlew runForgeClientQa 可运行独立客户端验收。测试模组不会打入正式 JAR，结果与截图位于 build/forge-client-qa/。
