package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxes;
import cn.rbq108.nextboundarycornerstone.motion.FlightAim;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Projectile.class)
public abstract class ProjectileLaunchMixin {
    @ModifyArgs(method = "shootFromRotation", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/projectile/Projectile;shoot(DDDFF)V"))
    private void cornerstone$flightLaunch(Args args, Entity shooter, float pitch, float yaw,
                                          float pitchOffset, float speed, float inaccuracy) {
        if (shooter instanceof Player player && SegmentedHitboxes.enabled(player)) {
            var direction = FlightAim.launchDirection(player, pitch, yaw, pitchOffset);
            args.set(0, direction.x);
            args.set(1, direction.y);
            args.set(2, direction.z);
        }
        // Vanilla still applies speed, inaccuracy, visual rotation and inherited shooter velocity.
    }
}
