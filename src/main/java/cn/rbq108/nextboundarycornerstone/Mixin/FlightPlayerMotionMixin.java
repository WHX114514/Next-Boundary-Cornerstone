package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.motion.FlightVelocity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class FlightPlayerMotionMixin {
    @WrapOperation(method = "travel", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/player/Player;setDeltaMovement(DDD)V"))
    private void cornerstone$flightDamping(Player player, double x, double y, double z, Operation<Void> original) {
        if (FlightVelocity.controls(player)) FlightVelocity.internal(() -> original.call(player, x, y, z));
        else original.call(player, x, y, z);
    }

    @WrapOperation(method = "travel", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/player/Player;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private void cornerstone$swimDamping(Player player, Vec3 motion, Operation<Void> original) {
        if (FlightVelocity.controls(player)) FlightVelocity.internal(() -> original.call(player, motion));
        else original.call(player, motion);
    }
}
