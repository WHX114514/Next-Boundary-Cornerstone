package cn.rbq108.nextboundarycornerstone.client.preferences;

import cn.rbq108.nextboundarycornerstone.api.client.PreferencePages;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import javax.annotation.ParametersAreNonnullByDefault;

/** Client preferences with independent, persistent scroll positions for each category. */
@ParametersAreNonnullByDefault
public final class ModSettingsScreen extends ModularUIScreen {
    private final Screen parent;

    public ModSettingsScreen(Screen parent) {
        super(createUI(parent), text("title"));
        this.parent = parent;
    }

    private static Component text(String key, Object... args) {
        return Component.translatable("ui.next_boundary_cornerstone.settings." + key, args);
    }

    private static ModularUI createUI(Screen parent) {
        var root = new UIElement().layout(l -> l.width(320).height(218));
        root.addClass("preferences-panel");
        root.setId("mod-settings");
        root.style(s -> s.background(new ColorRectTexture(0xFFE0E4DF)));
        root.addChildren(PreferencesTheme.label(text("title"), 14, 11, 292),
            PreferencesTheme.at(new UIElement(), 14, 26, 292, 1).style(s -> s.background(new ColorRectTexture(0xFF819B9C))));

        var definitions = new ArrayList<>(PreferencePages.pages());
        definitions.add(new PreferencePages.Page(ResourceLocation.fromNamespaceAndPath("next_boundary_cornerstone", "ui"),
            text("ui"), Integer.MAX_VALUE, () -> PreferencesTheme.label(text("ui_empty"), 0, 3, 274)));
        var pages = new ArrayList<ScrollerView>();
        var tabs = new ArrayList<Button>();
        var navigation = PreferencesTheme.scrollView(new ScrollerView());
        navigation.setId("settings-tabs");
        PreferencesTheme.at(navigation, 14, 34, 292, 22);
        navigation.scrollerStyle(style -> style.mode(ScrollerMode.HORIZONTAL));
        navigation.viewPort.layout(l -> l.paddingAll(0));
        navigation.viewPort.style(style -> style.background(new ColorRectTexture(0x00000000)));
        var tabContent = new UIElement().layout(l -> l.width(definitions.size() * 76).height(16));
        navigation.addScrollViewChild(tabContent);
        for (int i = 0; i < definitions.size(); i++) {
            var definition = definitions.get(i);
            String elementId = definition.id().toString().replace(':', '-').replace('/', '-');
            var page = page(elementId);
            page.addScrollViewChild(definition.content().get());
            var tab = PreferencesTheme.button(definition.title(), i * 76, 0, 70);
            tab.setId("tab-" + elementId);
            int index = i;
            tab.setOnClick(e -> select(pages, tabs, index));
            pages.add(page);
            tabs.add(tab);
            tabContent.addChild(tab);
            root.addChild(page);
        }
        select(pages, tabs, 0);
        root.addChild(navigation);

        var back = PreferencesTheme.button(Component.translatable("gui.done"), 228, 188, 78);
        back.setId("settings-done");
        back.setOnClick(e -> Minecraft.getInstance().setScreen(parent));
        root.addChild(back);
        return ModularUI.of(UI.of(root, PreferencesTheme.stylesheet()));
    }

    private static ScrollerView page(String id) {
        var page = PreferencesTheme.scrollView(new ScrollerView());
        page.setId(id);
        PreferencesTheme.at(page, 14, 58, 292, 120);
        page.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL));
        page.viewPort.layout(l -> l.paddingAll(4));
        page.viewPort.style(s -> s.background(new ColorRectTexture(0xFFD5DEDA)));
        page.viewContainer.layout(l -> l.widthPercent(100));
        return page;
    }

    private static void select(List<ScrollerView> pages, List<Button> tabs, int index) {
        for (int i = 0; i < pages.size(); i++) {
            boolean selected = i == index;
            pages.get(i).setDisplay(selected);
            tabs.get(i).setActive(!selected);
            int color = selected ? PreferencesTheme.CYAN : 0xFFD1D9D6;
            tabs.get(i).buttonStyle(style -> style.baseTexture(new ColorRectTexture(color)));
        }
    }

    @Override public boolean isPauseScreen() { return true; }

    @Override public void onClose() { Minecraft.getInstance().setScreen(parent); }

    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0x88000000);
    }
}
