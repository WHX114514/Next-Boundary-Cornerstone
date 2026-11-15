package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.body.EyeBlockChecks;
import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Test occupancy at the rendered eye, not eight corners of the retired 1.2-block cube. */
@Mixin(ScreenEffectRenderer.class)
public abstract class SegmentedEyeOverlayMixin {
    @Inject(method = "getOverlayBlock", at = @At("HEAD"), cancellable = true)
    private static void nextboundary$eyeOverlay(Player player, CallbackInfoReturnable<Pair<BlockState, BlockPos>> cir) {
        if (!SegmentedHitboxes.enabled(player)) return;
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        var eye = camera.isInitialized() && camera.getEntity() == player ? camera.getPosition() : player.getEyePosition();
        var blocking = EyeBlockChecks.viewBlocking(player.level(), eye, player.getScale());
        cir.setReturnValue(blocking == null ? null : Pair.of(blocking.state(), blocking.pos()));
    }
}
