package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 直接拦截抛壳数据的 POJO 类，避开所有渲染层的复杂逻辑和混淆
@Mixin(targets = "com.tacz.guns.client.resource.pojo.display.gun.ShellEjection", remap = false)
public class MixinShellRender {

    // 1. 拦截加速度（重力）—— 斩断抛物线！
    @Inject(method = "getAcceleration", at = @At("HEAD"), cancellable = true)
    private void nextboundary$removeGravity(CallbackInfoReturnable<Vector3f> cir) {
        if (GlobalVariables.B_LowGravity) {
            // 返回绝对的 0 加速度，让抛物线变成直线
            cir.setReturnValue(new Vector3f(0f, 0f, 0f));
        }
    }

    // 2. 拦截初速度 —— 营造太空缓慢漂浮感
    @Inject(method = "getInitialVelocity", at = @At("RETURN"), cancellable = true)
    private void nextboundary$slowVelocity(CallbackInfoReturnable<Vector3f> cir) {
        if (GlobalVariables.B_LowGravity) {
            Vector3f original = cir.getReturnValue();
            if (original != null) {
                // 把弹出的力度降为原来的 30%
                cir.setReturnValue(new Vector3f(original).mul(0.3f));
            }
        }
    }

    // 3. 拦截角速度 —— 让弹壳自转也慢下来
    @Inject(method = "getAngularVelocity", at = @At("RETURN"), cancellable = true)
    private void nextboundary$slowAngular(CallbackInfoReturnable<Vector3f> cir) {
        if (GlobalVariables.B_LowGravity) {
            Vector3f original = cir.getReturnValue();
            if (original != null) {
                // 自转速度降为 20%
                cir.setReturnValue(new Vector3f(original).mul(0.2f));
            }
        }
    }

    // 4. 拦截存活时间 —— 强行续命到 30 秒！
    @Inject(method = "getLivingTime", at = @At("RETURN"), cancellable = true)
    private void nextboundary$extendLifetime(CallbackInfoReturnable<Float> cir) {
        if (GlobalVariables.B_LowGravity) {
            // 原版通常只有 1~2 秒，这里直接强行赋值为 30.0f 秒
            // 只要没到 30 秒，渲染器就不会把它从队列里踢出去
            cir.setReturnValue(30.0f);
        }
    }
}