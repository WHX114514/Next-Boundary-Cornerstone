package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.body.ClientSegmentedHitboxes;
import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxes;
import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class SegmentedRemotePoseMixin<T extends LivingEntity> {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void nextboundary$pose(T entity, float swing, float amount, float age, float yaw, float pitch, CallbackInfo ci) {
        if (!(entity instanceof Player player) || ClientSegmentedHitboxes.isSampling() || !SegmentedHitboxes.enabled(player)) return;
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        var pose = player.getData(main.PLAYER_ROTATION).renderHitboxPose(partialTick);
        if (pose == null) return;
        PlayerModel<?> model = (PlayerModel<?>) (Object) this;
        ClientSegmentedHitboxes.apply(model, pose);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftPants.copyFrom(model.leftLeg);
        model.rightPants.copyFrom(model.rightLeg);
        model.jacket.copyFrom(model.body);
    }
}
