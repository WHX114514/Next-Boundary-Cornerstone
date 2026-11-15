package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.body.collision.PhysicsBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla block queries and server movement validation must see the union, never its envelope. */
@Mixin(Shapes.class)
public abstract class CompoundShapeMixin {
    @Inject(method = "create(Lnet/minecraft/world/phys/AABB;)Lnet/minecraft/world/phys/shapes/VoxelShape;", at = @At("HEAD"), cancellable = true)
    private static void nextboundary$union(AABB box, CallbackInfoReturnable<VoxelShape> cir) {
        if (box instanceof PhysicsBox body) cir.setReturnValue(body.shape());
    }
}
