package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxes;
import cn.rbq108.nextboundarycornerstone.camera.CameraRotation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.client.renderer.item.AnimateGeoItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ViewportEvent;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.tacz.guns.client.event.CameraSetupEvent", remap = false)
public abstract class TACZCameraAnimationMixin {
    @WrapOperation(method = "applyLevelCameraAnimation", at = @At(value = "INVOKE", target =
            "Lcom/tacz/guns/client/renderer/item/AnimateGeoItemRenderer;applyLevelCameraAnimation(Lnet/minecraftforge/client/event/ViewportEvent$ComputeCameraAngles;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/player/LocalPlayer;)V"))
    private static void cornerstone$localAnimation(AnimateGeoItemRenderer<?, ?> renderer,
            ViewportEvent.ComputeCameraAngles event, ItemStack stack, LocalPlayer player, Operation<Void> original) {
        if (!GlobalVariables.B_LowGravity || !SegmentedHitboxes.enabled(player) || event.getCamera().getEntity() != player) {
            original.call(renderer, event, stack, player);
            return;
        }
        var base = CameraRotation.rotation(event.getYaw(), event.getPitch(), event.getRoll());
        var upright = new ViewportEvent.ComputeCameraAngles(Minecraft.getInstance().gameRenderer,
                event.getCamera(), event.getPartialTick(), 0, event.getPitch(), 0);
        var uprightBase = CameraRotation.rotation(0, event.getPitch(), 0);
        // Sample TaCZ's original effect once in its supported, unrolled frame.
        original.call(renderer, upright, stack, player);
        var localEffect = uprightBase.invert().mul(CameraRotation.rotation(upright.getYaw(), upright.getPitch(), upright.getRoll()));
        var angles = CameraRotation.angles(new Quaternionf(base).mul(localEffect).normalize());
        event.setYaw(angles.yaw());
        event.setPitch(angles.pitch());
        event.setRoll(angles.roll());
    }
}
