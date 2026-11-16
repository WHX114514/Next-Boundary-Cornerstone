package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxes;
import cn.rbq108.nextboundarycornerstone.motion.FlightAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.joml.Vector2d;

/** Share the flight eye and aim, including TaCZ's custom spread overload that bypasses vanilla. */
@Pseudo
@Mixin(value = EntityKineticBullet.class, remap = false)
public abstract class TACZBulletOriginMixin {
    @Shadow private Vec3 startPos;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;"
            + "Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;"
            + "Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;"
            + "Lnet/minecraft/resources/ResourceLocation;ZLcom/tacz/guns/resource/pojo/data/gun/GunData;"
            + "Lcom/tacz/guns/resource/pojo/data/gun/BulletData;)V", at = @At("RETURN"))
    private void nextboundary$headOrigin(CallbackInfo ci) {
        var bullet = (EntityKineticBullet) (Object) this;
        if (bullet.getOwner() instanceof Player player && SegmentedHitboxes.enabled(player)) {
            var eye = player.getEyePosition();
            bullet.setPos(eye.x, eye.y, eye.z);
            // Damage falloff must use the same corrected origin as the physical bullet.
            startPos = eye;
        }
    }

    @WrapOperation(method = "shootFromRotation(Lnet/minecraft/world/entity/Entity;FFFFLorg/joml/Vector2d;)V",
            at = @At(value = "INVOKE", target = "Lcom/tacz/guns/entity/EntityKineticBullet;shoot(DDFLorg/joml/Vector2d;)V"))
    private void nextboundary$customSpreadAim(EntityKineticBullet bullet, double pitch, double yaw,
                                              float speed, Vector2d spread, Operation<Void> original,
                                              @Local(argsOnly = true) Entity shooter) {
        if (!(shooter instanceof Player player) || !SegmentedHitboxes.enabled(player)) {
            original.call(bullet, pitch, yaw, speed, spread);
            return;
        }
        // Let TaCZ generate its spread in the local frame, then use the common flight rotation.
        original.call(bullet, pitch - player.getXRot(), yaw - player.getYRot(), speed, spread);
        var local = bullet.getDeltaMovement();
        var velocity = FlightAim.rotate(player, local.x, local.y, local.z);
        bullet.setDeltaMovement(velocity);
        bullet.setYRot((float) Math.toDegrees(Math.atan2(velocity.x, velocity.z)));
        bullet.setXRot((float) Math.toDegrees(Math.atan2(velocity.y, velocity.horizontalDistance())));
        bullet.yRotO = bullet.getYRot();
        bullet.xRotO = bullet.getXRot();
        // TaCZ adds shooter velocity after this call; the join handler removes it once.
    }
}
