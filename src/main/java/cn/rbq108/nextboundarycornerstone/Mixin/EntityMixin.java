package cn.rbq108.nextboundarycornerstone.Mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import cn.rbq108.nextboundarycornerstone.api.RollEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin implements RollEntity {

    @Unique
    private boolean isRolling_testMod;
    @Unique
    private float roll_testMod;
    @Unique
    private float customYaw_testMod;
    @Unique
    private float customPitch_testMod;

    @Override
    public boolean doABarrelRoll$isRolling() {
        isRolling_testMod = true;
        return this.isRolling_testMod;
    }

    @Override
    public float doABarrelRoll$getRoll(float tickDelta) {
        if (this.isRolling_testMod) {
            return cn.rbq108.nextboundarycornerstone.camera.CameraManager.prevRoll +
                    (cn.rbq108.nextboundarycornerstone.camera.CameraManager.currentRoll - cn.rbq108.nextboundarycornerstone.camera.CameraManager.prevRoll) * tickDelta;
        }
        return 0.0f;
    }

    @Override
    public float doABarrelRoll$getYaw(float tickDelta) {
        return 0.0f;
    }

    @Override
    public float doABarrelRoll$getPitch(float tickDelta) {
        return 0.0f;
    }

    @Inject(method = "turn(DD)V", at = @At("HEAD"), cancellable = true)
    private void doABarrelRoll$quaternionTurn(double yRot, double xRot, CallbackInfo ci) {
        if (cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_LowGravity && (Object)this instanceof Player player) {
            ci.cancel();

            // 提取鼠标输入的增量
            float dy = (float) yRot * 0.15f; // 鼠标横向移动 (控制 Yaw)
            float dx = (float) xRot * 0.15f; // 鼠标纵向移动 (控制 Pitch)

            // 绕x轴是俯仰(dx)，绕Y是偏航(dy)
            cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.currentQuat.rotateX((float) Math.toRadians(dx));
            cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.currentQuat.rotateY((float) Math.toRadians(-dy));

            // 防止万向节死锁(Gimbal Lock)导致的180度翻转，改用前向向量提取平滑的 Yaw 和 Pitch
            org.joml.Vector3f forward = new org.joml.Vector3f(0, 0, 1).rotate(cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.currentQuat);
            
            float currentYaw = player.getYRot();
            float currentPitch = player.getXRot();
            
            double horizontalDistance = Math.sqrt(forward.x * forward.x + forward.z * forward.z);
            float newYaw = currentYaw; // 默认使用旧的Yaw，防止在正上/正下方时视角乱转
            if (horizontalDistance > 0.001) {
                newYaw = (float) Math.toDegrees(-Math.atan2(forward.x, forward.z));
            }
            float newPitch = (float) Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, -forward.y))));

            player.setYRot(currentYaw + net.minecraft.util.Mth.wrapDegrees(newYaw - currentYaw));
            player.setXRot(currentPitch + net.minecraft.util.Mth.wrapDegrees(newPitch - currentPitch));
        }
    }

    @Inject(method = "getUpVector(F)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void doABarrelRoll$injectUpVector(float partialTicks, CallbackInfoReturnable<Vec3> cir) {
        Entity entity = (Entity) (Object) this;
        if (cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_LowGravity && entity.getType() == net.minecraft.world.entity.EntityType.PLAYER) {
            org.joml.Vector3f up = new org.joml.Vector3f(0.0f, 1.0f, 0.0f);
            cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.currentQuat.transform(up);
            cir.setReturnValue(new Vec3(up.x(), up.y(), up.z()));
        }
    }
}