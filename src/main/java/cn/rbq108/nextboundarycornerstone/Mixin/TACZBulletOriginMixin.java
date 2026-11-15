package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxes;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Use the same three-dimensional eye anchor as the camera and authoritative rays. */
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
}
