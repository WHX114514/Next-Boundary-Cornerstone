package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.ServeMiao.communication.PlayerSent;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Queue the current view before vanilla and custom-mod gameplay packets alike. */
@Mixin(Connection.class)
public abstract class FlightItemUseMixin {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;Z)V", at = @At("HEAD"))
    private void cornerstone$syncBeforeAction(Packet<?> packet, PacketSendListener listener, boolean flush, CallbackInfo ci) {
        PlayerSent.beforePacket((Connection)(Object)this);
    }
}
