package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Server reach checks use distance to the nearest piece, including outstretched hands/feet. */
@Mixin(Player.class)
public abstract class SegmentedInteractionMixin {
    @WrapOperation(method = "canInteractWithEntity(Lnet/minecraft/world/entity/Entity;D)Z", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"))
    private AABB nextboundary$reach(Entity target, Operation<AABB> original) {
        return SegmentedHitboxes.enabled(target) ? SegmentedHitboxes.forHitQuery(target) : original.call(target);
    }
}
