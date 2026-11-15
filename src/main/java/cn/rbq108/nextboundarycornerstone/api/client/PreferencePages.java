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

/** Client-only extension point. Register from FMLClientSetupEvent.enqueueWork. */
public final class PreferencePages {
    private static final Map<ResourceLocation, Page> PAGES = new LinkedHashMap<>();
    private PreferencePages() {}

    /** Factories return fresh 274-unit-wide content. Host owns tabs, scrolling and navigation.
     * Use the registering mod's namespace for IDs. Lower order appears first.
     */
    public static synchronized void register(ResourceLocation id, Component title, int order, Supplier<UIElement> content) {
        var page = new Page(Objects.requireNonNull(id), Objects.requireNonNull(title), order, Objects.requireNonNull(content));
        if (PAGES.putIfAbsent(id, page) != null) throw new IllegalArgumentException("Duplicate preference page: " + id);
    }

    /** Immutable ordered snapshot. New windows obtain fresh content from each factory. */
    public static synchronized List<Page> pages() {
        return PAGES.values().stream().sorted(Comparator.comparingInt(Page::order)
            .thenComparing(page -> page.id().toString())).toList();
    }

    public record Page(ResourceLocation id, Component title, int order, Supplier<UIElement> content) {}
}
