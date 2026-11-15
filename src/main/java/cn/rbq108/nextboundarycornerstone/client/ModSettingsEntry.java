package cn.rbq108.nextboundarycornerstone.client;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.client.preferences.ModSettingsScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.ConfigScreenHandler;

public final class ModSettingsEntry {
    private ModSettingsEntry() {}

    public static void setup(FMLClientSetupEvent event) {
        net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> new ModSettingsScreen(parent)));
    }

    @EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class Setup {
        @SubscribeEvent public static void clientSetup(FMLClientSetupEvent event) { setup(event); }
    }

}
