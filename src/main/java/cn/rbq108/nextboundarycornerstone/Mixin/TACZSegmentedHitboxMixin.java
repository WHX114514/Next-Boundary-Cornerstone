package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.body.SegmentedAABB;
import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

/** TACZ has its own ray tracer and upright headshot box, bypassing ProjectileUtil. */
@Pseudo
@Mixin(targets = "com.tacz.guns.util.EntityUtil", remap = false)
public abstract class TACZSegmentedHitboxMixin {
    @WrapOperation(method = {"findEntityOnPath", "findEntitiesOnPath"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"))
    private static List<Entity> nextboundary$candidates(Level level, Entity source, AABB area,
                                                        Predicate<? super Entity> predicate, Operation<List<Entity>> original) {
        return SegmentedHitboxes.candidates(level, source, area, predicate, original.call(level, source, area, predicate));
    }

    @Inject(method = "getHitResult", at = @At("HEAD"), cancellable = true)
    private static void nextboundary$hit(Projectile projectile, Entity target, Vec3 start, Vec3 end,
                                         CallbackInfoReturnable<EntityKineticBullet.EntityResult> cir) {
        if (!SegmentedHitboxes.enabled(target)) return;
        SegmentedAABB box = (SegmentedAABB) SegmentedHitboxes.forHitQuery(target);
        var hit = box.clip(start, end);
        if (hit.isEmpty()) {
            cir.setReturnValue(null);
            return;
        }
        boolean headshot = box.isHeadshot(start, end, hit.get());
        cir.setReturnValue(new EntityKineticBullet.EntityResult(target, hit.get(), headshot));
    }
}
