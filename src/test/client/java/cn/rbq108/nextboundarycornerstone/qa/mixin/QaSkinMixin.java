package cn.rbq108.nextboundarycornerstone.qa.mixin;

import cn.rbq108.nextboundarycornerstone.qa.TACZQaState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class QaSkinMixin {
    @Inject(method="getModelName", at=@At("RETURN"), cancellable=true)
    private void skin(CallbackInfoReturnable<String> ci) {
        if (TACZQaState.active && (Object)this == Minecraft.getInstance().player)
            ci.setReturnValue(TACZQaState.slim ? "slim" : "default");
    }
}
