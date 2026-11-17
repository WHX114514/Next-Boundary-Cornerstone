package cn.rbq108.nextboundarycornerstone.camera;

import org.joml.Quaternionf;

/** Render-only angles. Never feed their pole representation back into flight input. */
public final class CameraRotation {
    private CameraRotation() {}

    public record Angles(float yaw, float pitch, float roll) {}

    public static Quaternionf rotation(float yaw, float pitch, float roll) {
        double x = Math.toRadians(pitch)/2, y = Math.toRadians(-yaw)/2, z = Math.toRadians(roll)/2;
        double sx = Math.sin(x), cx = Math.cos(x), sy = Math.sin(y), cy = Math.cos(y), sz = Math.sin(z), cz = Math.cos(z);
        return new Quaternionf((float)(cy*sx*cz + sy*cx*sz), (float)(sy*cx*cz - cy*sx*sz),
                (float)(cy*cx*sz - sy*sx*cz), (float)(cy*cx*cz + sy*sx*sz));
    }

    public static Angles angles(Quaternionf q) {
        double x = q.x, y = q.y, z = q.z, w = q.w;
        double n = x*x + y*y + z*z + w*w;
        double fx = 2*(x*z + y*w)/n, fy = 2*(y*z - x*w)/n;
        double fz = (w*w - x*x - y*y + z*z)/n;
        double horizontal = Math.hypot(fx, fz);
        double yaw = horizontal > 1e-12 ? -Math.atan2(fx, fz) : 0;
        double pitch = Math.atan2(-fy, horizontal);
        // Project the actual up onto the yaw/pitch frame. At the pole any yaw is
        // valid, provided roll compensates; the player's unrelated yaw is never used.
        double ux = 2*(x*y - z*w)/n, uy = (w*w - x*x + y*y - z*z)/n, uz = 2*(y*z + x*w)/n;
        double sy = Math.sin(yaw), cy = Math.cos(yaw), sp = Math.sin(pitch), cp = Math.cos(pitch);
        double roll = Math.atan2(-(ux*cy + uz*sy), -ux*sy*sp + uy*cp + uz*cy*sp);
        return new Angles((float)Math.toDegrees(yaw), (float)Math.toDegrees(pitch), (float)Math.toDegrees(roll));
    }
}
