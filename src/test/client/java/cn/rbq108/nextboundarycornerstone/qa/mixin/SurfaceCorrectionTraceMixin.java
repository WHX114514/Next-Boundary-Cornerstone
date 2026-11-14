package cn.rbq108.nextboundarycornerstone.qa.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class SurfaceCorrectionTraceMixin {
    @Inject(method = "handleMovePlayer", at = @At("TAIL"))
    private void surfaceqa$correction(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        if (Boolean.getBoolean("cornerstone.surface.qa")) com.mojang.logging.LogUtils.getLogger().info(
                "SURFACE_POSITION_PACKET x={} y={} z={} id={}", packet.getX(), packet.getY(), packet.getZ(), packet.getId());
    }
}
