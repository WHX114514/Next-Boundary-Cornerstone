package cn.rbq108.nextboundarycornerstone.body;

import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Eye-height anchor inside the head, shared by rendering and authoritative ray origins. */
public final class BodyEyes {
    // Standard face eye row, halfway through its pixel. Keeping z at the head's
    // horizontal center leaves room for the near plane when the face touches a wall.
    public static final float MODEL_EYE_Y = -3.5f / 16;
    private BodyEyes() {}

    public static Vec3 offset(SegmentedHitboxPose pose, float height, float scale) {
        var head = pose.joints().get(0);
        var transform = SegmentedHitboxGeometry.root(pose, height, scale)
                .translate(head.x() / 16, head.y() / 16, head.z() / 16).rotate(head.rotation());
        return new Vec3(transform.transformPosition(new Vector3f(0, MODEL_EYE_Y, 0)));
    }

    public static float neutralHeight(float height, float scale) {
        return height * .5f - .9f + scale * .9375f * (1.501f - MODEL_EYE_Y);
    }

    public static Vec3 position(Player player) {
        return player.position().add(cn.rbq108.nextboundarycornerstone.attachment.PlayerRotationAttachment.get(player).eyeOffset(player));
    }

    public static Vec3 position(Player player, float partialTick) {
        return player.getPosition(partialTick).add(cn.rbq108.nextboundarycornerstone.attachment.PlayerRotationAttachment.get(player).renderEyeOffset(player, partialTick));
    }
}
