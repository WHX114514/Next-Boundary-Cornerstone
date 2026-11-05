package cn.rbq108.nextboundarycornerstone.qa.mixin;

import cn.rbq108.nextboundarycornerstone.core.KeyPollingHandler;
import cn.rbq108.nextboundarycornerstone.qa.SurfaceClientQa;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Loaded only by the opt-in QA mod; production polling and physical bindings are unchanged. */
@Mixin(value = KeyPollingHandler.class, remap = false)
public abstract class SurfaceInputMixin {
    @Inject(method = "isPressed", at = @At("HEAD"), cancellable = true)
    private static void surfaceqa$simulatedInput(KeyMapping mapping, CallbackInfoReturnable<Boolean> result) {
        if (SurfaceClientQa.usesSimulatedInput()) result.setReturnValue(SurfaceClientQa.isHeld(mapping));
    }
}
