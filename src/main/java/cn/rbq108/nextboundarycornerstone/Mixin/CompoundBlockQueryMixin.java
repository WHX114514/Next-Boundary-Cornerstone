package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.body.collision.PhysicsBox;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Block probes use sixteen boxes directly, never a dense grid of the whole body. */
@Mixin(BlockCollisions.class)
public abstract class CompoundBlockQueryMixin {
    @Shadow @Final private AABB box;

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/phys/shapes/Shapes;create(Lnet/minecraft/world/phys/AABB;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    private VoxelShape nextboundary$broadShape(AABB query, Operation<VoxelShape> original) {
        if (query instanceof PhysicsBox) return Shapes.create(query.minX, query.minY, query.minZ, query.maxX, query.maxY, query.maxZ);
        return original.call(query);
    }

    @WrapOperation(method = "computeNext", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/phys/shapes/Shapes;joinIsNotEmpty(Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/shapes/VoxelShape;Lnet/minecraft/world/phys/shapes/BooleanOp;)Z"))
    private boolean nextboundary$pieceIntersection(VoxelShape obstacle, VoxelShape query, BooleanOp operation, Operation<Boolean> original) {
        if (box instanceof PhysicsBox body && operation == BooleanOp.AND) return body.intersectsShape(obstacle);
        return original.call(obstacle, query, operation);
    }
}
