package cn.rbq108.nextboundarycornerstone.qa.mixin;

import cn.rbq108.nextboundarycornerstone.qa.GunModelJitterQa;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe real TaCZ sway without changing its angle reads or drawing a mesh. */
@Pseudo
@Mixin(targets = {"com.tacz.guns.client.renderer.item.AnimateGeoItemRenderer",
        "com.tacz.guns.client.renderer.item.GunItemRendererWrapper"}, remap = false)
public abstract class GunSwayProbeMixin {
    @Inject(method = {"renderFirstPerson", "lambda$renderFirstPerson$5"},
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V",
                    ordinal = 1, shift = At.Shift.AFTER), cancellable = true)
    private void cornerstone$sample(CallbackInfo ci) {
        if (!GunModelJitterQa.probing) return;
        GunModelJitterQa.matrix = new Matrix4f(GunModelJitterQa.pose.last().pose());
        GunModelJitterQa.capturedPath = ci.getId();
        GunModelJitterQa.pose.popPose();
        ci.cancel();
    }
}
