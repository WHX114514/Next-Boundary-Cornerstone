package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxes;
import cn.rbq108.nextboundarycornerstone.motion.FlightAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CrossbowItem.class)
public abstract class CrossbowAimMixin {
    @WrapOperation(method = "shootProjectile", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/LivingEntity;getViewVector(F)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 cornerstone$flightForward(LivingEntity shooter, float partialTick, Operation<Vec3> original) {
        return shooter instanceof Player player && SegmentedHitboxes.enabled(player)
                ? FlightAim.rotate(player, 0, 0, 1) : original.call(shooter, partialTick);
    }

    @WrapOperation(method = "shootProjectile", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/LivingEntity;getUpVector(F)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 cornerstone$flightUp(LivingEntity shooter, float partialTick, Operation<Vec3> original) {
        return shooter instanceof Player player && SegmentedHitboxes.enabled(player)
                ? FlightAim.rotate(player, 0, 1, 0) : original.call(shooter, partialTick);
    }
}
