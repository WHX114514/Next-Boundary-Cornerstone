package cn.rbq108.nextboundarycornerstone.motion;

import net.minecraft.core.Direction;
import org.joml.Vector3f;

final class SurfaceAdhesionMath {
    record Settings(float strengthRatio, float normalDamping, int stopTicks, float sprintMultiplier) {}

    private SurfaceAdhesionMath() {}

    static void apply(Vector3f velocity, Direction normal, Vector3f worldIntent, Vector3f propulsionDelta,
                      float maxAcceleration, float weight, Settings settings) {
        if (normal == null || maxAcceleration <= 0 || weight <= 0) return;

        float normalVelocity = SurfaceGeometry.dot(velocity, normal);
        float normalIntent = SurfaceGeometry.dot(worldIntent, normal);
        float attraction = maxAcceleration * settings.strengthRatio() * weight;
        // No approach damping: only outward motion is resisted after a real contact.
        float resistance = attraction + Math.max(0, normalVelocity) * settings.normalDamping() * weight;
        float limit = maxAcceleration * 0.9f;
        if (normalIntent > 0.00001f) {
            // Use the acceleration the target-velocity controller actually supplied this tick,
            // including oblique input and its diminishing acceleration near the target speed.
            limit = Math.min(limit, Math.max(0, SurfaceGeometry.dot(propulsionDelta, normal)) * 0.9f);
        }
        SurfaceGeometry.addNormal(velocity, normal, -Math.min(resistance, limit));

        Vector3f tangentIntent = new Vector3f(worldIntent);
        SurfaceGeometry.addNormal(tangentIntent, normal, -normalIntent);
        if (tangentIntent.lengthSquared() <= 1.0e-10f) {
            Vector3f tangentVelocity = new Vector3f(velocity);
            SurfaceGeometry.addNormal(tangentVelocity, normal, -SurfaceGeometry.dot(velocity, normal));
            // Remove 99% in the configured time, with a finite deceleration budget.
            float fraction = (float) (1 - Math.pow(0.01, 1.0 / settings.stopTicks())) * weight;
            Vector3f braking = tangentVelocity.mul(-fraction);
            float budget = maxAcceleration * weight;
            if (braking.lengthSquared() > budget * budget) braking.normalize().mul(budget);
            velocity.add(braking);
            Vector3f remaining = new Vector3f(velocity);
            SurfaceGeometry.addNormal(remaining, normal, -SurfaceGeometry.dot(velocity, normal));
            if (remaining.lengthSquared() < 0.005f * 0.005f) velocity.sub(remaining);
        }
    }
}
