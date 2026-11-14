package cn.rbq108.nextboundarycornerstone.body;

import cn.rbq108.nextboundarycornerstone.Mixin.PlayerArmPoseAccessor;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/** Sample vanilla animation once per tick, even in first person or when nobody renders the player. */
public final class ClientSegmentedHitboxes {
    private static final PlayerModel<AbstractClientPlayer> NORMAL = create(false);
    private static final PlayerModel<AbstractClientPlayer> SLIM = create(true);
    private static boolean sampling;

    private ClientSegmentedHitboxes() {}

    private static PlayerModel<AbstractClientPlayer> create(boolean slim) {
        return new PlayerModel<>(PlayerModel.createMesh(CubeDeformation.NONE, slim).getRoot().bake(64, 64), slim);
    }

    public static SegmentedHitboxPose sample(AbstractClientPlayer player) {
        boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
        PlayerModel<AbstractClientPlayer> model = slim ? SLIM : NORMAL;
        HumanoidModel.ArmPose main = PlayerArmPoseAccessor.nextboundary$armPose(player, InteractionHand.MAIN_HAND);
        HumanoidModel.ArmPose off = PlayerArmPoseAccessor.nextboundary$armPose(player, InteractionHand.OFF_HAND);
        if (main.isTwoHanded()) off = player.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        model.rightArmPose = player.getMainArm() == HumanoidArm.RIGHT ? main : off;
        model.leftArmPose = player.getMainArm() == HumanoidArm.RIGHT ? off : main;
        model.attackTime = player.getAttackAnim(1);
        model.riding = player.isPassenger() && player.getVehicle() != null && player.getVehicle().shouldRiderSit();
        model.crouching = player.isCrouching();
        sampling = true;
        try {
            model.prepareMobModel(player, player.walkAnimation.position(), player.walkAnimation.speed(), 1);
            model.setupAnim(player, player.walkAnimation.position(), Math.min(1, player.walkAnimation.speed()), player.tickCount, 0, 0);
        } finally {
            sampling = false;
        }

        Quaternionf body = new Quaternionf().rotateY((float) Math.toRadians(-trunk.smoothY))
                .rotateX((float) Math.toRadians(trunk.smoothX)).rotateZ((float) Math.toRadians(trunk.smoothZ));
        return new SegmentedHitboxPose(body, player.getSwimAmount(1), slim, List.of(
                joint(model.head, headRotation(body, 1, player.isSwimming())), joint(model.body),
                joint(model.leftArm), joint(model.rightArm), joint(model.leftLeg), joint(model.rightLeg)), player.hasPose(net.minecraft.world.entity.Pose.SWIMMING));
    }

    public static boolean isSampling() { return sampling; }

    public static Quaternionf headRotation(Quaternionf body, float partialTick, boolean swimming) {
        Quaternionf relative;
        if (GlobalVariables.B_FreeCameraActive || GlobalVariables.headRelQuat.angle() > .001f) {
            relative = new Quaternionf(GlobalVariables.prevHeadRelQuat).slerp(GlobalVariables.headRelQuat, partialTick);
        } else {
            relative = new Quaternionf(body).invert().mul(new Quaternionf(GlobalVariables.prevQuat)
                    .slerp(GlobalVariables.currentQuat, partialTick));
        }
        Quaternionf y180 = new Quaternionf().rotateY((float) Math.PI);
        Quaternionf z180 = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf head = new Quaternionf(z180).invert().mul(new Quaternionf(y180).invert())
                .mul(relative).mul(y180).mul(z180);
        if (swimming) head.rotateX(-(float) Math.PI / 2);
        return head;
    }

    private static SegmentedHitboxPose.Joint joint(ModelPart part) {
        return joint(part, new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot));
    }

    private static SegmentedHitboxPose.Joint joint(ModelPart part, Quaternionf rotation) {
        return new SegmentedHitboxPose.Joint(part.x, part.y, part.z, rotation);
    }

    public static void apply(HumanoidModel<?> model, SegmentedHitboxPose pose) {
        ModelPart[] parts = {model.head, model.body, model.leftArm, model.rightArm, model.leftLeg, model.rightLeg};
        for (int i = 0; i < parts.length; i++) {
            var joint = pose.joints().get(i);
            Vector3f euler = joint.rotation().getEulerAnglesZYX(new Vector3f());
            parts[i].setPos(joint.x(), joint.y(), joint.z());
            parts[i].setRotation(euler.x, euler.y, euler.z);
        }
        model.hat.copyFrom(model.head);
    }
}
