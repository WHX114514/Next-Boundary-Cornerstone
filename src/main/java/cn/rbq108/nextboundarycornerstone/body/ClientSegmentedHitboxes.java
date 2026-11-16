package cn.rbq108.nextboundarycornerstone.body;

import cn.rbq108.nextboundarycornerstone.Mixin.PlayerArmPoseAccessor;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.player.AbstractClientPlayer;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;

/** Sample vanilla animation once per tick, even in first person or when nobody renders the player. */
public final class ClientSegmentedHitboxes {
    private static final PlayerModel<AbstractClientPlayer> NORMAL = create(false);
    private static final PlayerModel<AbstractClientPlayer> SLIM = create(true);
    private static boolean sampling;
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();
    private static long lastWarning = System.nanoTime() - 5_000_000_000L;
    private static final String[] JOINT_NAMES = {"head", "torso", "left_arm", "right_arm", "left_leg", "right_leg"};

    private ClientSegmentedHitboxes() {}

    private static PlayerModel<AbstractClientPlayer> create(boolean slim) {
        return new PlayerModel<>(PlayerModel.createMesh(CubeDeformation.NONE, slim).getRoot().bake(64, 64), slim);
    }

    public static SegmentedHitboxPose sample(AbstractClientPlayer player) {
        boolean slim = player.getModelName().equals("slim");
        PlayerModel<AbstractClientPlayer> model = slim ? SLIM : NORMAL;
        // setupAnim does not reset every pivot/axis. Reused sampling models must
        // not carry a previous animation or skin's transforms into the next tick.
        for (ModelPart part : parts(model)) {
            part.getAllParts().forEach(p -> {
                p.resetPose();
                p.xScale = p.yScale = p.zScale = 1;
            });
        }
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
        var previous = cn.rbq108.nextboundarycornerstone.attachment.PlayerRotationAttachment.get(player).getHitboxPose();
        if (previous != null && previous.slim() != slim) previous = null;
        var parts = parts(model);
        var joints = new ArrayList<SegmentedHitboxPose.Joint>(6);
        for (int i = 0; i < parts.length; i++) {
            var part = parts[i];
            var initial = part.getInitialPose();
            var fallback = previous == null ? new SegmentedHitboxPose.Joint(initial.x, initial.y, initial.z,
                    new Quaternionf().rotationZYX(initial.zRot, initial.yRot, initial.xRot)) : previous.joints().get(i);
            var rotation = i == 0 ? headRotation(body, 1, player.isSwimming())
                    : new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot);
            var joint = SegmentedHitboxPose.sampledJoint(part.x, part.y, part.z, rotation, fallback);
            long now = System.nanoTime();
            if (joint == fallback && now - lastWarning >= 5_000_000_000L) {
                lastWarning = now;
                LOG.warn("Invalid sampled joint {}; pivot=({}, {}, {}), rotation={}, item={}, mods={}",
                        JOINT_NAMES[i], part.x, part.y, part.z, rotation, player.getMainHandItem(),
                        net.minecraftforge.fml.ModList.get().getMods().stream()
                                .filter(m -> m.getModId().equals("tacz") || m.getModId().equals("playeranimator"))
                                .map(m -> m.getModId() + "@" + m.getVersion()).toList());
            }
            joints.add(joint);
        }
        return new SegmentedHitboxPose(body, player.getSwimAmount(1), slim, joints,
                player.hasPose(net.minecraft.world.entity.Pose.SWIMMING));
    }

    public static boolean isSampling() { return sampling; }

    public static Quaternionf headRotation(Quaternionf body, float partialTick, boolean swimming) {
        Quaternionf relative;
        if (cn.rbq108.nextboundarycornerstone.camera.CollisionFreeLook.isLimiting()) {
            relative = FreeLookMath.relative(body, GlobalVariables.currentQuat).headRotation();
        } else if (GlobalVariables.B_FreeCameraActive || GlobalVariables.headRelQuat.angle() > .001f) {
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

    private static ModelPart[] parts(HumanoidModel<?> model) {
        return new ModelPart[] {model.head, model.body, model.leftArm, model.rightArm, model.leftLeg, model.rightLeg};
    }

    public static void apply(HumanoidModel<?> model, SegmentedHitboxPose pose) {
        ModelPart[] parts = parts(model);
        for (int i = 0; i < parts.length; i++) {
            var joint = pose.joints().get(i);
            Vector3f euler = ModelPoseMath.eulerZYX(joint.rotation());
            parts[i].setPos(joint.x(), joint.y(), joint.z());
            parts[i].setRotation(euler.x, euler.y, euler.z);
        }
        model.hat.copyFrom(model.head);
    }
}
