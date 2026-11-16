package cn.rbq108.nextboundarycornerstone.body;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class SegmentedHitboxTests {
    private static int checks;

    public static void main(String[] args) {
        var standing = SegmentedHitboxPose.standing(new Quaternionf());
        var boxes = SegmentedHitboxGeometry.build(standing, 1.2f, 1, false);
        check(boxes.size() == 16, "head + three torso rows + four three-piece limbs");
        close(boxes.get(0).getXsize(), 8.0 / 16 * .9375, "head width");
        close(boxes.get(1).getYsize(), 4.0 / 16 * .9375, "torso row height");
        close(boxes.get(1).minY, boxes.get(2).maxY, "torso rows meet without gaps");
        close(boxes.get(4).minY, boxes.get(5).maxY, "arm rows meet without gaps");

        var turned = SegmentedHitboxGeometry.build(SegmentedHitboxPose.standing(new Quaternionf().rotateZ((float) Math.PI / 2)), 1.2f, 1, false);
        Vector3f expected = new Quaternionf().rotateZ((float) Math.PI / 2)
                .transform(new Vector3f((float) boxes.get(0).getCenter().x, (float) boxes.get(0).getCenter().y - .6f, (float) boxes.get(0).getCenter().z));
        close(turned.get(0).getCenter().x, expected.x, "body roll moves head around body center");
        close(turned.get(0).getCenter().y, expected.y + .6, "body roll keeps pivot");
        close(turned.get(1).getXsize(), boxes.get(1).getYsize(), "rolled row remains axis aligned");

        var joints = new ArrayList<>(standing.joints());
        var arm = joints.get(2);
        joints.set(2, new SegmentedHitboxPose.Joint(arm.x(), arm.y(), arm.z(), new Quaternionf().rotateX(-(float) Math.PI / 2)));
        var raised = SegmentedHitboxGeometry.build(new SegmentedHitboxPose(new Quaternionf(), 0, false, joints), 1.2f, 1, false);
        check(Math.abs(raised.get(6).getCenter().z - boxes.get(6).getCenter().z) > .4, "forearm follows its own joint");
        check(raised.get(10).equals(boxes.get(10)), "moving arm does not move legs");

        for (int i = 0; i < 64; i++) {
            Quaternionf q = new Quaternionf().rotationXYZ(i * .37f, i * .53f, i * .71f);
            var rotated = SegmentedHitboxGeometry.build(SegmentedHitboxPose.standing(q), 1.2f, 1, false);
            for (AABB box : rotated) check(Double.isFinite(box.minX) && box.getXsize() > 0 && box.getYsize() > 0 && box.getZsize() > 0, "finite boxes for arbitrary roll/pitch/yaw");
        }
        var swimming = SegmentedHitboxGeometry.build(new SegmentedHitboxPose(new Quaternionf(), 1, false, standing.joints()), 1.2f, 1, true);
        check(Math.abs(swimming.get(0).getCenter().z - swimming.get(15).getCenter().z) > 1, "swimming anatomy is horizontal");
        var slim = SegmentedHitboxGeometry.build(new SegmentedHitboxPose(new Quaternionf(), 0, true, standing.joints()), 1.2f, 1, false);
        close(slim.get(4).getXsize(), 3.0 / 16 * .9375, "slim skin arm width");
        var scaled = SegmentedHitboxGeometry.build(standing, 1.2f, 2, false);
        close(scaled.get(0).getXsize(), boxes.get(0).getXsize() * 2, "entity scale is respected");

        var composite = new SegmentedAABB(List.of(new AABB(0, 0, 0, 1, 1, 1), new AABB(2, 0, 0, 3, 1, 1)));
        check(!composite.contains(1.5, .5, .5), "envelope gap is not a hit");
        check(composite.clip(new Vec3(1.5, .5, -1), new Vec3(1.5, .5, 2)).isEmpty(), "ray through gap misses");
        close(composite.clip(new Vec3(4, .5, .5), new Vec3(-1, .5, .5)).orElseThrow().x, 3, "closest piece wins");
        check(composite.clip(new Vec3(.5, .5, .5), new Vec3(4, .5, .5)).orElseThrow().equals(new Vec3(.5, .5, .5)), "inside start reports distance zero");
        close(composite.distanceToSqr(new Vec3(1.5, .5, .5)), .25, "reach measures pieces, not envelope");
        check(composite.inflate(.1).clip(new Vec3(1.5, .5, -1), new Vec3(1.5, .5, 2)).isEmpty(), "inflation retains gap");
        check(composite.inflate(.1) == composite.inflate(.1), "repeated rays reuse inflated boxes");
        check(composite.inflate(0) == composite, "zero margin avoids allocation");
        try {
            SegmentedHitboxPose.standing(new Quaternionf(0, 0, 0, 0));
            throw new AssertionError("zero quaternion accepted");
        } catch (IllegalArgumentException expectedFailure) { checks++; }
        animationSafety();
        handContext();
        modelRotations();
        System.out.println("SEGMENTED_HITBOX_TEST_PASS assertions=" + checks);
    }

    private static void modelRotations() {
        for (float x : new float[] {-3, -2.2f, -.4f, 0, .4f, 1.2f, 2.2f, 3}) {
            for (float y : new float[] {-3, -2.1f, -(float)Math.PI/2, -1.57078f, -.3f, 0,
                    .3f, 1.1f, 1.57078f, (float)Math.PI/2, 2.1f, 3}) {
                for (float z : new float[] {-3, -.6f, 0, .6f, 3}) {
                    var q = new Quaternionf().rotationZYX(z, y, x).normalize();
                    var e = ModelPoseMath.eulerZYX(q);
                    var restored = new Quaternionf().rotationZYX(e.z, e.y, e.x);
                    check(Math.abs(q.dot(restored)) > .99999f, "rendered joint retains quaternion orientation including gimbal poles");
                }
            }
        }
    }

    private static void animationSafety() {
        var rotation = new Quaternionf().rotateXYZ(.3f, -.4f, .5f);
        var fallback = new SegmentedHitboxPose.Joint(5, 2, 0, new Quaternionf());
        for (float z : new float[] {-8.375f, 8.375f, -16, 16}) {
            var joint = SegmentedHitboxPose.sampledJoint(5, 2, z, rotation, fallback);
            check(joint != fallback && joint.z() == z, "translated gun animation preserved");
        }
        new SegmentedHitboxPose.Joint(-16, -16, 16, rotation);
        new SegmentedHitboxPose.Joint(16, 32, -16, rotation);
        for (float[] bad : new float[][] {{16.001f, 2, 0}, {-16.001f, 2, 0}, {0, -16.001f, 0},
                {0, 32.001f, 0}, {0, 2, 16.001f}, {0, 2, -16.001f},
                {Float.NaN, 2, 0}, {0, Float.POSITIVE_INFINITY, 0}, {0, 2, Float.NEGATIVE_INFINITY}}) {
            check(SegmentedHitboxPose.sampledJoint(bad[0], bad[1], bad[2], rotation, fallback) == fallback,
                    "invalid animation uses previous/default joint");
            rejects(() -> new SegmentedHitboxPose.Joint(bad[0], bad[1], bad[2], rotation));
        }
        for (var bad : new Quaternionf[] {new Quaternionf(0, 0, 0, 0),
                new Quaternionf(Float.NaN, 0, 0, 1), new Quaternionf(Float.POSITIVE_INFINITY, 0, 0, 1)}) {
            check(SegmentedHitboxPose.sampledJoint(5, 2, 0, bad, fallback) == fallback, "bad rotation falls back");
            rejects(() -> new SegmentedHitboxPose.Joint(5, 2, 0, bad));
        }
        check(SegmentedHitboxPose.sampledJoint(5, 2, -8.375f, rotation, fallback) != fallback,
                "animation resumes immediately after invalid sample");
        var joints = new ArrayList<>(SegmentedHitboxPose.standing(new Quaternionf()).joints());
        joints.set(2, new SegmentedHitboxPose.Joint(5, 2, -8.375f, rotation));
        var pose = new SegmentedHitboxPose(new Quaternionf(), .4f, true, joints);
        var buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            pose.write(buf);
            var read = SegmentedHitboxPose.read(buf);
            close(read.joints().get(2).z(), -8.375f, "network retains reload translation");
            check(read.slim() && read.joints().size() == 6, "network retains skin and skeleton");
            var lerp = SegmentedHitboxPose.interpolate(SegmentedHitboxPose.standing(new Quaternionf()), read, .5f);
            close(lerp.joints().get(2).z(), -8.375f / 2, "reload interpolation accepts extended pivot");
            buf.clear();
            pose.write(buf);
            // body quaternion + swim + two booleans + first joint's X
            buf.setFloat(22, Float.NaN);
            rejects(() -> SegmentedHitboxPose.read(buf));
        } finally { buf.release(); }
    }

    private static void handContext() {
        check(!HandRenderContext.active(), "body rendering starts outside hand scope");
        HandRenderContext.during(() -> {
            check(HandRenderContext.active(), "hand animation identified");
            HandRenderContext.during(() -> check(HandRenderContext.active(), "nested hand draw"));
            check(HandRenderContext.active(), "outer hand scope survives nested draw");
        });
        check(!HandRenderContext.active(), "hand scope clears normally");
        try {
            HandRenderContext.during(() -> { throw new IllegalStateException("QA hand failure"); });
        } catch (IllegalStateException expected) { checks++; }
        check(!HandRenderContext.active(), "hand scope clears on failure");
    }

    private static void rejects(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("invalid network/animation joint accepted");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void close(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 1.0e-5, message + ": " + actual + " != " + expected);
    }
}
