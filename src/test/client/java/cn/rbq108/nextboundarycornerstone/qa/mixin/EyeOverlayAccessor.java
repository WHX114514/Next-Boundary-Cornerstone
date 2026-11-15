package cn.rbq108.nextboundarycornerstone.qa.mixin;

import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ScreenEffectRenderer.class)
public interface EyeOverlayAccessor {
    @Invoker("getOverlayBlock")
    static Pair<BlockState, BlockPos> overlay(Player player) { throw new AssertionError(); }
}
