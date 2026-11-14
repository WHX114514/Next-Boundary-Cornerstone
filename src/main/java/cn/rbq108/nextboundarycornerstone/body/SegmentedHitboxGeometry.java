package cn.rbq108.nextboundarycornerstone.body;

import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Shared anatomy and model transforms for both hit-query modes. */
public final class SegmentedHitboxGeometry {
    private SegmentedHitboxGeometry() {}

    @FunctionalInterface
    public interface PartConsumer {
        void accept(Matrix4f transform, float x, float y, float z, float width, float height, float depth, int count);
    }

    public static List<AABB> build(SegmentedHitboxPose pose, float height, float scale, boolean swimming) {
        var boxes = new ArrayList<AABB>(16);
        visitParts(pose, height, scale, swimming,
                (transform, x, y, z, width, partHeight, depth, count) -> add(boxes, transform, x, y, z, width, partHeight, depth, count));
        return List.copyOf(boxes);
    }

    public static void visitParts(SegmentedHitboxPose pose, float height, float scale, boolean swimming, PartConsumer consumer) {
        Matrix4f root = root(pose, height, scale);

        // Model pixels, in the same order as the synchronized skeleton.
        visit(consumer, root, pose.joints().get(0), 0, -4, 0, 8, 8, 8, 1);
        visit(consumer, root, pose.joints().get(1), 0, 6, 0, 8, 12, 4, 3);
        float armWidth = pose.slim() ? 3 : 4;
        visit(consumer, root, pose.joints().get(2), pose.slim() ? .5f : 1, 4, 0, armWidth, 12, 4, 3);
        visit(consumer, root, pose.joints().get(3), pose.slim() ? -.5f : -1, 4, 0, armWidth, 12, 4, 3);
        visit(consumer, root, pose.joints().get(4), 0, 6, 0, 4, 12, 4, 3);
        visit(consumer, root, pose.joints().get(5), 0, 6, 0, 4, 12, 4, 3);
    }

    public static Matrix4f root(SegmentedHitboxPose pose, float height, float scale) {
        float center = height * .5f;
        Matrix4f root = new Matrix4f().translation(0, center, 0).rotate(pose.body())
                .translate(0, -.9f + (pose.swimming() ? center : 0), 0).scale(scale)
                .rotateY((float) Math.PI).rotateX(-pose.swim() * (float) Math.PI / 2);
        if (pose.swimming() && pose.swim() > 0) root.translate(0, -1, .3f);
        root.scale(-.9375f, -.9375f, .9375f).translate(0, -1.501f, 0);
        return root;
    }

    private static void visit(PartConsumer consumer, Matrix4f root, SegmentedHitboxPose.Joint joint,
                            float x, float y, float z, float width, float height, float depth, int count) {
        Matrix4f transform = new Matrix4f(root).translate(joint.x() / 16, joint.y() / 16, joint.z() / 16)
                .rotate(joint.rotation());
        consumer.accept(transform, x, y, z, width, height, depth, count);
    }

    private static void add(List<AABB> boxes, Matrix4f transform,
                            float x, float y, float z, float width, float height, float depth, int count) {
        float hx = width / 32, hy = height / count / 32, hz = depth / 32;
        // Absolute basis gives an AABB envelope in constant time, without rotating eight corners.
        float ex = Math.abs(transform.m00()) * hx + Math.abs(transform.m10()) * hy + Math.abs(transform.m20()) * hz;
        float ey = Math.abs(transform.m01()) * hx + Math.abs(transform.m11()) * hy + Math.abs(transform.m21()) * hz;
        float ez = Math.abs(transform.m02()) * hx + Math.abs(transform.m12()) * hy + Math.abs(transform.m22()) * hz;
        for (int i = 0; i < count; i++) {
            Vector3f c = transform.transformPosition(new Vector3f(x / 16, (y - height / 2 + (i + .5f) * height / count) / 16, z / 16));
            boxes.add(new AABB(c.x - ex, c.y - ey, c.z - ez, c.x + ex, c.y + ey, c.z + ez));
        }
    }
}
