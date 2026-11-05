package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import cn.rbq108.nextboundarycornerstone.motion.SurfaceAdhesion;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Observe actual collision clipping, including entities and the world border, without moving twice. */
@Mixin(Entity.class)
public abstract class LocalPlayerCollisionMixin {
    @WrapOperation(method = "move", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 nextboundary$recordCollision(Entity entity, Vec3 requested, Operation<Vec3> original) {
        Vec3 resolved = original.call(entity, requested);
        if (entity instanceof LocalPlayer player && GlobalVariables.B_LowGravity) {
            SurfaceAdhesion.recordCollision(player, requested, resolved);
        }
        return resolved;
    }
}
