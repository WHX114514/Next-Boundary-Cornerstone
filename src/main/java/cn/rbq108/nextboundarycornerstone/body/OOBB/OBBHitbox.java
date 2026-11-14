package cn.rbq108.nextboundarycornerstone.body.OOBB;

import cn.rbq108.nextboundarycornerstone.body.SegmentedAABB;
import cn.rbq108.nextboundarycornerstone.body.BodyBoxTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/** The inherited envelope is only for broad-phase queries; narrow-phase methods use OBBs. */
public final class OBBHitbox extends SegmentedAABB {
    private final List<OrientedBox> boxes;
    private double cachedInflation;
    private OBBHitbox inflated;

    public OBBHitbox(List<OrientedBox> boxes) {
        this(boxes, List.of());
    }

    public OBBHitbox(List<OrientedBox> boxes, List<BodyBoxTag> tags) {
        super(boxes.stream().map(OrientedBox::bounds).toList(), tags);
        this.boxes = List.copyOf(boxes);
    }

    public List<OrientedBox> orientedPieces() { return boxes; }

    @Override
    public AABB inflate(double amount) {
        if (amount == 0) return this;
        if (inflated == null || cachedInflation != amount) {
            cachedInflation = amount;
            inflated = new OBBHitbox(boxes.stream().map(box -> box.inflate(amount)).toList(), tags());
        }
        return inflated;
    }

    @Override
    public boolean contains(double x, double y, double z) {
        Vec3 point = new Vec3(x, y, z);
        for (OrientedBox box : boxes) if (box.contains(point)) return true;
        return false;
    }

    @Override
    public Optional<Vec3> clip(Vec3 start, Vec3 end) {
        Vec3 closest = null;
        double distance = Double.POSITIVE_INFINITY;
        for (OrientedBox box : boxes) {
            var hit = box.clip(start, end);
            if (hit.isPresent()) {
                double d = start.distanceToSqr(hit.get());
                if (d < distance) { closest = hit.get(); distance = d; }
            }
        }
        return Optional.ofNullable(closest);
    }

    @Override
    public Optional<Vec3> clipHead(Vec3 start, Vec3 end) {
        return boxes.getFirst().clip(start, end);
    }

    @Override
    public double distanceToSqr(Vec3 point) {
        double distance = Double.POSITIVE_INFINITY;
        for (OrientedBox box : boxes) distance = Math.min(distance, box.distanceToSqr(point));
        return distance;
    }
}
