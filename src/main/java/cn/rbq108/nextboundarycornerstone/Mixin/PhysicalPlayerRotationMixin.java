package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxes;
import cn.rbq108.nextboundarycornerstone.main;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The renderer uses the accepted physical posture even if a requested pose change was blocked. */
@Mixin(PlayerRenderer.class)
public abstract class PhysicalPlayerRotationMixin {
    @Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            at = @At("HEAD"), cancellable = true)
    private void nextboundary$acceptedPosture(AbstractClientPlayer player, PoseStack stack, float age, float yaw,
                                              float partialTick, float scale, CallbackInfo ci) {
        if (!SegmentedHitboxes.enabled(player)) return;
        var pose = player.getData(main.PLAYER_ROTATION).renderHitboxPose(partialTick);
        if (pose == null) return;
        stack.mulPose(new Quaternionf().rotateY((float) Math.PI).rotateX(-pose.swim() * (float) Math.PI / 2));
        if (pose.swimming() && pose.swim() > 0) stack.translate(0, -1, .3f);
        ci.cancel();
    }
}
