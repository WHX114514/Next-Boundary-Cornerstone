package cn.rbq108.nextboundarycornerstone.motion;

import org.joml.Vector3f;

/** Existing target-speed propulsion and manual braking, without a Minecraft dependency. */
final class FlightVelocityMath {
    private FlightVelocityMath() {}

    static Vector3f advance(Vector3f velocity, Vector3f target, boolean hasInput, boolean shouldBrake,
                            float maxAcceleration, float brakeRatio) {
        Vector3f before = new Vector3f(velocity);
        if (!hasInput && shouldBrake) {
            velocity.lerp(new Vector3f(), Math.min(brakeRatio * 0.1f, 0.95f));
            if (velocity.lengthSquared() < 0.005f * 0.005f) velocity.zero();
        } else if (hasInput) {
            Vector3f acceleration = new Vector3f(target).sub(velocity);
            if (acceleration.lengthSquared() > maxAcceleration * maxAcceleration) {
                acceleration.normalize().mul(maxAcceleration);
            }
            velocity.add(acceleration);
        }
        return new Vector3f(velocity).sub(before);
    }
}
