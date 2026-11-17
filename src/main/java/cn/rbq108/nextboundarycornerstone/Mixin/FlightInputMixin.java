package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.camera.HandSway;
import cn.rbq108.nextboundarycornerstone.motion.FlightVelocity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Change vanilla consumption only; other mods still receive real keys and movement input. */
@Mixin(LocalPlayer.class)
public abstract class FlightInputMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void cornerstone$swayTick(CallbackInfo ci) {
        if (FlightVelocity.controls((LocalPlayer)(Object)this)) HandSway.tick((LocalPlayer)(Object)this);
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "FIELD", opcode = org.objectweb.asm.Opcodes.GETFIELD, target =
            "Lnet/minecraft/client/player/Input;jumping:Z"))
    private boolean cornerstone$consumeJump(boolean value) {
        return value && !FlightVelocity.controls((LocalPlayer)(Object)this);
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "FIELD", opcode = org.objectweb.asm.Opcodes.GETFIELD, target =
            "Lnet/minecraft/client/player/Input;shiftKeyDown:Z"))
    private boolean cornerstone$consumeSneak(boolean value) {
        return value && !FlightVelocity.controls((LocalPlayer)(Object)this);
    }

    @ModifyExpressionValue(method = {"aiStep", "sendPosition"}, at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/player/LocalPlayer;isShiftKeyDown()Z"))
    private boolean cornerstone$noVanillaCrouch(boolean value) {
        return value && !FlightVelocity.controls((LocalPlayer)(Object)this);
    }

    @ModifyArg(method = "aiStep", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/player/LocalPlayer;setSprinting(Z)V"), index = 0)
    private boolean cornerstone$noVanillaSprint(boolean value) {
        return value && !FlightVelocity.controls((LocalPlayer)(Object)this);
    }

    @ModifyExpressionValue(method = "serverAiStep", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
    private float cornerstone$swayPitch(float value) {
        return FlightVelocity.controls((LocalPlayer)(Object)this) ? HandSway.pitch(1) : value;
    }

    @ModifyExpressionValue(method = "serverAiStep", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
    private float cornerstone$swayYaw(float value) {
        return FlightVelocity.controls((LocalPlayer)(Object)this) ? HandSway.yaw(1) : value;
    }
}
