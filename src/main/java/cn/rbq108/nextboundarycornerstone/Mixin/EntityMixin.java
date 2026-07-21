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

            // === Yaw & Pitch ===
            // 绝对不能用四元数反推(asin/atan2)来设置 player 的 XRot 和 YRot！
            // 因为在倒立(roll=180°)或跨越±90°俯仰角时，四元数在世界坐标系下的分量会反转/停滞，
            // 导致原版手部摆动(ItemInHandRenderer)接收到反向或为零的视角增量。
            //
            // 正确做法：直接将屏幕坐标系的鼠标原始增量 (dx, dy) 累加到 XRot 和 YRot 上。
            // 这样手部摆动增量就永远与玩家当前屏幕的鼠标操作方向保持一致，
            // 无论角色处于何种 roll 角度(0°, 90°, 180°)，手部上下左右摆动都完全正常。
            player.setYRot(player.getYRot() + dy);
            player.setXRot(player.getXRot() + dx);
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