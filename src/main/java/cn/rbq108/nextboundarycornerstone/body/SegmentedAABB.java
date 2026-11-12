package cn.rbq108.nextboundarycornerstone.body;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/** Used only during hit queries: the envelope gathers candidates, the pieces decide hits. */
public final class SegmentedAABB extends AABB {
    private final List<AABB> pieces;
    private double cachedInflation;
    private AABB inflated;

    public SegmentedAABB(List<AABB> pieces) {
        this(pieces, envelope(pieces));
    }

    private SegmentedAABB(List<AABB> pieces, AABB envelope) {
        super(envelope.minX, envelope.minY, envelope.minZ, envelope.maxX, envelope.maxY, envelope.maxZ);
        this.pieces = List.copyOf(pieces);
    }

    private static AABB envelope(List<AABB> pieces) {
        if (pieces.isEmpty()) throw new IllegalArgumentException("Empty hitbox");
        AABB box = pieces.getFirst();
        for (int i = 1; i < pieces.size(); i++) box = box.minmax(pieces.get(i));
        return box;
    }

    @Override
    public AABB inflate(double amount) {
        if (amount == 0) return this;
        if (inflated == null || cachedInflation != amount) {
            cachedInflation = amount;
            inflated = new SegmentedAABB(pieces.stream().map(box -> box.inflate(amount)).toList());
        }
        return inflated;
    }

    @Override
    public boolean contains(double x, double y, double z) {
        for (AABB box : pieces) if (box.contains(x, y, z)) return true;
        return false;
    }

    @Override
    public Optional<Vec3> clip(Vec3 start, Vec3 end) {
        Vec3 closest = null;
        double distance = Double.POSITIVE_INFINITY;
        for (AABB box : pieces) {
            if (box.contains(start)) return Optional.of(start);
            Optional<Vec3> hit = box.clip(start, end);
            if (hit.isPresent()) {
                double d = start.distanceToSqr(hit.get());
                if (d < distance) { closest = hit.get(); distance = d; }
            }
        }
        return Optional.ofNullable(closest);
    }

    @Override
    public double distanceToSqr(Vec3 point) {
        double distance = Double.POSITIVE_INFINITY;
        for (AABB box : pieces) distance = Math.min(distance, box.distanceToSqr(point));
        return distance;
    }

    public List<AABB> pieces() { return pieces; }
}
