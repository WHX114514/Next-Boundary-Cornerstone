package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.camera.HandSway;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public abstract class FirstPersonSwayMixin {
    @WrapOperation(method = "renderItemInHand", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/player/LocalPlayer;I)V"))
    private void cornerstone$renderHandSway(ItemInHandRenderer renderer, float partialTick, PoseStack pose,
                                           MultiBufferSource.BufferSource buffers, LocalPlayer player,
                                           int light, Operation<Void> original) {
        HandSway.withRenderBob(player, partialTick, () -> original.call(renderer, partialTick, pose, buffers, player, light));
    }
}
