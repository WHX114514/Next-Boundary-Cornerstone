package cn.rbq108.nextboundarycornerstone.body;

import net.minecraft.network.FriendlyByteBuf;
import org.joml.Quaternionf;

import java.util.List;

/** Tick snapshot of the model skeleton, independent of client/render classes. */
public record SegmentedHitboxPose(Quaternionf body, float swim, boolean slim, List<Joint> joints) {
    public record Joint(float x, float y, float z, Quaternionf rotation) {
        public Joint {
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)
                    || Math.abs(x) > 8 || y < -2 || y > 16 || Math.abs(z) > 8) {
                throw new IllegalArgumentException("Invalid hitbox joint pivot");
            }
            rotation = normalized(rotation);
        }
    }

    public SegmentedHitboxPose {
        body = normalized(body);
        if (!Float.isFinite(swim) || swim < 0 || swim > 1 || joints.size() != 6) {
            throw new IllegalArgumentException("Invalid hitbox pose");
        }
        joints = List.copyOf(joints);
    }

    public static Quaternionf normalized(Quaternionf q) {
        float length = q.lengthSquared();
        if (!Float.isFinite(length) || length < 1.0e-8f) {
            throw new IllegalArgumentException("Invalid hitbox rotation");
        }
        return new Quaternionf(q).normalize();
    }

    public static SegmentedHitboxPose standing(Quaternionf body) {
        return new SegmentedHitboxPose(body, 0, false, List.of(
                joint(0, 0), joint(0, 0), joint(5, 2), joint(-5, 2), joint(1.9f, 12), joint(-1.9f, 12)));
    }

    private static Joint joint(float x, float y) {
        return new Joint(x, y, 0, new Quaternionf());
    }

    public static SegmentedHitboxPose interpolate(SegmentedHitboxPose from, SegmentedHitboxPose to, float t) {
        t = Math.max(0, Math.min(1, t));
        var joints = new java.util.ArrayList<Joint>(6);
        for (int i = 0; i < 6; i++) {
            Joint a = from.joints().get(i), b = to.joints().get(i);
            joints.add(new Joint(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t,
                    new Quaternionf(a.rotation).slerp(b.rotation, t)));
        }
        return new SegmentedHitboxPose(new Quaternionf(from.body).slerp(to.body, t),
                from.swim + (to.swim - from.swim) * t, to.slim, joints);
    }

    public void write(FriendlyByteBuf buf) {
        writeQuaternion(buf, body);
        buf.writeFloat(swim);
        buf.writeBoolean(slim);
        for (Joint joint : joints) {
            buf.writeFloat(joint.x());
            buf.writeFloat(joint.y());
            buf.writeFloat(joint.z());
            writeQuaternion(buf, joint.rotation());
        }
    }

    public static SegmentedHitboxPose read(FriendlyByteBuf buf) {
        Quaternionf body = readQuaternion(buf);
        float swim = buf.readFloat();
        boolean slim = buf.readBoolean();
        var joints = new java.util.ArrayList<Joint>(6);
        for (int i = 0; i < 6; i++) {
            joints.add(new Joint(buf.readFloat(), buf.readFloat(), buf.readFloat(), readQuaternion(buf)));
        }
        return new SegmentedHitboxPose(body, swim, slim, joints);
    }

    private static Quaternionf readQuaternion(FriendlyByteBuf buf) {
        return new Quaternionf(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    private static void writeQuaternion(FriendlyByteBuf buf, Quaternionf q) {
        buf.writeFloat(q.x); buf.writeFloat(q.y); buf.writeFloat(q.z); buf.writeFloat(q.w);
    }
}
