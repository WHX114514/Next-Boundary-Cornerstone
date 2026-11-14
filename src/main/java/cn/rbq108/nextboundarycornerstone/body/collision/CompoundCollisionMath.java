package cn.rbq108.nextboundarycornerstone.body.collision;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

/** Translation clipping and a bounded, shortest-first search for room to rotate. */
public final class CompoundCollisionMath {
    public static final double EPSILON = 1.0e-7;
    private static final int MAX_SEARCH_NODES = 128;

    private CompoundCollisionMath() {}

    public static AABB envelope(List<AABB> boxes) {
        AABB envelope = boxes.getFirst();
        for (int i = 1; i < boxes.size(); i++) envelope = envelope.minmax(boxes.get(i));
        return new AABB(envelope.minX, envelope.minY, envelope.minZ, envelope.maxX, envelope.maxY, envelope.maxZ);
    }

    public static Vec3 clip(List<AABB> bodies, List<VoxelShape> obstacles, Vec3 requested) {
        Vec3 offset = Vec3.ZERO;
        double y = axis(Direction.Axis.Y, bodies, obstacles, offset, requested.y);
        offset = offset.add(0, y, 0);
        double x, z;
        // Same axis ordering as Entity.collideWithShapes, with one shared limit for all pieces.
        if (Math.abs(requested.x) < Math.abs(requested.z)) {
            z = axis(Direction.Axis.Z, bodies, obstacles, offset, requested.z);
            offset = offset.add(0, 0, z);
            x = axis(Direction.Axis.X, bodies, obstacles, offset, requested.x);
        } else {
            x = axis(Direction.Axis.X, bodies, obstacles, offset, requested.x);
            offset = offset.add(x, 0, 0);
            z = axis(Direction.Axis.Z, bodies, obstacles, offset, requested.z);
        }
        return new Vec3(x, y, z);
    }

    private static double axis(Direction.Axis axis, List<AABB> bodies, List<VoxelShape> obstacles, Vec3 offset, double amount) {
        if (amount == 0) return 0;
        for (AABB body : bodies) {
            amount = Shapes.collide(axis, body.move(offset), obstacles, amount);
            if (Math.abs(amount) < EPSILON) return 0;
        }
        return amount;
    }

    public static boolean overlaps(AABB a, AABB b) {
        return Math.min(a.maxX, b.maxX) - Math.max(a.minX, b.minX) > EPSILON
                && Math.min(a.maxY, b.maxY) - Math.max(a.minY, b.minY) > EPSILON
                && Math.min(a.maxZ, b.maxZ) - Math.max(a.minZ, b.minZ) > EPSILON;
    }

    public static boolean clear(List<AABB> bodies, List<AABB> obstacles, Vec3 offset) {
        return firstOverlap(bodies, obstacles, offset) == null;
    }

    private record Overlap(AABB body, AABB obstacle) {}
    private record Cell(long x, long y, long z) {
        static Cell of(Vec3 p) { return new Cell(Math.round(p.x * 1.0e7), Math.round(p.y * 1.0e7), Math.round(p.z * 1.0e7)); }
    }

    private static Overlap firstOverlap(List<AABB> bodies, List<AABB> obstacles, Vec3 offset) {
        for (AABB original : bodies) {
            AABB body = original.move(offset);
            for (AABB obstacle : obstacles) if (overlaps(body, obstacle)) return new Overlap(body, obstacle);
        }
        return null;
    }

    /** Both the whole rotation sweep and the translation to its new pivot must be unobstructed. */
    public static Optional<Vec3> findRotationOffset(List<AABB> previous, List<AABB> sweep,
                                                   List<VoxelShape> shapes, List<AABB> obstacles, double maximum) {
        var queue = new PriorityQueue<Vec3>(Comparator.comparingDouble(Vec3::lengthSqr)
                .thenComparingDouble(p -> p.x).thenComparingDouble(p -> p.y).thenComparingDouble(p -> p.z));
        Set<Cell> visited = new HashSet<>();
        queue.add(Vec3.ZERO);
        int nodes = 0;
        while (!queue.isEmpty() && nodes++ < MAX_SEARCH_NODES) {
            Vec3 shift = queue.remove();
            if (!visited.add(Cell.of(shift))) continue;
            if (clip(previous, shapes, shift).distanceToSqr(shift) > 1.0e-12) continue;
            Overlap collision = firstOverlap(sweep, obstacles, shift);
            if (collision == null) return Optional.of(shift);
            AABB body = collision.body(), obstacle = collision.obstacle();
            offer(queue, shift.add(obstacle.minX - body.maxX - EPSILON, 0, 0), maximum);
            offer(queue, shift.add(obstacle.maxX - body.minX + EPSILON, 0, 0), maximum);
            offer(queue, shift.add(0, obstacle.minY - body.maxY - EPSILON, 0), maximum);
            offer(queue, shift.add(0, obstacle.maxY - body.minY + EPSILON, 0), maximum);
            offer(queue, shift.add(0, 0, obstacle.minZ - body.maxZ - EPSILON), maximum);
            offer(queue, shift.add(0, 0, obstacle.maxZ - body.minZ + EPSILON), maximum);
        }
        return Optional.empty();
    }

    private static void offer(PriorityQueue<Vec3> queue, Vec3 value, double maximum) {
        if (value.lengthSqr() <= maximum * maximum + EPSILON * EPSILON) queue.add(value);
    }
}
