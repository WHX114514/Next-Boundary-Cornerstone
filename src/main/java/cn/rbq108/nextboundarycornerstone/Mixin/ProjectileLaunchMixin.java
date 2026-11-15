package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxes;
import cn.rbq108.nextboundarycornerstone.motion.FlightAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Projectile.class)
public abstract class ProjectileLaunchMixin {
    @WrapOperation(method = "shootFromRotation", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/projectile/Projectile;shoot(DDDFF)V"))
    private void cornerstone$flightLaunch(Projectile projectile, double x, double y, double z,
                                          float speed, float inaccuracy, Operation<Void> original,
                                          @Local(argsOnly = true) Entity shooter,
                                          @Local(argsOnly = true, ordinal = 0) float pitch,
                                          @Local(argsOnly = true, ordinal = 1) float yaw,
                                          @Local(argsOnly = true, ordinal = 2) float pitchOffset) {
        if (shooter instanceof Player player && SegmentedHitboxes.enabled(player)) {
            var direction = FlightAim.launchDirection(player, pitch, yaw, pitchOffset);
            x = direction.x;
            y = direction.y;
            z = direction.z;
        }
        original.call(projectile, x, y, z, speed, inaccuracy);
        // Vanilla still applies speed, inaccuracy, visual rotation and inherited shooter velocity.
    }
}
