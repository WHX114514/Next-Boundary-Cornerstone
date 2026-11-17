package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxes;
import cn.rbq108.nextboundarycornerstone.motion.FlightAim;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Server-side vector readers must see the same view as angle-based launchers. */
@Mixin(Entity.class)
public abstract class FlightViewMixin {
    @Inject(method = "getLookAngle", at = @At("HEAD"), cancellable = true)
    private void cornerstone$look(CallbackInfoReturnable<Vec3> ci) {
        if (SegmentedHitboxes.enabled((Entity)(Object)this)) ci.setReturnValue(FlightAim.rotate((Player)(Object)this, 0, 0, 1));
    }

    @Inject(method = "getViewVector", at = @At("HEAD"), cancellable = true)
    private void cornerstone$serverView(float partialTick, CallbackInfoReturnable<Vec3> ci) {
        var entity = (Entity)(Object)this;
        // The local client has quaternion interpolation in EntityMixin; remote/server angles
        // are world angles and retain vanilla interpolation.
        if (!entity.level().isClientSide && partialTick == 1 && SegmentedHitboxes.enabled(entity))
            ci.setReturnValue(FlightAim.rotate((Player)entity, 0, 0, 1));
    }
}
