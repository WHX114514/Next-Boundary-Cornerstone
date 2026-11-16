package cn.rbq108.nextboundarycornerstone.body;

import org.joml.Quaternionf;
import org.joml.Vector3f;

final class ModelPoseMath {
    private ModelPoseMath() {}

    /** Inverse of ModelPart's rotationZYX(z, y, x), including vertical-Y poses.
     * JOML 1.10.5 getEulerAnglesZYX uses the wrong sign for y*y in its X denominator. */
    static Vector3f eulerZYX(Quaternionf q) {
        double x = q.x, y = q.y, z = q.z, w = q.w;
        double length = x*x + y*y + z*z + w*w;
        double m00 = length - 2*(y*y + z*z), m10 = 2*(x*y + w*z);
        double horizontal = Math.hypot(m00, m10);
        double pitch = Math.atan2(2*(w*y - x*z), horizontal);
        double roll, yaw;
        if (horizontal > 1.0e-6 * length) {
            roll = Math.atan2(2*(w*x + y*z), length - 2*(x*x + y*y));
            yaw = Math.atan2(m10, m00);
        } else {
            // At Y=+/-90, X/Z share one degree of freedom. Choose X=0
            // and preserve the equivalent combined Z rotation.
            roll = 0;
            yaw = Math.atan2(2*(w*z - x*y), length - 2*(x*x + z*z));
        }
        return new Vector3f((float) roll, (float) pitch, (float) yaw);
    }
}
