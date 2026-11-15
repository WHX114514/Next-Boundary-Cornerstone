# 客户端偏好设置扩展

基石提供暂停菜单“下一个边境”偏好设置入口、模组列表配置工厂、共享窗口和通用配色。客户端需要 LDLib2 2.2.42 或更新版本。专用服务端不注册客户端入口。窗口按注册顺序值、页面资源 ID 排序，分类导航横向溢出可滚动，各页独立纵向滚动，完成与 Esc 返回父界面，单人游戏内保持暂停。

附属模组在仅客户端的 `FMLClientSetupEvent` 中通过 `event.enqueueWork` 调用 `cn.rbq108.nextboundarycornerstone.api.client.PreferencePages.register(id, title, order, factory)`。使用自己的命名空间，重复 ID 会明确报错。`factory` 每次打开返回新的 LDLib2 `UIElement`，内容宽 274 个逻辑单位，高度按实际内容设置；内容较长时由宿主滚动。可使用 `PreferencesTheme` 的标签、按钮与定位工具，避免改全局样式。注册表返回不可变有序快照。

```java
event.enqueueWork(() -> PreferencePages.register(
    ResourceLocation.fromNamespaceAndPath("my_addon", "preferences"),
    Component.translatable("ui.my_addon.preferences"), 200,
    MyPreferences::createContent));
```

通用 UI 空态由基石提供；工业机器页由工业注册，刷新率、液体单位与翻译仍由工业维护。继续使用 `next_boundary_industry-chemistry-client.toml`，不迁移配置键或存档，也不改变机器读数刷新和加工逻辑。工业的 `runModSettingsClientQa` 在中文、GUI 3、ModernUI 中验证跨模组接入、唯一暂停入口、保存、切页、滚动、完成／Esc 和两个模组的配置工厂。相关服务端验证为工业的 `MachineDisplayRefreshGameTests`。

基石单独验收使用 `:next_boundary_cornerstone:runPreferencesClientQa`（从工业联合构建根目录运行），或在基石根目录执行 `:runPreferencesClientQa`。客户端测试源码独立于正式 JAR，验证未安装工业时的配置工厂、通用 UI 页、附属页面注册、重复 ID 和重开生成新内容。若需 ModernUI，在 `.local-artifacts/preferences/game/mods/` 放置该模组，截图与结果保存在 `.local-artifacts/preferences/`。
