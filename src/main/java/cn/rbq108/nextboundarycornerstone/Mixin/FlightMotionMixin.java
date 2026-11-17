package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.motion.FlightVelocity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Exclude particular vanilla maintenance writes, never a whole travel/tick callback. */
@Mixin(LivingEntity.class)
public abstract class FlightMotionMixin {
    @WrapOperation(method = {"aiStep", "travel", "handleRelativeFrictionAndCalculateMovement"}, at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private void cornerstone$vanillaVector(LivingEntity entity, Vec3 motion, Operation<Void> original) {
        if (FlightVelocity.controls(entity)) FlightVelocity.internal(() -> original.call(entity, motion));
        else original.call(entity, motion);
    }

    @WrapOperation(method = {"aiStep", "travel"}, at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(DDD)V"))
    private void cornerstone$vanillaComponents(LivingEntity entity, double x, double y, double z, Operation<Void> original) {
        if (FlightVelocity.controls(entity)) FlightVelocity.internal(() -> original.call(entity, x, y, z));
        else original.call(entity, x, y, z);
    }

    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
    private Vec3 cornerstone$noDuplicateAcceleration(Vec3 input) {
        return FlightVelocity.controls((LivingEntity)(Object)this) ? Vec3.ZERO : input;
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "FIELD", opcode = org.objectweb.asm.Opcodes.GETFIELD, target =
            "Lnet/minecraft/world/entity/LivingEntity;jumping:Z"))
    private boolean cornerstone$noVanillaJump(boolean jumping) {
        return jumping && !FlightVelocity.controls((LivingEntity)(Object)this);
    }
}
