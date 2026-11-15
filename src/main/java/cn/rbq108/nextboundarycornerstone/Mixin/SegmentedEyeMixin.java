package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.body.BodyEyes;
import cn.rbq108.nextboundarycornerstone.body.EyeBlockChecks;
import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxes;
import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** All eye-origin APIs agree in both JVM sides; camera-only height edits would desync picking. */
@Mixin(Entity.class)
public abstract class SegmentedEyeMixin {
    @Inject(method = "isInWall", at = @At("HEAD"), cancellable = true)
    private void nextboundary$eyeSuffocation(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player && SegmentedHitboxes.enabled(player))
            cir.setReturnValue(EyeBlockChecks.suffocating(player));
    }

    @Inject(method = "getEyePosition()Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void nextboundary$currentEye(CallbackInfoReturnable<Vec3> cir) {
        if ((Object) this instanceof Player player && SegmentedHitboxes.enabled(player)) cir.setReturnValue(BodyEyes.position(player));
    }

    @Inject(method = "getEyePosition(F)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void nextboundary$renderEye(float partialTick, CallbackInfoReturnable<Vec3> cir) {
        if ((Object) this instanceof Player player && SegmentedHitboxes.enabled(player)) cir.setReturnValue(BodyEyes.position(player, partialTick));
    }

    @Inject(method = "getEyeY", at = @At("HEAD"), cancellable = true)
    private void nextboundary$eyeY(CallbackInfoReturnable<Double> cir) {
        if ((Object) this instanceof Player player && SegmentedHitboxes.enabled(player)) cir.setReturnValue(BodyEyes.position(player).y);
    }

    @Inject(method = "getEyeHeight()F", at = @At("HEAD"), cancellable = true)
    private void nextboundary$eyeHeight(CallbackInfoReturnable<Float> cir) {
        if ((Object) this instanceof Player player && SegmentedHitboxes.enabled(player))
            cir.setReturnValue((float) cn.rbq108.nextboundarycornerstone.attachment.PlayerRotationAttachment.get(player).eyeOffset(player).y);
    }
}
