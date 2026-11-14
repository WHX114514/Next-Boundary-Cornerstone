package cn.rbq108.nextboundarycornerstone.body.collision;

import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxGeometry;
import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxPose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Pure pose transaction: never changes an entity while searching for space. */
public final class RotationPlanner {
    public record Result(SegmentedHitboxPose pose, Vec3 offset, boolean bodyBlocked) {}
    private RotationPlanner() {}

    public static List<AABB> boxes(SegmentedHitboxPose pose, Vec3 origin, float height, float scale) {
        return SegmentedHitboxGeometry.build(pose, height, scale, pose.swimming()).stream()
                .map(box -> box.move(origin)).toList();
    }

    public static Result plan(SegmentedHitboxPose previous, SegmentedHitboxPose desired, Vec3 origin,
                               float height, float scale, List<VoxelShape> shapes) {
        List<AABB> obstacles = shapes.stream().flatMap(shape -> shape.toAabbs().stream()).toList();
        List<AABB> oldBoxes = boxes(previous, origin, height, scale);
        List<AABB> sweep = sweep(previous, desired, origin, height, scale);
        var offset = CompoundCollisionMath.findRotationOffset(oldBoxes, sweep, shapes, obstacles, .5 * scale);
        if (offset.isPresent()) return new Result(desired, offset.get(), false);

        // Preserve the desired world head direction on the previous body's neck, like free look.
        SegmentedHitboxPose headOnly = headOnly(previous, desired);
        List<AABB> headSweep = sweep(previous, headOnly, origin, height, scale);
        if (CompoundCollisionMath.clear(headSweep, obstacles, Vec3.ZERO)) return new Result(headOnly, Vec3.ZERO, true);
        // A head may have less room than the requested free-look angle. Keep the largest safe turn.
        SegmentedHitboxPose safe = previous;
        float low = 0, high = 1;
        for (int i = 0; i < 8; i++) {
            float middle = (low + high) / 2;
            var partial = SegmentedHitboxPose.interpolate(previous, headOnly, middle);
            if (CompoundCollisionMath.clear(sweep(previous, partial, origin, height, scale), obstacles, Vec3.ZERO)) {
                safe = partial;
                low = middle;
            } else high = middle;
        }
        return new Result(safe, Vec3.ZERO, true);
    }

    public static List<AABB> sweep(SegmentedHitboxPose from, SegmentedHitboxPose to, Vec3 origin, float height, float scale) {
        double bodyAngle = angle(from.body(), to.body());
        double jointAngle = 0, pivotDistance = 0;
        for (int i = 0; i < 6; i++) {
            var a = from.joints().get(i); var b = to.joints().get(i);
            jointAngle = Math.max(jointAngle, angle(a.rotation(), b.rotation()));
            pivotDistance = Math.max(pivotDistance, Math.max(Math.abs(a.x() - b.x()),
                    Math.max(Math.abs(a.y() - b.y()), Math.abs(a.z() - b.z()))) / 16);
        }
        double totalAngle = bodyAngle + jointAngle + Math.abs(from.swim() - to.swim()) * Math.PI / 2;
        int steps = Math.max(1, Math.min(72, (int) Math.ceil(Math.max(totalAngle / Math.toRadians(5), pivotDistance / .05))));
        // Only inflate parts which actually rotate. Inflating stationary arms would prevent free look at a wall.
        double[] allowance = new double[6];
        // The render root translates before applying entity scale. Its offset therefore does
        // not shrink with a tiny player, but still follows the body's rotational arc.
        double rootOffset = Math.max(Math.abs(-.9 + (from.swimming() ? height * .5 : 0)),
                Math.abs(-.9 + (to.swimming() ? height * .5 : 0)));
        double rootAllowance = rootOffset * (1 - Math.cos(Math.min(Math.PI, bodyAngle / steps) / 2));
        for (int i = 0; i < 6; i++) {
            double partAngle = bodyAngle + Math.abs(from.swim() - to.swim()) * Math.PI / 2
                    + angle(from.joints().get(i).rotation(), to.joints().get(i).rotation());
            allowance[i] = rootAllowance + 4 * scale * (1 - Math.cos(Math.min(Math.PI, partAngle / steps) / 2));
        }
        var result = new ArrayList<AABB>(steps * 16);
        List<AABB> last = boxes(from, origin, height, scale);
        // Vanilla's swim translation turns on as soon as swim is positive. Preserve
        // that one-sided endpoint instead of spanning only from the untranslated pose.
        List<AABB> afterSwimStarts = from.swimming() && from.swim() == 0 && to.swim() > 0
                ? boxes(new SegmentedHitboxPose(from.body(), Math.nextUp(0f), from.slim(), from.joints(), true), origin, height, scale)
                : null;
        for (int step = 1; step <= steps; step++) {
            List<AABB> next = boxes(SegmentedHitboxPose.interpolate(from, to, (float) step / steps), origin, height, scale);
            // The renderer's posture flag switches at t=1. Include the continuous endpoint
            // before that switch as well, otherwise its final arc can fall outside the union.
            boolean swimEnds = from.swimming() && from.swim() > 0 && to.swim() == 0;
            List<AABB> beforePostureSwitch = step == steps && (from.swimming() != to.swimming() || swimEnds)
                    ? boxes(new SegmentedHitboxPose(to.body(), swimEnds ? Math.nextUp(0f) : to.swim(), to.slim(), to.joints(), from.swimming()), origin, height, scale)
                    : null;
            for (int i = 0; i < last.size(); i++) {
                AABB covered = last.get(i).minmax(next.get(i));
                if (step == 1 && afterSwimStarts != null) covered = covered.minmax(afterSwimStarts.get(i));
                if (beforePostureSwitch != null) covered = covered.minmax(beforePostureSwitch.get(i));
                result.add(covered.inflate(allowance[i == 0 ? 0 : 1 + (i - 1) / 3]));
            }
            last = next;
        }
        return result;
    }

    private static double angle(Quaternionf a, Quaternionf b) {
        double dot = (double) a.x * b.x + (double) a.y * b.y + (double) a.z * b.z + (double) a.w * b.w;
        double aa = (double) a.x * a.x + (double) a.y * a.y + (double) a.z * a.z + (double) a.w * a.w;
        double bb = (double) b.x * b.x + (double) b.y * b.y + (double) b.z * b.z + (double) b.w * b.w;
        return 2 * Math.acos(Math.min(1, Math.abs(dot) / Math.sqrt(aa * bb)));
    }

    public static SegmentedHitboxPose headOnly(SegmentedHitboxPose previous, SegmentedHitboxPose desired) {
        Quaternionf oldRoot = SegmentedHitboxGeometry.root(previous, 1.2f, 1).getUnnormalizedRotation(new Quaternionf()).normalize();
        Quaternionf newRoot = SegmentedHitboxGeometry.root(desired, 1.2f, 1).getUnnormalizedRotation(new Quaternionf()).normalize();
        Quaternionf head = oldRoot.conjugate().mul(newRoot).mul(desired.joints().getFirst().rotation()).normalize();
        Quaternionf neutral = new Quaternionf().rotateX(previous.swimming() ? -(float) Math.PI / 2 : 0);
        Vector3f euler = new Quaternionf(neutral).conjugate().mul(head).getEulerAnglesYXZ(new Vector3f());
        head.set(neutral).mul(new Quaternionf().rotationYXZ(clamp(euler.y, 110), clamp(euler.x, 90), clamp(euler.z, 60)));
        var joints = new ArrayList<>(previous.joints());
        var neck = previous.joints().getFirst();
        joints.set(0, new SegmentedHitboxPose.Joint(neck.x(), neck.y(), neck.z(), head));
        return new SegmentedHitboxPose(previous.body(), previous.swim(), previous.slim(), joints, previous.swimming());
    }

    private static float clamp(float angle, float degrees) {
        float limit = (float) Math.toRadians(degrees);
        return Math.max(-limit, Math.min(limit, angle));
    }
}
