package cn.rbq108.nextboundarycornerstone.api.client;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** 用于注册客户端偏好设置页面。请在 FMLClientSetupEvent 的 enqueueWork 中调用 register。Client-only extension point. Register from FMLClientSetupEvent.enqueueWork. *///叽里咕噜说什么呢
public final class PreferencePages {
    private static final Map<ResourceLocation, Page> PAGES = new LinkedHashMap<>();
    private PreferencePages() {}

    /**
     *  注册一个偏好设置页面。
     *   content 每次调用都应创建新的页面内容，宽度为 274 个界面单位。
     *   标签页、滚动和页面切换由设置窗口统一处理。
     *   id 的命名空间应使用注册方的模组 ID；order 越小，页面越靠前。
     * Factories return fresh 274-unit-wide content. Host owns tabs, scrolling and navigation.
     * Use the registering mod's namespace for IDs. Lower order appears first.
     * GPT你能写人话吗！！
     */
    public static synchronized void register(ResourceLocation id, Component title, int order, Supplier<UIElement> content) {
        var page = new Page(Objects.requireNonNull(id), Objects.requireNonNull(title), order, Objects.requireNonNull(content));
        if (PAGES.putIfAbsent(id, page) != null) throw new IllegalArgumentException("Duplicate preference page: " + id);
    }

    /**
     * 返回按显示顺序排列的页面列表，列表不可修改。
     * 每次打开新窗口时，都会为各页面创建新的界面内容。
     * Immutable ordered snapshot. New windows obtain fresh content from each factory. */
    public static synchronized List<Page> pages() {
        return PAGES.values().stream().sorted(Comparator.comparingInt(Page::order)
            .thenComparing(page -> page.id().toString())).toList();
    }

    public record Page(ResourceLocation id, Component title, int order, Supplier<UIElement> content) {}
}
