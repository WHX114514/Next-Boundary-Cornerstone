package cn.rbq108.nextboundarycornerstone.MixinTACZ;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 直接锁定 TACZ 的后座力数据源
 * 无论它是怎么计算后座力位移的，只要它问“现在后座力导致的视角偏移是多少”，我们就强制返回 0
 */
@Mixin(targets = "com.tacz.guns.client.gameplay.LocalPlayerRecoil", remap = false)
public class PatchMixin {

    // 拦截获取 Pitch 偏移的方法 (通常类似 getRecoilPitch 或类似名称)
    // 如果报 Method not found，请检查 TACZ 源码中该类的 getRecoilPitch 方法名
    @Inject(method = "getRecoilPitch", at = @At("HEAD"), cancellable = true)
    private void onGetRecoilPitch(CallbackInfoReturnable<Float> cir) {
        if (GlobalVariables.B_LowGravity) {
            cir.setReturnValue(0.0f); // 强行返回0，TACZ 将认为当前没有任何后座力导致的上抬
        }
    }

    // 拦截获取 Yaw 偏移的方法
    @Inject(method = "getRecoilYaw", at = @At("HEAD"), cancellable = true)
    private void onGetRecoilYaw(CallbackInfoReturnable<Float> cir) {
        if (GlobalVariables.B_LowGravity) {
            cir.setReturnValue(0.0f); // 强行返回0
        }
    }
}
//package cn.rbq108.nextboundarycornerstone.MixinTACZ;
//
//import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
//import net.minecraft.client.player.LocalPlayer;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//@Mixin(LocalPlayer.class)
//public class PatchMixin {
//
//    /**
//     * 我们不再拦截 TACZ，而是拦截 Minecraft 的“视角刷新时刻”。
//     * 在每一帧视角被应用到渲染前，强制修正为我们的四元数数值。
//     */
////    @Inject(method = "aiStep()V", at = @At("TAIL"))
////    private void forceCorrectView(CallbackInfo ci) {
////        if (GlobalVariables.B_LowGravity) {
////            LocalPlayer player = (LocalPlayer) (Object) this;
////
////            // 从你的四元数里反向解算出正确的欧拉角
////            org.joml.Vector3f euler = GlobalVariables.currentQuat.getEulerAnglesYXZ(new org.joml.Vector3f());
////            float correctYaw = (float) Math.toDegrees(-euler.y);
////            float correctPitch = (float) Math.toDegrees(euler.x);
////
////            // 强制覆盖，将 TACZ 在这一帧可能产生的所有偏转强行抹平
////            player.setYRot(correctYaw);
////            player.setXRot(correctPitch);
////        }
////    }
//}