package cn.rbq108.nextboundarycornerstone.qa.mixin;

import cn.rbq108.nextboundarycornerstone.qa.TACZQaState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class HandPoseProbeMixin {
    @Inject(method="renderHand", at=@At("RETURN"))
    private void checkHand(PoseStack stack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                           ModelPart arm, ModelPart sleeve, CallbackInfo ci) {
        TACZQaState.hand(arm, arm == ((PlayerRenderer)(Object)this).getModel().leftArm);
    }
}
