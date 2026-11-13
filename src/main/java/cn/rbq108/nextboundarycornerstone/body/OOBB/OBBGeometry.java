package cn.rbq108.nextboundarycornerstone.body.OOBB;

import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxGeometry;
import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxPose;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class OBBGeometry {
    private OBBGeometry() {}

    public static List<OrientedBox> build(SegmentedHitboxPose pose, float height, float scale, boolean swimming) {
        var boxes = new ArrayList<OrientedBox>(16);
        SegmentedHitboxGeometry.visitParts(pose, height, scale, swimming, (transform, x, y, z, width, partHeight, depth, count) -> {
            Vec3 basisX = new Vec3(transform.m00(), transform.m01(), transform.m02());
            Vec3 basisY = new Vec3(transform.m10(), transform.m11(), transform.m12());
            Vec3 basisZ = new Vec3(transform.m20(), transform.m21(), transform.m22());
            Vec3 axisX = basisX.normalize();
            // Remove float round-off so local/world distance and clipping use an orthonormal basis.
            Vec3 axisY = basisY.subtract(axisX.scale(basisY.dot(axisX))).normalize();
            Vec3 axisZ = axisX.cross(axisY).normalize();
            if (axisZ.dot(basisZ) < 0) axisZ = axisZ.scale(-1);
            Vec3 half = new Vec3(width / 32 * basisX.length(), partHeight / count / 32 * basisY.length(), depth / 32 * basisZ.length());
            for (int i = 0; i < count; i++) {
                Vector3f center = transform.transformPosition(new Vector3f(x / 16,
                        (y - partHeight / 2 + (i + .5f) * partHeight / count) / 16, z / 16));
                boxes.add(new OrientedBox(new Vec3(center), axisX, axisY, axisZ, half));
            }
        });
        return List.copyOf(boxes);
    }
}
