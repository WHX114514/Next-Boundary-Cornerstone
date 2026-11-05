package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.motion.SurfaceAdhesion;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPositionCorrectionMixin {
    @Inject(method = "handleMovePlayer", at = @At("TAIL"))
    private void nextboundary$resetSurfaceAfterTeleport(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        // Includes short same-dimension teleports and server position corrections.
        SurfaceAdhesion.reset();
    }
}
