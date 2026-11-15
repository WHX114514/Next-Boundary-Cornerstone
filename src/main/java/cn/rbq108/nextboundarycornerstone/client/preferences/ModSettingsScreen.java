package cn.rbq108.nextboundarycornerstone.client.preferences;

import cn.rbq108.nextboundarycornerstone.api.client.PreferencePages;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Native Forge preferences with independent scroll positions for registered pages. */
public final class ModSettingsScreen extends Screen {
    private final Screen parent;
    private final List<PreferencePages.Page> definitions = new ArrayList<>(PreferencePages.pages());
    private final Map<ResourceLocation, Integer> scrolls = new HashMap<>();
    private final List<AbstractWidget> content = new ArrayList<>();
    private final Map<ResourceLocation, List<AbstractWidget>> pageContents = new HashMap<>();
    private final Map<AbstractWidget, Integer> originalX = new IdentityHashMap<>();
    private final Map<AbstractWidget, Integer> originalY = new IdentityHashMap<>();
    private int selected, tabStart, left, top, pageHeight;

    public ModSettingsScreen(Screen parent) {
        super(text("title"));
        this.parent = parent;
        definitions.add(new PreferencePages.Page(new ResourceLocation("next_boundary_cornerstone", "ui"),
                text("ui"), Integer.MAX_VALUE, List::of));
    }
    private static Component text(String key) { return Component.translatable("ui.next_boundary_cornerstone.settings." + key); }

    @Override protected void init() {
        left = (width - 320) / 2;
        top = Math.max(4, (height - 218) / 2);
        clearWidgets();
        content.clear();
        addRenderableWidget(Button.builder(Component.literal("<"), b -> { tabStart = Math.max(0, tabStart - 1); init(); })
                .bounds(left + 14, top + 34, 18, 20).build()).active = tabStart > 0;
        for (int slot = 0; slot < 3 && tabStart + slot < definitions.size(); slot++) {
            int index = tabStart + slot;
            var tab = addRenderableWidget(Button.builder(definitions.get(index).title(), b -> { selected = index; init(); })
                    .bounds(left + 35 + slot * 83, top + 34, 80, 20).build());
            tab.active = index != selected;
        }
        addRenderableWidget(Button.builder(Component.literal(">"), b -> { tabStart++; init(); })
                .bounds(left + 288, top + 34, 18, 20).build()).active = tabStart + 3 < definitions.size();
        pageHeight = 120;
        var definition = definitions.get(selected);
        var widgets = pageContents.computeIfAbsent(definition.id(), id -> {
            var created = List.copyOf(definition.content().get());
            for (var widget : created) {
                originalX.put(widget, widget.getX());
                originalY.put(widget, widget.getY());
            }
            return created;
        });
        for (var widget : widgets) {
            int relativeY = originalY.get(widget);
            widget.setX(left + 22 + originalX.get(widget));
            pageHeight = Math.max(pageHeight, relativeY + widget.getHeight() + 4);
            content.add(widget);
            addWidget(widget);
        }
        positionContent();
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + 228, top + 188, 78, 20).build());
    }
    private int scroll() { return scrolls.getOrDefault(definitions.get(selected).id(), 0); }
    private void positionContent() {
        for (var widget : content) {
            widget.setY(top + 62 + originalY.get(widget) - scroll());
            widget.visible = widget.getY() >= top + 58 && widget.getY() + widget.getHeight() <= top + 178;
        }
    }
    @Override public boolean mouseScrolled(double x, double y, double delta) {
        if (x >= left + 14 && x < left + 306 && y >= top + 58 && y < top + 178) {
            scrolls.put(definitions.get(selected).id(), Math.max(0, Math.min(pageHeight - 120, scroll() - (int)(delta * 20))));
            positionContent();
            return true;
        }
        return super.mouseScrolled(x, y, delta);
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.fill(left, top, left + 320, top + 218, 0xFFE0E4DF);
        g.drawString(font, title, left + 14, top + 12, 0xFF263D42, false);
        g.fill(left + 14, top + 26, left + 306, top + 27, 0xFF819B9C);
        g.fill(left + 14, top + 58, left + 306, top + 178, 0xFFD5DEDA);
        super.render(g, mouseX, mouseY, partialTick);
        g.enableScissor(left + 14, top + 58, left + 306, top + 178);
        if (content.isEmpty()) g.drawWordWrap(font, text("ui_empty"), left + 22, top + 65, 274, 0xFF263D42);
        for (var widget : content) if (widget.visible) widget.render(g, mouseX, mouseY, partialTick);
        g.disableScissor();
        if (pageHeight > 120) {
            int thumb = Math.max(12, 120 * 120 / pageHeight);
            int y = top + 58 + scroll() * (120 - thumb) / (pageHeight - 120);
            g.fill(left + 301, top + 58, left + 305, top + 178, 0xFFB1C2BD);
            g.fill(left + 301, y, left + 305, y + thumb, 0xFF547F84);
        }
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return true; }
}
