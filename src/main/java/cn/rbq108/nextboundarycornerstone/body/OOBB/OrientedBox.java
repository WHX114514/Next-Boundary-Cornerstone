package cn.rbq108.nextboundarycornerstone.body.OOBB;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Immutable box with orthonormal axes. All intersections take place in the box's local space. */
public record OrientedBox(Vec3 center, Vec3 axisX, Vec3 axisY, Vec3 axisZ, Vec3 halfSize) {
    public OrientedBox {
        if (!finite(center) || !finite(halfSize) || halfSize.x < 0 || halfSize.y < 0 || halfSize.z < 0
                || !finite(axisX) || !finite(axisY) || !finite(axisZ)
                || Math.abs(axisX.lengthSqr() - 1) > 1.0e-5 || Math.abs(axisY.lengthSqr() - 1) > 1.0e-5
                || Math.abs(axisZ.lengthSqr() - 1) > 1.0e-5
                || Math.abs(axisX.dot(axisY)) > 1.0e-5 || Math.abs(axisX.dot(axisZ)) > 1.0e-5
                || Math.abs(axisY.dot(axisZ)) > 1.0e-5) {
            throw new IllegalArgumentException("Invalid oriented box");
        }
    }

    private static boolean finite(Vec3 v) {
        return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }

    private Vec3 local(Vec3 point) {
        Vec3 delta = point.subtract(center);
        return new Vec3(delta.dot(axisX), delta.dot(axisY), delta.dot(axisZ));
    }

    public Vec3 world(Vec3 point) {
        return center.add(axisX.scale(point.x)).add(axisY.scale(point.y)).add(axisZ.scale(point.z));
    }

    public boolean contains(Vec3 point) {
        Vec3 p = local(point);
        return p.x >= -halfSize.x && p.x < halfSize.x && p.y >= -halfSize.y && p.y < halfSize.y
                && p.z >= -halfSize.z && p.z < halfSize.z;
    }

    public Optional<Vec3> clip(Vec3 start, Vec3 end) {
        Vec3 p = local(start);
        Vec3 delta = end.subtract(start);
        double dx = delta.dot(axisX), dy = delta.dot(axisY), dz = delta.dot(axisZ);
        if (Math.abs(dx) < 1.0e-12) {
            if (Math.abs(p.x) > halfSize.x) return Optional.empty();
            dx = 0;
        }
        if (Math.abs(dy) < 1.0e-12) {
            if (Math.abs(p.y) > halfSize.y) return Optional.empty();
            dy = 0;
        }
        if (Math.abs(dz) < 1.0e-12) {
            if (Math.abs(p.z) > halfSize.z) return Optional.empty();
            dz = 0;
        }
        double ax = dx == 0 ? Double.NEGATIVE_INFINITY : (-halfSize.x - p.x) / dx;
        double bx = dx == 0 ? Double.POSITIVE_INFINITY : (halfSize.x - p.x) / dx;
        double ay = dy == 0 ? Double.NEGATIVE_INFINITY : (-halfSize.y - p.y) / dy;
        double by = dy == 0 ? Double.POSITIVE_INFINITY : (halfSize.y - p.y) / dy;
        double az = dz == 0 ? Double.NEGATIVE_INFINITY : (-halfSize.z - p.z) / dz;
        double bz = dz == 0 ? Double.POSITIVE_INFINITY : (halfSize.z - p.z) / dz;
        double enter = Math.max(0, Math.max(Math.min(ax, bx), Math.max(Math.min(ay, by), Math.min(az, bz))));
        double exit = Math.min(1, Math.min(Math.max(ax, bx), Math.min(Math.max(ay, by), Math.max(az, bz))));
        return enter <= exit ? Optional.of(start.add(delta.scale(enter))) : Optional.empty();
    }

    public double distanceToSqr(Vec3 point) {
        Vec3 p = local(point);
        double x = Math.max(0, Math.abs(p.x) - halfSize.x);
        double y = Math.max(0, Math.abs(p.y) - halfSize.y);
        double z = Math.max(0, Math.abs(p.z) - halfSize.z);
        return x * x + y * y + z * z;
    }

    public AABB bounds() {
        double x = Math.abs(axisX.x) * halfSize.x + Math.abs(axisY.x) * halfSize.y + Math.abs(axisZ.x) * halfSize.z;
        double y = Math.abs(axisX.y) * halfSize.x + Math.abs(axisY.y) * halfSize.y + Math.abs(axisZ.y) * halfSize.z;
        double z = Math.abs(axisX.z) * halfSize.x + Math.abs(axisY.z) * halfSize.y + Math.abs(axisZ.z) * halfSize.z;
        return new AABB(center.x - x, center.y - y, center.z - z, center.x + x, center.y + y, center.z + z);
    }

    public OrientedBox move(double x, double y, double z) {
        return new OrientedBox(center.add(x, y, z), axisX, axisY, axisZ, halfSize);
    }

    public OrientedBox inflate(double amount) {
        return new OrientedBox(center, axisX, axisY, axisZ,
                new Vec3(Math.max(0, halfSize.x + amount), Math.max(0, halfSize.y + amount), Math.max(0, halfSize.z + amount)));
    }
}
