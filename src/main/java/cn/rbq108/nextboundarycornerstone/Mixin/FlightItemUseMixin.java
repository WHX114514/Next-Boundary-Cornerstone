package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.ServeMiao.communication.PlayerSent;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Queue the current view before the use/release packet, including turns since the last tick. */
@Mixin(MultiPlayerGameMode.class)
public abstract class FlightItemUseMixin {
    @Inject(method = "useItem", at = @At("HEAD"))
    private void cornerstone$syncBeforeUse(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        PlayerSent.sendRotation();
    }

    @Inject(method = "releaseUsingItem", at = @At("HEAD"))
    private void cornerstone$syncBeforeRelease(Player player, CallbackInfo ci) {
        PlayerSent.sendRotation();
    }
}
