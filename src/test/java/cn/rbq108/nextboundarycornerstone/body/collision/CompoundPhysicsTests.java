package cn.rbq108.nextboundarycornerstone.body.collision;

import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxPose;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;

import java.util.List;

public final class CompoundPhysicsTests {
    private static int checks;

    public static void main(String[] args) {
        translation();
        transforms();
        nudging();
        crampedTurn();
        intermediateTurn();
        headAtArmContact();
        scaledSweep();
        swimmingSweep();
        scaledThinFloor();
        swimmingHeadOnly();
        System.out.println("COMPOUND_PHYSICS_TEST_PASS assertions=" + checks);
    }

    private static List<VoxelShape> shapes(List<AABB> solids) { return solids.stream().map(Shapes::create).toList(); }

    private static void translation() {
        var pieces = List.of(new AABB(0, 0, 0, .2, 1, .2), new AABB(.8, 0, 0, 1, 1, .2));
        var centerPost = List.of(new AABB(.4, 0, 1, .6, 1, 2));
        var requested = new Vec3(0, 0, 3);
        close(CompoundCollisionMath.clip(pieces, shapes(centerPost), requested).z, 3, "post passes through empty space between parts");
        check(CompoundCollisionMath.clip(List.of(CompoundCollisionMath.envelope(pieces)), shapes(centerPost), requested).z < 1,
                "the former large box would have stopped at the post");
        var unit = List.of(new AABB(0, 0, 0, 1, 1, 1));
        for (Direction face : Direction.values()) {
            Vec3 delta = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ()).scale(20);
            AABB wall = switch (face) {
                case EAST -> new AABB(2, -4, -4, 2.01, 4, 4);
                case WEST -> new AABB(-1.01, -4, -4, -1, 4, 4);
                case UP -> new AABB(-4, 2, -4, 4, 2.01, 4);
                case DOWN -> new AABB(-4, -1.01, -4, 4, -1, 4);
                case SOUTH -> new AABB(-4, -4, 2, 4, 4, 2.01);
                case NORTH -> new AABB(-4, -4, -1.01, 4, 4, -1);
            };
            Vec3 resolved = CompoundCollisionMath.clip(unit, shapes(List.of(wall)), delta);
            close(resolved.length(), 1, "continuous six-axis collision against thin shape " + face);
        }
        var wall = shapes(List.of(new AABB(2, -4, -4, 3, 4, 4)));
        Vec3 slide = CompoundCollisionMath.clip(unit, wall, new Vec3(3, .2, 1.5));
        close(slide.x, 1, "wall clips only inward axis");
        close(slide.y, .2, "vertical tangent retained");
        close(slide.z, 1.5, "horizontal tangent retained");
    }

    private static void transforms() {
        PhysicsBox body = new PhysicsBox(List.of(new AABB(0, 0, 0, .2, 1, 1), new AABB(.8, 0, 0, 1, 1, 1)));
        check(body.shape() == body.shape(), "union voxel shape is cached");
        check(!body.intersects(.4, .2, .2, .6, .8, .8), "full-block fast-path query retains empty space");
        check(!body.intersects(new PhysicsBox(List.of(new AABB(.4, .2, .2, .6, .8, .8)))),
                "compound-to-compound intersection tests actual pieces");
        check(!Shapes.joinIsNotEmpty(body.shape(), Shapes.create(new AABB(.4, .2, .2, .6, .8, .8)), BooleanOp.AND),
                "union retains interior empty space");
        var moved = body.move(4, 2, -3).deflate(.01);
        check(moved.pieces().size() == 2 && !moved.contains(4.5, 2.5, -2.5), "move and deflate retain separate boxes");
        check(moved.expandTowards(0, 0, 1).pieces().size() == 2, "vanilla sweep queries retain compound type");
    }

    private static void nudging() {
        var old = List.of(new AABB(-.2, -.5, -.5, .2, .5, .5));
        var desired = List.of(new AABB(-.4, -.5, -.5, .4, .5, .5));
        var wall = List.of(new AABB(.35, -4, -4, 2, 4, 4));
        Vec3 shift = CompoundCollisionMath.findRotationOffset(old, desired, shapes(wall), wall, .5).orElseThrow();
        close(shift.x, -.0500001, "turning against a wall pushes player by nearest separating displacement");
        close(shift.y, 0, "no unnecessary vertical displacement");
        check(CompoundCollisionMath.clear(desired, wall, shift), "new posture fits after displacement");
        var trapped = List.of(wall.getFirst(), new AABB(-2, -4, -4, -.21, 4, 4));
        check(CompoundCollisionMath.findRotationOffset(old, desired, shapes(trapped), trapped, .5).isEmpty(), "opposite wall prevents displacement");

        var narrowOld = List.of(new AABB(-.1, -.3, -.3, .1, .3, .3));
        var longNew = List.of(new AABB(-.5, -.3, -.3, .5, .3, .3));
        var thinWalls = List.of(new AABB(.25, -4, -4, .26, 4, 4), new AABB(-2, -4, -4, -.11, 4, 4));
        check(CompoundCollisionMath.findRotationOffset(narrowOld, longNew, shapes(thinWalls), thinWalls, 1).isEmpty(),
                "a free endpoint across a thin wall cannot be reached by teleporting");
    }

    private static void crampedTurn() {
        var previous = SegmentedHitboxPose.standing(new Quaternionf());
        var desired = SegmentedHitboxPose.standing(new Quaternionf().rotateY((float) Math.PI / 2));
        var walls = shapes(List.of(new AABB(-4, -4, -.6, 4, 4, -.35), new AABB(-4, -4, .35, 4, 4, .6)));
        var result = RotationPlanner.plan(previous, desired, Vec3.ZERO, 1.2f, 1, walls);
        check(result.bodyBlocked(), "narrow corridor refuses body turn");
        check(Math.abs(result.pose().body().dot(previous.body())) > .99999, "body retains last safe orientation");
        check(Math.abs(result.pose().joints().getFirst().rotation().dot(previous.joints().getFirst().rotation())) < .99,
                "head can turn independently in corridor");
        close(result.offset().length(), 0, "no displacement when no valid room exists");
        check(CompoundCollisionMath.clear(RotationPlanner.boxes(result.pose(), Vec3.ZERO, 1.2f, 1),
                walls.stream().flatMap(s -> s.toAabbs().stream()).toList(), Vec3.ZERO), "accepted head-only shape is collision-free");
        var released = RotationPlanner.plan(result.pose(), desired, Vec3.ZERO, 1.2f, 1, List.of());
        check(!released.bodyBlocked() && Math.abs(released.pose().body().dot(desired.body())) > .99999,
                "body resumes following view after leaving cramped space");
    }

    private static void intermediateTurn() {
        var previous = SegmentedHitboxPose.standing(new Quaternionf());
        var desired = SegmentedHitboxPose.standing(new Quaternionf().rotateZ((float) Math.PI));
        var walls = List.of(new AABB(-2, -4, -4, -.65, 4, 4), new AABB(.5, -4, -4, 2, 4, 4));
        check(CompoundCollisionMath.clear(RotationPlanner.boxes(desired, Vec3.ZERO, 1.2f, 1), walls, Vec3.ZERO),
                "a half-roll endpoint fits in corridor");
        var result = RotationPlanner.plan(previous, desired, Vec3.ZERO, 1.2f, 1, shapes(walls));
        check(result.bodyBlocked(), "intermediate rotational sweep prevents spinning through wall");
    }

    private static void headAtArmContact() {
        var previous = SegmentedHitboxPose.standing(new Quaternionf().rotateY(.9f));
        var bounds = CompoundCollisionMath.envelope(RotationPlanner.boxes(previous, Vec3.ZERO, 1.2f, 1));
        var walls = List.of(new AABB(-4, -4, bounds.maxZ + .00001, 4, 4, 3),
                new AABB(-4, -4, -3, 4, 4, bounds.minZ - .00001));
        var joints = new java.util.ArrayList<>(previous.joints());
        joints.set(0, new SegmentedHitboxPose.Joint(0, 0, 0, new Quaternionf().rotateY(.7f)));
        var desired = new SegmentedHitboxPose(previous.body(), 0, false, joints);
        var result = RotationPlanner.plan(previous, desired, Vec3.ZERO, 1.2f, 1, shapes(walls));
        check(!result.bodyBlocked(), "stationary arms at wall do not obstruct head-only rotation");
        check(Math.abs(result.pose().joints().getFirst().rotation().dot(desired.joints().getFirst().rotation())) > .99999,
                "head reaches requested angle while arms remain at contact");
    }

    private static void scaledSweep() {
        for (float scale : new float[] {.01f, .0625f, .25f, 1, 4}) {
            var from = SegmentedHitboxPose.standing(new Quaternionf().rotateZ(-.02f));
            var to = SegmentedHitboxPose.standing(new Quaternionf().rotateZ(.02f));
            var sweep = RotationPlanner.sweep(from, to, Vec3.ZERO, 1.2f * scale, scale);
            var actual = RotationPlanner.boxes(SegmentedHitboxPose.interpolate(from, to, .5f), Vec3.ZERO, 1.2f * scale, scale);
            for (int part = 0; part < 16; part++) {
                AABB bounds = actual.get(part);
                AABB covered = sweep.get(part);
                for (int i = part + 16; i < sweep.size(); i += 16) covered = covered.minmax(sweep.get(i));
                check(covered.minX <= bounds.minX + 1.0e-7 && covered.minY <= bounds.minY + 1.0e-7
                                && covered.minZ <= bounds.minZ + 1.0e-7 && covered.maxX >= bounds.maxX - 1.0e-7
                                && covered.maxY >= bounds.maxY - 1.0e-7 && covered.maxZ >= bounds.maxZ - 1.0e-7,
                        "rotation sweep covers intermediate part " + part + " at scale " + scale);
            }
        }
    }

    private static void swimmingSweep() {
        var standing = SegmentedHitboxPose.standing(new Quaternionf());
        for (boolean alreadySwimming : new boolean[] {false, true}) {
            var from = new SegmentedHitboxPose(new Quaternionf().rotateZ(-.02f), 0, false, standing.joints(), alreadySwimming);
            var to = new SegmentedHitboxPose(new Quaternionf().rotateZ(.02f), 1, false, standing.joints(), true);
            verifySwimSweep(from, to, "enter flag=" + alreadySwimming);
            verifySwimSweep(to, from, "exit flag=" + alreadySwimming);
        }
    }

    private static void verifySwimSweep(SegmentedHitboxPose from, SegmentedHitboxPose to, String label) {
        var sweep = RotationPlanner.sweep(from, to, Vec3.ZERO, 1.2f, 1);
        int steps = sweep.size() / 16;
        for (int step = 0; step < steps; step++) {
            for (int sample = 1; sample < 10; sample++) {
                var actual = RotationPlanner.boxes(SegmentedHitboxPose.interpolate(from, to, (step + sample / 10f) / steps), Vec3.ZERO, 1.2f, 1);
                for (int part = 0; part < 16; part++) {
                    AABB bounds = actual.get(part), covered = sweep.get(step * 16 + part);
                    check(covered.minX <= bounds.minX + 1.0e-7 && covered.minY <= bounds.minY + 1.0e-7
                                    && covered.minZ <= bounds.minZ + 1.0e-7 && covered.maxX >= bounds.maxX - 1.0e-7
                                    && covered.maxY >= bounds.maxY - 1.0e-7 && covered.maxZ >= bounds.maxZ - 1.0e-7,
                            "swim transition intermediate coverage " + label + " step=" + step + " part=" + part);
                }
            }
        }
    }

    private static void scaledThinFloor() {
        float scale = .0625f, height = 1.2f * scale;
        var from = SegmentedHitboxPose.standing(new Quaternionf().rotateX(-.04f));
        var to = SegmentedHitboxPose.standing(new Quaternionf().rotateX(.04f));
        double plane = CompoundCollisionMath.envelope(RotationPlanner.boxes(
                SegmentedHitboxPose.interpolate(from, to, .5f), Vec3.ZERO, height, scale)).minY + .00001;
        var floor = List.of(new AABB(-1, plane - 1, -1, 1, plane, 1));
        check(CompoundCollisionMath.clear(RotationPlanner.boxes(from, Vec3.ZERO, height, scale), floor, Vec3.ZERO)
                        && CompoundCollisionMath.clear(RotationPlanner.boxes(to, Vec3.ZERO, height, scale), floor, Vec3.ZERO),
                "small player's initial and final poses both fit above thin contact plane");
        var result = RotationPlanner.plan(from, to, Vec3.ZERO, height, scale, shapes(floor));
        check(!result.bodyBlocked() && result.offset().y > .00001, "small player moves upward for intermediate floor contact");
    }

    private static void swimmingHeadOnly() {
        var joints = new java.util.ArrayList<>(SegmentedHitboxPose.standing(new Quaternionf()).joints());
        joints.set(0, new SegmentedHitboxPose.Joint(0, 0, 0, new Quaternionf().rotateX(-(float) Math.PI / 2)));
        var from = new SegmentedHitboxPose(new Quaternionf(), 1, false, joints, true);
        var desired = new SegmentedHitboxPose(new Quaternionf().rotateY(.35f), 1, false, joints, true);
        var accepted = RotationPlanner.headOnly(from, desired);
        check(accepted.swimming() && accepted.swim() == 1 && accepted.body().equals(from.body()),
                "head-only fallback retains floating body and posture");
        var wanted = cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxGeometry.root(desired, 1.2f, 1)
                .transformDirection(desired.joints().getFirst().rotation().transform(new org.joml.Vector3f(0, 0, 1))).normalize();
        var actual = cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxGeometry.root(accepted, 1.2f, 1)
                .transformDirection(accepted.joints().getFirst().rotation().transform(new org.joml.Vector3f(0, 0, 1))).normalize();
        check(wanted.dot(actual) > .99999, "floating head-only fallback preserves world head direction");
    }

    private static void close(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 1.0e-5, message + ": " + actual + " != " + expected);
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
