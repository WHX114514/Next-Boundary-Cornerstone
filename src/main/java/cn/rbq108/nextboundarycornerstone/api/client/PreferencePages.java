package cn.rbq108.nextboundarycornerstone.api.client;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
import java.util.function.Supplier;

/** Forge native preference pages. Register during client setup via enqueueWork. */
public final class PreferencePages {
    private static final Map<ResourceLocation, Page> PAGES = new LinkedHashMap<>();
    private PreferencePages() {}

    /** Each factory returns fresh widgets with page-relative positions and width up to 274. */
    public static synchronized void register(ResourceLocation id, Component title, int order,
                                             Supplier<List<AbstractWidget>> content) {
        var page = new Page(Objects.requireNonNull(id), Objects.requireNonNull(title), order, Objects.requireNonNull(content));
        if (PAGES.putIfAbsent(id, page) != null) throw new IllegalArgumentException("Duplicate preference page: " + id);
    }
    public static synchronized List<Page> pages() {
        return PAGES.values().stream().sorted(Comparator.comparingInt(Page::order)
                .thenComparing(page -> page.id().toString())).toList();
    }
    public record Page(ResourceLocation id, Component title, int order, Supplier<List<AbstractWidget>> content) {}
}
