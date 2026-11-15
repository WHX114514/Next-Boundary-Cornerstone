package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.body.collision.SegmentedPhysics;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class SegmentedPhysicsMixin {
    @ModifyConstant(method = "move", constant = @Constant(doubleValue = 1.0e-7, ordinal = 0),
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;")))
    private double nextboundary$keepSmallResolvedMovement(double minimumSquaredDistance) {
        // collide has already checked the complete body. Dropping a sub-0.000316
        // nudge here would incorrectly make a feasible turn look body-blocked.
        return (Object) this instanceof Player player && SegmentedPhysics.enabled(player) ? 0 : minimumSquaredDistance;
    }

    @Inject(method = "getBoundingBox", at = @At("HEAD"), cancellable = true)
    private void nextboundary$physicalBody(CallbackInfoReturnable<AABB> cir) {
        if ((Object) this instanceof Player player && SegmentedPhysics.enabled(player)) cir.setReturnValue(SegmentedPhysics.box(player));
    }

    @Inject(method = "collide", at = @At("HEAD"), cancellable = true)
    private void nextboundary$compoundMovement(Vec3 requested, CallbackInfoReturnable<Vec3> cir) {
        if ((Object) this instanceof Player player && SegmentedPhysics.enabled(player)) cir.setReturnValue(SegmentedPhysics.clipMovement(player, requested));
    }

    @WrapOperation(method = "checkInsideBlocks", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;entityInside(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)V"))
    private void nextboundary$actualInside(BlockState state, Level level, BlockPos position, Entity entity, Operation<Void> original) {
        if (entity instanceof Player player && SegmentedPhysics.enabled(player)) {
            var block = new AABB(position);
            if (SegmentedPhysics.box(player).pieces().stream().noneMatch(piece -> piece.intersects(block))) return;
        }
        original.call(state, level, position, entity);
    }
}
