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
import cn.rbq108.nextboundarycornerstone.camera.HandSway;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer {

    // Match LocalPlayer's current-frame view getters; only passenger yaw is interpolated.
    @WrapOperation(method = "renderHandsWithItems", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/player/LocalPlayer;getViewXRot(F)F"))
    private float cornerstone$screenPitch(LocalPlayer player, float partialTick, Operation<Float> original) {
        return player.isNoGravity() ? HandSway.pitch(1) : original.call(player, partialTick);
    }

    @WrapOperation(method = "renderHandsWithItems", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/player/LocalPlayer;getViewYRot(F)F"))
    private float cornerstone$screenYaw(LocalPlayer player, float partialTick, Operation<Float> original) {
        return player.isNoGravity() ? HandSway.yaw(player.isPassenger() ? partialTick : 1) : original.call(player, partialTick);
    }

    @WrapOperation(method = "renderHandsWithItems", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
    private float cornerstone$handPitch(LocalPlayer player, Operation<Float> original) {
        return player.isNoGravity() ? HandSway.pitch(1) : original.call(player);
    }

    @ModifyExpressionValue(method = "renderHandsWithItems", at = @At(value = "FIELD", target =
            "Lnet/minecraft/client/player/LocalPlayer;xRotO:F"))
    private float cornerstone$previousHandPitch(float original) {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        return player != null && player.isNoGravity() ? HandSway.pitch(0) : original;
    }

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"))
    private void onRenderHandsWithItems(
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource.BufferSource bufferSource,
            LocalPlayer player,
            int combinedLight,
            CallbackInfo ci
    ) {
        if (player.isNoGravity() && cn.rbq108.nextboundarycornerstone.api.FreeLookAPI.isFreeLookActive()) {
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
