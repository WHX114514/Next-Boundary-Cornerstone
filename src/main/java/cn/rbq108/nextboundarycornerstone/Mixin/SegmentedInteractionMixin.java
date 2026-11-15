package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.extensions.IForgePlayer;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Server reach checks use distance to the nearest piece, including outstretched hands/feet. */
@Mixin(IForgePlayer.class)
public interface SegmentedInteractionMixin {
    /**
     * @author Next Boundary Cornerstone
     * @reason Measure zero-gravity reach against body pieces; preserve Forge reach for other entities.
     */
    @org.spongepowered.asm.mixin.Overwrite(remap = false)
    default boolean isCloseEnough(Entity target, double distance) {
        var player = (net.minecraft.world.entity.player.Player) (Object) this;
        AABB box = SegmentedHitboxes.enabled(target) ? SegmentedHitboxes.forHitQuery(target) : target.getBoundingBox();
        return box.inflate(target.getPickRadius()).distanceToSqr(player.getEyePosition()) < distance * distance;
    }
}
