package cn.rbq108.nextboundarycornerstone.body.OOBB;

import cn.rbq108.nextboundarycornerstone.body.SegmentedAABB;
import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxGeometry;
import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxPose;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.List;

public final class OBBTests {
    private static int checks;

    public static void main(String[] args) {
        double diagonal = Math.sqrt(.5);
        var box = new OrientedBox(Vec3.ZERO, new Vec3(diagonal, diagonal, 0), new Vec3(-diagonal, diagonal, 0),
                new Vec3(0, 0, 1), new Vec3(1, 1, 1));
        Vec3 corner = new Vec3(1.3, 1.3, 0);
        check(box.bounds().contains(corner) && !box.contains(corner), "envelope corner is outside real OBB");
        Vec3 start = corner.add(0, 0, -2), end = corner.add(0, 0, 2);
        check(box.bounds().clip(start, end).isPresent() && box.clip(start, end).isEmpty(), "OBB rejects AABB corner false hit");
        close(box.distanceToSqr(corner), Math.pow(1.3 * Math.sqrt(2) - 1, 2), "reach uses actual rotated face");
        close(box.clip(new Vec3(0, 0, -2), new Vec3(0, 0, 2)).orElseThrow().z, -1, "ray hits correct face");
        check(box.clip(Vec3.ZERO, Vec3.ZERO).orElseThrow().equals(Vec3.ZERO), "stationary ray inside hits at zero");
        check(box.clip(corner, corner).isEmpty(), "stationary ray in envelope corner misses");
        check(box.clip(new Vec3(0, 0, -3), new Vec3(0, 0, -2)).isEmpty(), "finite segment does not extend to face");
        check(box.clip(new Vec3(0, 0, 2), new Vec3(0, 0, 3)).isEmpty(), "ray moving away misses");
        check(box.clip(Vec3.ZERO, new Vec3(3, 0, 0)).orElseThrow().equals(Vec3.ZERO), "inside origin returns start");
        check(box.inflate(.9).contains(corner), "margin inflates local half sizes");

        var composite = new OBBHitbox(List.of(box, box.move(0, 0, 4)));
        close(composite.clip(new Vec3(0, 0, 7), new Vec3(0, 0, -3)).orElseThrow().z, 5, "nearest part wins independent of list order");
        check(!composite.contains(corner), "composite containment uses OBBs");
        close(composite.distanceToSqr(corner), box.distanceToSqr(corner), "composite reach excludes envelope corner");
        check(composite.inflate(.1) instanceof OBBHitbox, "inflation retains OBB narrow phase");
        check(composite.inflate(.1) == composite.inflate(.1) && composite.inflate(0) == composite, "inflation is cached");
        Vec3 front = new Vec3(0, 0, -3), back = new Vec3(0, 0, 7);
        check(composite.isHeadshot(front, back, composite.clip(front, back).orElseThrow()), "first visible head is a headshot");
        check(!composite.isHeadshot(back, front, composite.clip(back, front).orElseThrow()), "body occluding head is not a headshot");
        check(composite.clipHead(start, end).isEmpty(), "headshot query rejects rotated envelope corner");

        var far = box.move(29_999_000, -56, -29_999_000);
        var farHit = far.clip(new Vec3(29_999_000, -56, -29_999_003), new Vec3(29_999_000, -56, -29_998_997)).orElseThrow();
        close(farHit.z, -29_999_001, "large world coordinates keep ray precision");

        for (int i = 0; i < 24; i++) {
            var pose = SegmentedHitboxPose.standing(new Quaternionf().rotationXYZ(i * .17f, i * .31f, i * .47f));
            if (i % 2 == 0) pose = new SegmentedHitboxPose(pose.body(), 1, i % 4 == 0, pose.joints());
            var oriented = OBBGeometry.build(pose, 1.2f, i % 3 == 0 ? 2 : 1, i % 2 == 0);
            var envelopes = SegmentedHitboxGeometry.build(pose, 1.2f, i % 3 == 0 ? 2 : 1, i % 2 == 0);
            check(oriented.size() == 16, "same sixteen anatomical pieces");
            for (int j = 0; j < 16; j++) {
                OrientedBox part = oriented.get(j);
                var bound = part.bounds();
                var aabb = envelopes.get(j);
                close(bound.minX, aabb.minX, "OBB/AABB anatomy bounds X");
                close(bound.maxY, aabb.maxY, "OBB/AABB anatomy bounds Y");
                close(bound.maxZ, aabb.maxZ, "OBB/AABB anatomy bounds Z");
                Vec3 h = part.halfSize();
                Vec3 rayStart = part.world(new Vec3(h.x * .2, -h.y * .1, -h.z * 3));
                Vec3 rayEnd = part.world(new Vec3(h.x * .2, -h.y * .1, h.z * 3));
                Vec3 hit = part.clip(rayStart, rayEnd).orElseThrow();
                check(hit.distanceTo(part.world(new Vec3(h.x * .2, -h.y * .1, -h.z))) < 1.0e-5,
                        "ray returns world-space face after arbitrary pitch/yaw/roll, swimming and scale");
                close(part.distanceToSqr(part.world(new Vec3(h.x + .2, 0, 0))), .04, "rotated distance is Euclidean");
            }
        }
        var identityPose = SegmentedHitboxPose.standing(new Quaternionf());
        var aabb = new SegmentedAABB(SegmentedHitboxGeometry.build(identityPose, 1.2f, 1, false));
        var obb = new OBBHitbox(OBBGeometry.build(identityPose, 1.2f, 1, false));
        Vec3 head = obb.orientedPieces().getFirst().center();
        check(aabb.clip(head.add(0, 0, -2), head.add(0, 0, 2)).orElseThrow()
                .distanceTo(obb.clip(head.add(0, 0, -2), head.add(0, 0, 2)).orElseThrow()) < 1.0e-5,
                "unrotated OBB retains existing AABB hit location");
        System.out.println("OBB_TEST_PASS assertions=" + checks);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void close(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 1.0e-5, message + ": " + actual + " != " + expected);
    }
}
