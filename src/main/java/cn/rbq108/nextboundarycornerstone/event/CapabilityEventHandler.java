package cn.rbq108.nextboundarycornerstone.event;

import cn.rbq108.nextboundarycornerstone.capability.PlayerRotationProvider;
import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = main.MODID)
public final class CapabilityEventHandler {
    @SubscribeEvent public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            var provider = new PlayerRotationProvider();
            event.addCapability(new ResourceLocation(main.MODID, "player_rotation"), provider);
            event.addListener(provider::invalidate);
        }
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        var original = event.getOriginal();
        original.reviveCaps();
        original.getCapability(PlayerRotationProvider.PLAYER_ROTATION).ifPresent(old ->
            event.getEntity().getCapability(PlayerRotationProvider.PLAYER_ROTATION).ifPresent(copy -> {
                copy.setQuaternion(old.getQuaternion());
                copy.setLowGravity(old.isLowGravity());
            }));
        original.invalidateCaps();
    }
}
