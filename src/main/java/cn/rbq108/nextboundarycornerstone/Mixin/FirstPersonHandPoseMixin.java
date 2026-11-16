package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.body.HandRenderContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerRenderer.class)
public abstract class FirstPersonHandPoseMixin {
    @WrapOperation(method = "renderHand", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/model/PlayerModel;setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V"))
    private void nextboundary$handAnimation(PlayerModel<?> model, LivingEntity player, float swing, float amount,
                                           float age, float yaw, float pitch, Operation<Void> original) {
        HandRenderContext.during(() -> original.call(model, player, swing, amount, age, yaw, pitch));
    }
}
