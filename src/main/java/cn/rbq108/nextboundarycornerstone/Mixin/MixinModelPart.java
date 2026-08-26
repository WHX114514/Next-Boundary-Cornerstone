package cn.rbq108.nextboundarycornerstone.Mixin; // 🩺 确保是大写的 Mixin

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelPart.class)
public abstract class MixinModelPart {
    @Shadow public float x;
    @Shadow public float y;
    @Shadow public float z;

    @Inject(method = "translateAndRotate", at = @At("HEAD"), cancellable = true)
    public void onTranslateAndRotate(PoseStack poseStack, CallbackInfo ci) {
        net.minecraft.client.player.LocalPlayer localPlayer = net.minecraft.client.Minecraft.getInstance().player;
        if (localPlayer != null && localPlayer.isNoGravity() && GlobalVariables.isPlayerRendering) {
            if ((Object)this == GlobalVariables.playerHead || (Object)this == GlobalVariables.playerHat) {
                // 1. 保留脖子支点
                poseStack.translate(this.x / 16.0F, this.y / 16.0F, this.z / 16.0F);
                // 2. 注入抵消矩阵（彻底锁头）
                if (GlobalVariables.headFixQuat != null) {
                    poseStack.mulPose(GlobalVariables.headFixQuat);
                }
                // 3. 拦截原版，不让它乱动
                ci.cancel();
            }
        }
    }
}