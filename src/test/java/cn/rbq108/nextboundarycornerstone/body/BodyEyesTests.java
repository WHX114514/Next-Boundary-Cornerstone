package cn.rbq108.nextboundarycornerstone.body;

import cn.rbq108.nextboundarycornerstone.body.OOBB.OBBGeometry;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.ArrayList;

public final class BodyEyesTests {
    private static int checks;
    public static void main(String[] args) {
        var neutral = SegmentedHitboxPose.standing(new Quaternionf());
        Vec3 eye = BodyEyes.offset(neutral, 1.2f, 1);
        check(Math.abs(eye.y - 1.3122656) < 1.0e-6 && eye.x == 0 && eye.z == 0, "upright eye uses model's eye row, not cube center");
        check(Math.abs(eye.y - .72) > .5, "legacy lowered eye height is gone");
        check(Math.abs(eye.y - BodyEyes.neutralHeight(1.2f, 1)) < 1.0e-6, "dimension metadata matches neutral model eye");
        for (float scale : new float[] {.0625f, .5f, 1, 2, 4}) {
            for (float swim : new float[] {0, .3f, 1}) {
                for (boolean swimming : new boolean[] {false, true}) {
                    for (int angle = 0; angle < 8; angle++) {
                        var joints = new ArrayList<>(neutral.joints());
                        joints.set(0, new SegmentedHitboxPose.Joint(0, 0, 0, new Quaternionf().rotateYXZ(angle * .17f, angle * -.11f, angle * .07f)));
                        var pose = new SegmentedHitboxPose(new Quaternionf().rotateXYZ(angle * .31f, angle * .23f, angle * .47f), swim, false, joints, swimming);
                        Vec3 actual = BodyEyes.offset(pose, 1.2f * scale, scale);
                        var head = OBBGeometry.build(pose, 1.2f * scale, scale, swimming).getFirst();
                        Vec3 expected = head.center().add(head.axisY().scale(.5 / 16 * .9375 * scale));
                        check(head.contains(actual), "eye remains inside rotated/scaled/swimming head");
                        var probe = EyeBlockChecks.probe(actual, scale);
                        for (int corner = 0; corner < 8; corner++)
                            check(head.contains(new Vec3((corner & 1) == 0 ? probe.minX : probe.maxX,
                                    (corner & 2) == 0 ? probe.minY : probe.maxY, (corner & 4) == 0 ? probe.minZ : probe.maxZ)),
                                    "occupancy probe stays inside head at all orientations and scales");
                        check(actual.distanceTo(expected) < 3.0e-6, "eye matches independent head-box basis and eye row");
                    }
                }
            }
        }
        var tiltedHead = new ArrayList<>(neutral.joints());
        tiltedHead.set(0, new SegmentedHitboxPose.Joint(0, 0, 0, new Quaternionf().rotateX(.7f)));
        check(BodyEyes.offset(new SegmentedHitboxPose(neutral.body(), 0, false, tiltedHead), 1.2f, 1).distanceTo(eye) > .1,
                "independent head pitch moves viewpoint with eyes");
        var onlyArm = new ArrayList<>(neutral.joints());
        onlyArm.set(2, new SegmentedHitboxPose.Joint(5, 2, 0, new Quaternionf().rotateX(1.2f)));
        check(BodyEyes.offset(new SegmentedHitboxPose(neutral.body(), 0, true, onlyArm), 1.2f, 1).equals(eye),
                "arm animation and slim skin do not move eyes");
        var inverted = BodyEyes.offset(SegmentedHitboxPose.standing(new Quaternionf().rotateZ((float) Math.PI)), 1.2f, 1);
        check(inverted.y < 0, "upside-down eye can be below entity origin rather than world-up eye height");
        var from = SegmentedHitboxPose.standing(new Quaternionf().rotateZ(-.8f));
        var to = SegmentedHitboxPose.standing(new Quaternionf().rotateZ(.8f));
        Vec3 middle = BodyEyes.offset(SegmentedHitboxPose.interpolate(from, to, .5f), 1.2f, 1);
        check(middle.distanceTo(eye) < 1.0e-6, "eye follows interpolated skeleton arc");
        check(middle.distanceTo(BodyEyes.offset(from, 1.2f, 1).lerp(BodyEyes.offset(to, 1.2f, 1), .5)) > .1,
                "eye interpolation does not cut through body along an endpoint chord");
        System.out.println("BODY_EYES_TEST_PASS assertions=" + checks);
    }
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
}
