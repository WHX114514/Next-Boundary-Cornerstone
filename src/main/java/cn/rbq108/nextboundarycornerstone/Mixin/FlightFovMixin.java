package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.ModCompatible;
import cn.rbq108.nextboundarycornerstone.motion.FlightVelocity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Preserve the existing flight FOV without advertising creative flight to other mods. */
@Mixin(AbstractClientPlayer.class)
public abstract class FlightFovMixin {
    @ModifyExpressionValue(method = "getFieldOfViewModifier", at = @At(value = "FIELD", target =
            "Lnet/minecraft/world/entity/player/Abilities;flying:Z"))
    private boolean cornerstone$flightFov(boolean flying) {
        var player=(AbstractClientPlayer)(Object)this;
        return flying || FlightVelocity.controls(player) && !ModCompatible.isGrabbingAeronauticsHandle(player);
    }
}
