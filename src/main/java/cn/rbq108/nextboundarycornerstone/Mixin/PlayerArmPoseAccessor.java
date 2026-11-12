package cn.rbq108.nextboundarycornerstone.Mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PlayerRenderer.class)
public interface PlayerArmPoseAccessor {
    @Invoker("getArmPose")
    static HumanoidModel.ArmPose nextboundary$armPose(AbstractClientPlayer player, InteractionHand hand) {
        throw new AssertionError();
    }
}
