package cn.rbq108.nextboundarycornerstone.event;

import cn.rbq108.nextboundarycornerstone.capability.PlayerRotationProvider;
import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = main.MODID)
public class CapabilityEventHandler {

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(cn.rbq108.nextboundarycornerstone.capability.PlayerRotationCapability.class);
    }

    @SuppressWarnings("removal")
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(new ResourceLocation(main.MODID, "player_rotation"), new PlayerRotationProvider());
        }
    }
}