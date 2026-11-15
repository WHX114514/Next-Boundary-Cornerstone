package cn.rbq108.nextboundarycornerstone.client;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.client.preferences.ModSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public final class ModSettingsEntry {
    private ModSettingsEntry() {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        ModList.get().getModContainerById(main.MODID).orElseThrow()
            .registerExtensionPoint(IConfigScreenFactory.class, (container, parent) -> new ModSettingsScreen(parent));
    }

    @SubscribeEvent
    public static void pauseMenu(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen screen) || !screen.showsPauseMenu()) return;
        int x = 12, y = screen.height - 28, width = Math.min(204, screen.width - 24);
        for (var child : screen.children()) {
            if (child instanceof Button mods && mods.getMessage().getContents() instanceof TranslatableContents text
                    && text.getKey().equals("fml.menu.mods")) {
                width = mods.getWidth();
                x = mods.getX();
                y = mods.getY() + mods.getHeight() + 4;
                // Reserve a full row for the complete mod name and preferences label.
                for (var other : screen.children()) {
                    if (other instanceof Button button && button.getY() >= y) button.setY(button.getY() + 24);
                }
                break;
            }
        }
        event.addListener(Button.builder(Component.translatable("ui.next_boundary_cornerstone.settings.button"),
            button -> Minecraft.getInstance().setScreen(new ModSettingsScreen(screen)))
            .bounds(x, y, width, 20).build());
    }
}
