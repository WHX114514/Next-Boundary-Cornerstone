package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.body.collision.SegmentedPhysics;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pose approval and depenetration use physical pieces; vanilla's width-based corner samples do not. */
@Mixin(LocalPlayer.class)
public abstract class SegmentedPushOutMixin {
    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    private void nextboundary$checkPhysicalBody(double x, double z, CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (SegmentedPhysics.enabled(player)) ci.cancel();
    }
}
