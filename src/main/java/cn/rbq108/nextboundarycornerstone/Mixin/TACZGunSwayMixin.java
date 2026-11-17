package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxes;
import cn.rbq108.nextboundarycornerstone.camera.HandSway;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** TaCZ compares these angles to screen-axis bob, not the flight's world Euler angles. */
@Pseudo
@Mixin(targets = {"com.tacz.guns.client.renderer.item.AnimateGeoItemRenderer",
        "com.tacz.guns.client.renderer.item.GunItemRendererWrapper"}, remap = false)
public abstract class TACZGunSwayMixin {
    @WrapOperation(method = {"renderFirstPerson", "lambda$renderFirstPerson$5"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getViewXRot(F)F", remap = true))
    private float cornerstone$screenPitch(LocalPlayer player, float partialTick, Operation<Float> original) {
        // LocalPlayer's pitch getter uses the current angle, even at partial ticks.
        return SegmentedHitboxes.enabled(player) ? HandSway.pitch(1) : original.call(player, partialTick);
    }

    @WrapOperation(method = {"renderFirstPerson", "lambda$renderFirstPerson$5"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getViewYRot(F)F", remap = true))
    private float cornerstone$screenYaw(LocalPlayer player, float partialTick, Operation<Float> original) {
        return SegmentedHitboxes.enabled(player) ? HandSway.yaw(player.isPassenger() ? partialTick : 1)
                : original.call(player, partialTick);
    }
}
