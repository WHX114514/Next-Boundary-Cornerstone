package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.body.collision.PhysicsBox;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CollisionGetter.class)
public interface CompoundCollisionQueryMixin {
    @Inject(method = "noCollision(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Z", at = @At("HEAD"), cancellable = true)
    private void nextboundary$preciseQuery(Entity entity, AABB box, CallbackInfoReturnable<Boolean> cir) {
        if (!(box instanceof PhysicsBox body)) return;
        CollisionGetter level = (CollisionGetter) this;
        for (var shape : level.getBlockCollisions(entity, body)) {
            if (!shape.isEmpty()) { cir.setReturnValue(false); return; }
        }
        for (var shape : level.getEntityCollisions(entity, body)) {
            if (body.intersectsShape(shape)) { cir.setReturnValue(false); return; }
        }
        var border = level.getWorldBorder();
        cir.setReturnValue(entity == null || !border.isInsideCloseToBorder(entity, body)
                || !body.intersectsShape(border.getCollisionShape()));
    }
}
