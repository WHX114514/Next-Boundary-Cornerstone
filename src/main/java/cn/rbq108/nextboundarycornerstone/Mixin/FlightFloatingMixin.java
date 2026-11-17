package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxes;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Exempt only server-authorized zero gravity from the player's floating check. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class FlightFloatingMixin {
    @Shadow public ServerPlayer player;

    @ModifyExpressionValue(method = "handleMovePlayer", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/server/MinecraftServer;isFlightAllowed()Z"))
    private boolean cornerstone$zeroGravityFloating(boolean allowed) {
        return allowed || SegmentedHitboxes.enabled(player);
    }
}
