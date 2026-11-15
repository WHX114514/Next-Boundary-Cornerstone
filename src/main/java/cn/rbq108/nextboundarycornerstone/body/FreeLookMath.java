package cn.rbq108.nextboundarycornerstone.body;

import org.joml.Quaternionf;

/** Shared ordinary-free-look limits; fixed-head orbit deliberately bypasses them. */
public final class FreeLookMath {
    public static final float VIEW_YAW = 110, HEAD_YAW = 90, PITCH = 90, ROLL = 60;
    private FreeLookMath() {}

    public record Angles(float yaw, float pitch, float roll) {
        public Angles limited(boolean firstPerson) {
            return new Angles(limitYaw(yaw, firstPerson), clamp(pitch, PITCH), clamp(roll, ROLL));
        }
        public Angles turn(float deltaYaw, float deltaPitch, float deltaRoll, boolean firstPerson) {
            return new Angles(yaw + deltaYaw, pitch + deltaPitch, roll + deltaRoll).limited(firstPerson);
        }
        public Quaternionf rotation() { return FreeLookMath.rotation(yaw, pitch, roll); }
        public Quaternionf headRotation() {
            return FreeLookMath.rotation(clamp(yaw, HEAD_YAW), clamp(pitch, PITCH), clamp(roll, ROLL));
        }
    }

    public static float limitYaw(float yaw, boolean firstPerson) {
        return firstPerson ? clamp(yaw, VIEW_YAW) : wrap(yaw);
    }
    public static float clamp(float value, float limit) { return Math.max(-limit, Math.min(limit, value)); }
    private static float wrap(float value) {
        value %= 360;
        if (value >= 180) value -= 360;
        if (value < -180) value += 360;
        return value;
    }
    private static Quaternionf rotation(float yaw, float pitch, float roll) {
        return new Quaternionf().rotationYXZ((float) Math.toRadians(-yaw),
                (float) Math.toRadians(pitch), (float) Math.toRadians(roll));
    }

    public static Angles relative(Quaternionf body, Quaternionf view) {
        Quaternionf q = new Quaternionf(body).invert().mul(view).normalize();
        double x = q.x, y = q.y, z = q.z, w = q.w;
        double length = x * x + y * y + z * z + w * w;
        double sinPitch = Math.max(-1, Math.min(1, 2 * (w * x - y * z) / length));
        double pitch = Math.asin(sinPitch), yaw, roll;
        if (Math.abs(sinPitch) > .9999999) {
            // At vertical pitch yaw and roll share one degree of freedom. Keep an
            // equivalent orientation rather than producing atan2(0,0) or NaN.
            yaw = 2 * Math.atan2(y, w);
            roll = 0;
        } else {
            yaw = Math.atan2(2 * (w * y + x * z), length - 2 * (x * x + y * y));
            roll = Math.atan2(2 * (w * z + x * y), length - 2 * (x * x + z * z));
        }
        return new Angles(wrap((float) Math.toDegrees(-yaw)), (float) Math.toDegrees(pitch),
                wrap((float) Math.toDegrees(roll)));
    }

    public static Quaternionf headRelative(SegmentedHitboxPose pose) {
        var head = new Quaternionf(pose.joints().get(0).rotation());
        if (pose.swimming()) head.rotateX((float) Math.PI / 2);
        var basis = new Quaternionf().rotateY((float) Math.PI).rotateZ((float) Math.PI);
        return new Quaternionf(basis).mul(head).mul(basis.conjugate()).normalize();
    }
}
