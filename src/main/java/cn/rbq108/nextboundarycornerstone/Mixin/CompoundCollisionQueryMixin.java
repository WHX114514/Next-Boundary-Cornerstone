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
    /**
     * @author Next Boundary Cornerstone
     * @reason Forge's Mixin 0.8.5 cannot inject into interface methods. Preserve vanilla queries,
     *         checking the exact body pieces for compound physics boxes.
     */
    @org.spongepowered.asm.mixin.Overwrite
    default boolean noCollision(Entity entity, AABB box) {
        CollisionGetter level = (CollisionGetter) this;
        for (var shape : level.getBlockCollisions(entity, box)) if (!shape.isEmpty()) return false;
        var collisions = level.getEntityCollisions(entity, box);
        if (box instanceof PhysicsBox body) {
            for (var shape : collisions) if (body.intersectsShape(shape)) return false;
            var border = level.getWorldBorder();
            return entity == null || !border.isInsideCloseToBorder(entity, body)
                    || !body.intersectsShape(border.getCollisionShape());
        }
        if (!collisions.isEmpty()) return false;
        var border = level.getWorldBorder();
        return entity == null || !border.isInsideCloseToBorder(entity, box)
                || !net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(border.getCollisionShape(),
                        net.minecraft.world.phys.shapes.Shapes.create(box), net.minecraft.world.phys.shapes.BooleanOp.AND);
    }
}
