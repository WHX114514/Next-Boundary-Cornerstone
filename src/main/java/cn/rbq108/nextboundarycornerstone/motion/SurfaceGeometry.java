package cn.rbq108.nextboundarycornerstone.motion;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.List;

/** Geometry shared by the live block-shape probe and the numerical regression tests. */
final class SurfaceGeometry {
    static final double CONTACT_EPSILON = 0.0001;
    static final double RELEASE_DISTANCE = 0.35;

    record Contact(Direction normal, double gap, double plane) {}

    private SurfaceGeometry() {}

    static Map<Direction, Contact> findContacts(AABB body, Iterable<AABB> shapes) {
        Map<Direction, Contact> result = new EnumMap<>(Direction.class);
        for (AABB shape : shapes) {
            if (overlap(body.minY, body.maxY, shape.minY, shape.maxY)
                    && overlap(body.minZ, body.maxZ, shape.minZ, shape.maxZ)) {
                offer(result, Direction.EAST, body.minX - shape.maxX, shape.maxX);
                offer(result, Direction.WEST, shape.minX - body.maxX, shape.minX);
            }
            if (overlap(body.minX, body.maxX, shape.minX, shape.maxX)
                    && overlap(body.minZ, body.maxZ, shape.minZ, shape.maxZ)) {
                offer(result, Direction.UP, body.minY - shape.maxY, shape.maxY);
                offer(result, Direction.DOWN, shape.minY - body.maxY, shape.minY);
            }
            if (overlap(body.minX, body.maxX, shape.minX, shape.maxX)
                    && overlap(body.minY, body.maxY, shape.minY, shape.maxY)) {
                offer(result, Direction.SOUTH, body.minZ - shape.maxZ, shape.maxZ);
                offer(result, Direction.NORTH, shape.minZ - body.maxZ, shape.minZ);
            }
        }
        return result;
    }

    static Map<Direction, Contact> findContacts(List<AABB> bodies, List<AABB> shapes) {
        Map<Direction, Contact> result = new EnumMap<>(Direction.class);
        for (AABB body : bodies) {
            for (Contact contact : findContacts(body, shapes).values()) {
                Contact previous = result.get(contact.normal());
                if (previous == null || contact.gap() < previous.gap()) result.put(contact.normal(), contact);
            }
        }
        return result;
    }

    private static boolean overlap(double aMin, double aMax, double bMin, double bMax) {
        // An edge/point is not a supporting face. This also avoids stair internal-face captures.
        return Math.min(aMax, bMax) - Math.max(aMin, bMin) > CONTACT_EPSILON;
    }

    private static void offer(Map<Direction, Contact> contacts, Direction normal, double gap, double plane) {
        if (gap < -CONTACT_EPSILON || gap > RELEASE_DISTANCE + CONTACT_EPSILON) return;
        Contact previous = contacts.get(normal);
        if (previous == null || gap < previous.gap()) {
            contacts.put(normal, new Contact(normal, Math.max(0, gap), plane));
        }
    }

    static float dot(Vector3f vector, Direction normal) {
        return vector.x * normal.getStepX() + vector.y * normal.getStepY() + vector.z * normal.getStepZ();
    }

    static void addNormal(Vector3f vector, Direction normal, float amount) {
        vector.add(normal.getStepX() * amount, normal.getStepY() * amount, normal.getStepZ() * amount);
    }

    static void removeInwardVelocity(Vector3f velocity, Map<Direction, Contact> contacts) {
        for (Contact contact : contacts.values()) {
            float inward = dot(velocity, contact.normal());
            if (contact.gap() <= CONTACT_EPSILON && inward < 0) {
                addNormal(velocity, contact.normal(), -inward);
            }
        }
    }

    static EnumSet<Direction> blockedNormals(Vec3 requested, Vec3 resolved) {
        EnumSet<Direction> normals = EnumSet.noneOf(Direction.class);
        if (clipped(requested.x, resolved.x)) normals.add(requested.x > 0 ? Direction.WEST : Direction.EAST);
        if (clipped(requested.y, resolved.y)) normals.add(requested.y > 0 ? Direction.DOWN : Direction.UP);
        if (clipped(requested.z, resolved.z)) normals.add(requested.z > 0 ? Direction.NORTH : Direction.SOUTH);
        return normals;
    }

    private static boolean clipped(double requested, double resolved) {
        return requested > 1.0e-7 && resolved < requested - 1.0e-7
                || requested < -1.0e-7 && resolved > requested + 1.0e-7;
    }

    static void removeInwardVelocity(Vector3f velocity, Iterable<Direction> blockedNormals) {
        for (Direction normal : blockedNormals) {
            float component = dot(velocity, normal);
            if (component < 0) addNormal(velocity, normal, -component);
        }
    }

    static double separation(AABB body, Direction normal, double plane) {
        return switch (normal) {
            case EAST -> body.minX - plane;
            case WEST -> plane - body.maxX;
            case UP -> body.minY - plane;
            case DOWN -> plane - body.maxY;
            case SOUTH -> body.minZ - plane;
            case NORTH -> plane - body.maxZ;
        };
    }
}
