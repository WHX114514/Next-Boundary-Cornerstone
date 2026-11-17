package cn.rbq108.nextboundarycornerstone.Mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import cn.rbq108.nextboundarycornerstone.api.RollEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class EntityMixin implements RollEntity {

    // DDD, push and lerpMotion all delegate to this setter. Capture the final value once.
    @Inject(method = "setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V", at = @At("TAIL"))
    private void cornerstone$externalVelocity(net.minecraft.world.phys.Vec3 velocity, CallbackInfo ci) {
        cn.rbq108.nextboundarycornerstone.motion.FlightVelocity.capture((Entity)(Object)this, velocity);
    }

    @Unique
    private static boolean isLocalPlayerClass_testMod(Player player) {
        return player.getClass().getName().equals("net.minecraft.client.player.LocalPlayer");
    }

    @Inject(method = "getViewVector(F)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void doABarrelRoll$overrideGetViewVector(float partialTicks, CallbackInfoReturnable<net.minecraft.world.phys.Vec3> cir) {
        if ((Object)this instanceof Player player && player.isNoGravity()) {
            if (isLocalPlayerClass_testMod(player)) {
                org.joml.Quaternionf smoothedQuat = new org.joml.Quaternionf(cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.prevQuat)
                        .slerp(cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.currentQuat, partialTicks);
                org.joml.Vector3f fwd = new org.joml.Vector3f(0, 0, 1).rotate(smoothedQuat);
                cir.setReturnValue(new net.minecraft.world.phys.Vec3(fwd.x, fwd.y, fwd.z));
            }
        }
    }

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
        if ((Object)this instanceof Player player && player.isNoGravity()) {
            if (!isLocalPlayerClass_testMod(player)) {
                return;
            }
            ci.cancel();

            // 提取鼠标输入的增量
            float dy = (float) yRot * 0.15f; // 鼠标横向移动 (控制 Yaw)
            float dx = (float) xRot * 0.15f; // 鼠标纵向移动 (控制 Pitch)

            // 记录原始鼠标增量，供 GameRenderer.bobView 的 Mixin 使用
            // 这样手臂摆动就能用干净的鼠标输入驱动，而不是有死锁隐患的欧拉角差值
            cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.mouseDeltaX = dx; // 纵向（俯仰）
            cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.mouseDeltaY = dy; // 横向（偏航）

            if (cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_FreeCameraActive) {
                if (cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_HeadRotationLocked) {
                    // 如果头部已被锁定，视角自由随意转动（不受身体基准和限位束缚，绕自身相机轴）
                    cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.currentQuat.rotateX((float) Math.toRadians(dx));
                    cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.currentQuat.rotateY((float) Math.toRadians(-dy));
                } else {
                    cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_freeLookYaw += dy;
                    cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_freeLookPitch += dx;

                    // 第一人称视角限位：左右各自限制 110 度，上下各自限制 90 度
                    if (net.minecraft.client.Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
                        cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_freeLookYaw =
                            cn.rbq108.nextboundarycornerstone.body.FreeLookMath.limitYaw(cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_freeLookYaw, true);
                    } else {
                        // 第三人称下可以 360° 循环看
                        cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_freeLookYaw =
                            net.minecraft.util.Mth.wrapDegrees(cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_freeLookYaw);
                    }

                    // Clamp pitch in [-90, 90]
                    cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_freeLookPitch =
                        cn.rbq108.nextboundarycornerstone.body.FreeLookMath.clamp(cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_freeLookPitch,
                                cn.rbq108.nextboundarycornerstone.body.FreeLookMath.PITCH);
                }
            } else if (!cn.rbq108.nextboundarycornerstone.camera.CollisionFreeLook.turn(dy, dx, 0)) {
                // 绕x轴是俯仰(dx)，绕Y是偏航(dy)
                cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.currentQuat.rotateX((float) Math.toRadians(dx));
                cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.currentQuat.rotateY((float) Math.toRadians(-dy));
            }

            cn.rbq108.nextboundarycornerstone.camera.HandSway.turn((net.minecraft.client.player.LocalPlayer)player, dx, dy);
            cn.rbq108.nextboundarycornerstone.motion.FlightAim.publishAngles(player);
        }
    }

    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = "setNoGravity(Z)V", at = @org.spongepowered.asm.mixin.injection.At("HEAD"), argsOnly = true)
    private boolean nextboundary$modifyNoGravity(boolean noGravity) {
        if ((Object)this instanceof Player player) {
            if (cn.rbq108.nextboundarycornerstone.ServeMiao.communication.NetworkHandler.isSpectatorAndRestored(player)) {
                return false;
            }
        }
        return noGravity;
    }
}
