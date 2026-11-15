package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.body.BodyEyes;
import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Replace the base eye anchor before vanilla performs third-person zoom and obstruction checks. */
@Mixin(Camera.class)
public abstract class SegmentedEyeCameraMixin {
    @WrapOperation(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"))
    private void nextboundary$headEye(Camera camera, double x, double y, double z, Operation<Void> original,
                                     @Local(argsOnly = true) Entity entity, @Local(argsOnly = true) float partialTick) {
        if (entity instanceof Player player && SegmentedHitboxes.enabled(player)) {
            var eye = BodyEyes.position(player, partialTick);
            original.call(camera, eye.x, eye.y, eye.z);
        } else original.call(camera, x, y, z);
    }
}
