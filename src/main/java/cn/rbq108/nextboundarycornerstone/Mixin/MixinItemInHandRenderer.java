package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.player.LocalPlayer;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer {

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"))
    private void onRenderHandsWithItems(
            float partialTicks, 
            PoseStack poseStack, 
            MultiBufferSource.BufferSource bufferSource, 
            LocalPlayer player, 
            int combinedLight, 
            CallbackInfo ci
    ) {
        if (GlobalVariables.B_LowGravity && GlobalVariables.B_FreeCameraActive) {
            // 线性插值计算当前渲染帧视角和机体朝向的相对旋转关系，实现第一人称手部物理对齐机体
            Quaternionf smoothedCam = new Quaternionf(GlobalVariables.prevQuat)
                    .slerp(GlobalVariables.currentQuat, partialTicks);
            
            // 计算从当前视角旋转到机体朝向的转换旋转量
            Quaternionf camToBody = smoothedCam.conjugate().mul(GlobalVariables.getBodyQuat());
            
            // 应用到第一人称手部渲染的 PoseStack 上 (通过 Y 轴镜像变换基底，修正上下反向)
            poseStack.scale(1.0f, -1.0f, 1.0f);
            poseStack.mulPose(camToBody);
            poseStack.scale(1.0f, -1.0f, 1.0f);
        }
    }
}
