package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxes;
import cn.rbq108.nextboundarycornerstone.motion.FlightAim;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrowableProjectile.class)
public abstract class ThrowableEyeOriginMixin {
    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/LivingEntity;"
            + "Lnet/minecraft/world/level/Level;)V", at = @At("RETURN"))
    private void cornerstone$flightEyeOrigin(CallbackInfo ci) {
        var projectile = (Projectile) (Object) this;
        if (projectile.getOwner() instanceof Player player && SegmentedHitboxes.enabled(player)) {
            projectile.setPos(FlightAim.launchPosition(player));
        }
    }
}
