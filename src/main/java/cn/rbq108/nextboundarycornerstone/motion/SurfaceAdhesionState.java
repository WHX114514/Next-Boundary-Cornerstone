package cn.rbq108.nextboundarycornerstone.motion;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import java.util.Map;

/** Session-local contact memory; no state is shared with a server player. */
final class SurfaceAdhesionState {
    private Direction normal;
    private double plane;
    private int missingTicks;
    private int cooldown;

    Direction update(AABB body, Map<Direction, SurfaceGeometry.Contact> contacts, Vector3f velocity, boolean sprinting) {
        if (cooldown > 0) {
            cooldown--;
            return null;
        }
        if (sprinting) {
            clearSurface();
            return null;
        }
        if (normal != null) {
            // Keep the original plane even when no shape is returned: a fast outward step must
            // detach immediately rather than spend the two missing-support ticks still attached.
            if (SurfaceGeometry.separation(body, normal, plane) > SurfaceGeometry.RELEASE_DISTANCE) {
                clearSurface();
                cooldown = 5;
                return null;
            }
            SurfaceGeometry.Contact support = contacts.get(normal);
            if (support != null) {
                plane = support.plane();
                missingTicks = 0;
                return normal;
            }
            if (++missingTicks < 2) return null; // Keep memory, but apply no force over a gap.
            clearSurface();
        }

        SurfaceGeometry.Contact best = null;
        float bestImpact = -Float.MAX_VALUE;
        for (SurfaceGeometry.Contact contact : contacts.values()) {
            if (contact.gap() > SurfaceGeometry.CONTACT_EPSILON) continue;
            float impact = -SurfaceGeometry.dot(velocity, contact.normal());
            if (best == null || impact > bestImpact) {
                best = contact;
                bestImpact = impact;
            }
        }
        if (best != null) {
            normal = best.normal();
            plane = best.plane();
        }
        return normal;
    }

    Direction rememberedNormal() {
        return normal;
    }

    void reset() {
        clearSurface();
        cooldown = 0;
    }

    private void clearSurface() {
        normal = null;
        missingTicks = 0;
    }
}
