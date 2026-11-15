package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxes;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.client.Minecraft;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** TaCZ 1.20.1 renders muzzle offsets using yaw/pitch only. Add the missing camera roll once. */
@Pseudo
@Mixin(value = EntityKineticBullet.class, remap = false)
public abstract class FixTACZ {
    @Inject(method = "setFirstPersonRenderOffset", at = @At("HEAD"))
    private void nextboundary$tracerRoll(Vector3f offset, CallbackInfo ci) {
        var client = Minecraft.getInstance();
        var bullet = (EntityKineticBullet) (Object) this;
        if (offset == null || client.player == null || bullet.getOwner() != client.player
                || !SegmentedHitboxes.enabled(client.player)) return;
        var view = new Quaternionf(GlobalVariables.prevQuat).slerp(GlobalVariables.currentQuat, client.getFrameTime());
        var withoutRoll = new Quaternionf().rotationYXZ((float) Math.toRadians(-bullet.getCameraYRot()),
                (float) Math.toRadians(bullet.getCameraXRot()), 0);
        float roll = withoutRoll.conjugate().mul(view).getEulerAnglesYXZ(new Vector3f()).z;
        offset.rotateZ(-roll);
    }
}
