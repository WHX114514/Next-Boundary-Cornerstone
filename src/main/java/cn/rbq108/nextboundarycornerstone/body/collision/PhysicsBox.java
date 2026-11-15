package cn.rbq108.nextboundarycornerstone.body.collision;

import cn.rbq108.nextboundarycornerstone.body.SegmentedAABB;
import cn.rbq108.nextboundarycornerstone.body.BodyBoxTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/** An envelope for spatial indexing, with the actual union preserved through vanilla box transforms. */
public final class PhysicsBox extends SegmentedAABB {
    private VoxelShape shape;
    private PhysicsBox inflated;
    private double inflateX, inflateY, inflateZ;

    public PhysicsBox(List<AABB> pieces) { super(pieces); }
    public PhysicsBox(List<AABB> pieces, List<BodyBoxTag> tags) { super(pieces, tags); }

    @Override public boolean intersects(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        // BlockCollisions has a full-cube fast path which bypasses VoxelShape intersection.
        for (AABB piece : pieces()) if (piece.intersects(minX, minY, minZ, maxX, maxY, maxZ)) return true;
        return false;
    }

    @Override public boolean intersects(AABB other) {
        if (other instanceof PhysicsBox compound) {
            for (AABB piece : compound.pieces()) if (intersects(piece)) return true;
            return false;
        }
        return intersects(other.minX, other.minY, other.minZ, other.maxX, other.maxY, other.maxZ);
    }

    public VoxelShape shape() {
        if (shape == null) {
            // Collision needs the exact union, not repeated optimize/decompose passes.
            // A balanced merge also avoids rebuilding a nearly complete grid per part.
            shape = union(0, pieces().size());
        }
        return shape;
    }

    private VoxelShape union(int start, int end) {
        if (end - start == 1) return Shapes.create(pieces().get(start));
        int middle = (start + end) >>> 1;
        return Shapes.joinUnoptimized(union(start, middle), union(middle, end), BooleanOp.OR);
    }

    public boolean intersectsShape(VoxelShape obstacle) {
        if (obstacle.isEmpty() || !intersects(obstacle.bounds())) return false;
        for (AABB box : obstacle.toAabbs()) {
            for (AABB piece : pieces()) if (CompoundCollisionMath.overlaps(piece, box)) return true;
        }
        return false;
    }

    @Override public PhysicsBox move(double x, double y, double z) {
        if (x == 0 && y == 0 && z == 0) return this;
        return new PhysicsBox(pieces().stream().map(box -> box.move(x, y, z)).toList(), tags());
    }
    @Override public PhysicsBox move(Vec3 delta) { return move(delta.x, delta.y, delta.z); }
    @Override public PhysicsBox inflate(double x, double y, double z) {
        if (x == 0 && y == 0 && z == 0) return this;
        if (inflated == null || inflateX != x || inflateY != y || inflateZ != z) {
            inflateX = x; inflateY = y; inflateZ = z;
            inflated = new PhysicsBox(pieces().stream().map(box -> box.inflate(x, y, z)).toList(), tags());
        }
        return inflated;
    }
    @Override public PhysicsBox inflate(double amount) { return amount == 0 ? this : inflate(amount, amount, amount); }
    @Override public PhysicsBox deflate(double amount) { return inflate(-amount); }
    @Override public PhysicsBox deflate(double x, double y, double z) { return inflate(-x, -y, -z); }
    @Override public PhysicsBox expandTowards(double x, double y, double z) {
        if (x == 0 && y == 0 && z == 0) return this;
        return new PhysicsBox(pieces().stream().map(box -> box.expandTowards(x, y, z)).toList(), tags());
    }
    @Override public PhysicsBox expandTowards(Vec3 delta) { return expandTowards(delta.x, delta.y, delta.z); }
}
