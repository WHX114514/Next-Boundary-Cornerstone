package cn.rbq108.nextboundarycornerstone.motion;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

/** Run with `bash gradlew surfaceTest`; no game bootstrap or external test engine required. */
public final class SurfaceAdhesionTests {
    private static final AABB BODY = new AABB(0, 0, 0, 1.2, 1.2, 1.2);
    private static final SurfaceAdhesionMath.Settings DEFAULTS = new SurfaceAdhesionMath.Settings(.4f, .3f, 6);
    private static int checks;

    public static void main(String[] args) {
        run("six face probes and inward-only collision synchronization", SurfaceAdhesionTests::sixFaces);
        run("partial-hit collision retains outward and tangent velocity", SurfaceAdhesionTests::collisionDirections);
        run("seams, partial shapes and nearest visible face", SurfaceAdhesionTests::geometry);
        run("contact-only acquisition and support retention", SurfaceAdhesionTests::acquisition);
        run("animated landing retains contact after releasing input", SurfaceAdhesionTests::animatedLanding);
        run("missing support pauses force and expires", SurfaceAdhesionTests::missingSupport);
        run("release distance, cooldown and reset", SurfaceAdhesionTests::release);
        run("corner keeps previous normal", SurfaceAdhesionTests::corner);
        run("sprint detaches immediately and requires new contact", SurfaceAdhesionTests::sprintTransition);
        run("six-axis finite attraction and no approach damping", SurfaceAdhesionTests::normalForces);
        run("normal and oblique sustained propulsion escape", SurfaceAdhesionTests::escape);
        run("near-target tiny outward acceleration remains positive", SurfaceAdhesionTests::tinyAcceleration);
        run("normal-only input brakes tangent drift", SurfaceAdhesionTests::braking);
        run("sprint preserves normal and tangent drift at the surface", SurfaceAdhesionTests::sprintForces);
        run("free drift and original target-speed/manual-brake behavior", SurfaceAdhesionTests::flight);
        run("zero acceleration remains finite", SurfaceAdhesionTests::zeroAcceleration);
        System.out.println("SURFACE_TEST_PASS scenarios=16 assertions=" + checks);
    }

    private static void run(String name, Runnable test) {
        test.run();
        System.out.println("PASS " + name);
    }

    private static void sixFaces() {
        for (Direction normal : Direction.values()) {
            var contacts = contact(normal, 0);
            check(contacts.size() == 1 && contacts.containsKey(normal), "single direct face " + normal);
            Vector3f velocity = vector(normal, -.4f);
            SurfaceGeometry.removeInwardVelocity(velocity, contacts);
            close(velocity.length(), 0, "inward clipped " + normal);
            velocity = vector(normal, .4f);
            SurfaceGeometry.removeInwardVelocity(velocity, contacts);
            close(SurfaceGeometry.dot(velocity, normal), .4, "outward retained " + normal);
        }
    }

    private static void collisionDirections() {
        Vector3f velocity = new Vector3f(-.3f, .08f, .19f);
        SurfaceGeometry.removeInwardVelocity(velocity, contact(Direction.EAST, 0));
        close(velocity.x, 0, "partial distance before impact still clips stored x");
        close(velocity.y, .08, "y tangent retained");
        close(velocity.z, .19, "z tangent retained");
        SurfaceGeometry.removeInwardVelocity(velocity, contact(Direction.UP, 0));
        close(velocity.y, .08, "outward velocity never zeroed by a floor contact");
        velocity.set(-.2f, -.2f, -.2f);
        var contacts = SurfaceGeometry.findContacts(BODY, List.of(obstacle(Direction.EAST, 0), obstacle(Direction.UP, 0)));
        SurfaceGeometry.removeInwardVelocity(velocity, contacts);
        close(velocity.x, 0, "corner x clipped");
        close(velocity.y, 0, "corner y clipped");
        close(velocity.z, -.2, "corner tangent retained");
        var clipped = SurfaceGeometry.blockedNormals(new Vec3(.4, -.2, .3), new Vec3(.12, -.2, .3));
        check(clipped.size() == 1 && clipped.contains(Direction.WEST), "partial non-block clipping records only blocked axis");
        velocity.set(.4f, .1f, .3f);
        SurfaceGeometry.removeInwardVelocity(velocity, clipped);
        close(velocity.x, 0, "entity/border collision clips inward velocity");
        velocity.x = -.2f;
        SurfaceGeometry.removeInwardVelocity(velocity, clipped);
        close(velocity.x, -.2, "entity/border collision preserves outward velocity");
        check(SurfaceGeometry.blockedNormals(Vec3.ZERO, new Vec3(0, .6, 0)).isEmpty(),
                "automatic step-up is not an attempted vertical collision");
    }

    private static void geometry() {
        AABB body = new AABB(.3, .5, .3, 1.5, 1.7, 1.5);
        var contacts = SurfaceGeometry.findContacts(body, List.of(new AABB(0, 0, 0, 1, .5, 1), new AABB(1, 0, 0, 2, .5, 1)));
        check(contacts.containsKey(Direction.UP), "half slabs and a block seam support the footprint");
        contacts = SurfaceGeometry.findContacts(BODY, List.of(new AABB(1.2, 1.2, 0, 2, 2, 1)));
        check(contacts.isEmpty(), "edge-only adjacency is not a face");
        contacts = SurfaceGeometry.findContacts(BODY, List.of(obstacle(Direction.EAST, .3), obstacle(Direction.EAST, .1)));
        close(contacts.get(Direction.EAST).gap(), .1, "nearest side wins over a hidden farther face");
        check(SurfaceGeometry.findContacts(BODY, List.of(obstacle(Direction.EAST, .36))).isEmpty(), "outside range ignored");
        check(SurfaceGeometry.findContacts(BODY, List.of()).isEmpty(), "air and empty collision shapes ignored");
        check(SurfaceGeometry.findContacts(BODY, List.of(new AABB(0, 0, 0, 1.2, 1.2, 1.2))).isEmpty(), "overlapping volume has no artificial internal face");
    }

    private static void acquisition() {
        SurfaceAdhesionState state = new SurfaceAdhesionState();
        check(state.update(BODY, contact(Direction.UP, .1), new Vector3f(), false) == null, "no remote capture");
        check(state.update(BODY, contact(Direction.UP, 0), new Vector3f(), false) == Direction.UP, "touch captures");
        AABB moved = BODY.move(0, .2, 0);
        var supports = SurfaceGeometry.findContacts(moved, List.of(obstacle(Direction.UP, 0)));
        check(state.update(moved, supports, new Vector3f(), false) == Direction.UP, "retention reaches farther than capture");
    }

    private static void missingSupport() {
        SurfaceAdhesionState state = attached(Direction.UP);
        check(state.update(BODY, Map.of(), new Vector3f(), false) == null, "no force on first missing tick");
        check(state.rememberedNormal() == Direction.UP, "first missing tick retains memory");
        check(state.update(BODY, contact(Direction.UP, 0), new Vector3f(), false) == Direction.UP, "seam recovers");
        state.update(BODY, Map.of(), new Vector3f(), false);
        check(state.update(BODY, Map.of(), new Vector3f(), false) == null && state.rememberedNormal() == null, "second missing tick clears");
    }

    private static void animatedLanding() {
        var pose = cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxPose.standing(new org.joml.Quaternionf());
        var bodies = cn.rbq108.nextboundarycornerstone.body.collision.RotationPlanner.boxes(pose, new Vec3(0, 2, 0), 1.2f, 1);
        var floor = new AABB(-10, 0, -10, 10, 1, 10);
        double fall = cn.rbq108.nextboundarycornerstone.body.collision.CompoundCollisionMath.envelope(bodies).minY - 1;
        var request = new Vec3(.3, -fall - .1, .2);
        var resolved = cn.rbq108.nextboundarycornerstone.body.collision.CompoundCollisionMath.clip(
                bodies, List.of(net.minecraft.world.phys.shapes.Shapes.create(floor)), request);
        check(resolved.y > request.y, "oblique landing really collides with the floor");
        var landed = bodies.stream().map(box -> box.move(resolved)).toList();
        var contactsAtImpact = SurfaceGeometry.findContacts(landed, List.of(floor));
        check(contactsAtImpact.get(Direction.UP).gap() <= SurfaceGeometry.CONTACT_EPSILON, "feet touched at impact");

        // Pose sampling runs before the next surface probe. A tiny retreat is enough to
        // miss first capture even though collision synchronization already removed downward drift.
        var animated = landed.stream().map(box -> box.move(0, .02, 0)).toList();
        var body = cn.rbq108.nextboundarycornerstone.body.collision.CompoundCollisionMath.envelope(animated);
        var contacts = SurfaceGeometry.findContacts(animated, List.of(floor));
        Vector3f velocity = new Vector3f(.3f, -.1f, .2f);
        SurfaceGeometry.removeInwardVelocity(velocity, SurfaceGeometry.blockedNormals(request, resolved));
        SurfaceAdhesionState state = new SurfaceAdhesionState();
        state.recordBlockCollision(contactsAtImpact, new Vector3f(.3f, -.1f, .2f), false);
        var normal = state.update(body, contacts, velocity, false);
        check(normal == Direction.UP, "actual collision survives a small animation gap");
        for (int tick = 0; tick < 12; tick++) {
            SurfaceAdhesionMath.apply(velocity, normal, new Vector3f(), new Vector3f(), .1f, DEFAULTS);
        }
        close(velocity.x, 0, "released horizontal x drift parks after landing");
        close(velocity.z, 0, "released horizontal z drift parks after landing");

        state.reset();
        state.recordBlockCollision(contactsAtImpact, new Vector3f(.3f, -.1f, .2f), true);
        check(state.rememberedNormal() == null, "sprinting impact cannot establish attachment");
        state.recordBlockCollision(contacts, new Vector3f(.3f, -.1f, .2f), false);
        check(state.rememberedNormal() == null, "nearby blocks after a non-block collision cannot establish attachment");
        state.recordBlockCollision(contactsAtImpact, new Vector3f(.3f, -.1f, .2f), false);
        check(state.update(body.move(0, 1, 0), Map.of(), velocity, false) == null,
                "moving out of reach after impact still releases");
    }

    private static void release() {
        SurfaceAdhesionState state = attached(Direction.UP);
        check(state.update(BODY.move(0, .8, 0), Map.of(), new Vector3f(), false) == null, "fast outward step releases immediately");
        check(state.rememberedNormal() == null, "release clears normal");
        for (int i = 0; i < 5; i++) check(state.update(BODY, contact(Direction.UP, 0), new Vector3f(), false) == null, "cooldown tick " + i);
        check(state.update(BODY, contact(Direction.UP, 0), new Vector3f(), false) == Direction.UP, "capture after cooldown");
        state.reset();
        check(state.rememberedNormal() == null, "reset removes session contact");
        check(state.update(BODY, contact(Direction.EAST, 0), new Vector3f(), false) == Direction.EAST, "reset allows new face without stale cooldown");
    }

    private static void corner() {
        SurfaceAdhesionState state = attached(Direction.UP);
        var contacts = SurfaceGeometry.findContacts(BODY, List.of(obstacle(Direction.UP, 0), obstacle(Direction.EAST, 0)));
        check(state.update(BODY, contacts, new Vector3f(-1, 0, 0), false) == Direction.UP, "keep old support despite stronger new impact");
    }

    private static void sprintTransition() {
        SurfaceAdhesionState state = attached(Direction.UP);
        var touching = contact(Direction.UP, 0);
        check(state.update(BODY, touching, new Vector3f(), true) == null, "first sprint tick detaches");
        check(state.rememberedNormal() == null, "sprint clears contact memory");
        check(state.update(BODY, touching, new Vector3f(), true) == null, "touching during sprint cannot attach");
        check(state.update(BODY.move(0, .2, 0), contact(Direction.UP, .2), new Vector3f(), false) == null,
                "ending sprint near a surface does not restore the old attachment");
        check(state.update(BODY, touching, new Vector3f(), false) == Direction.UP, "touching after sprint captures again");
    }

    private static void normalForces() {
        for (Direction normal : Direction.values()) {
            Vector3f inward = vector(normal, -.2f);
            SurfaceAdhesionMath.apply(inward, normal, new Vector3f(), new Vector3f(), .1f, DEFAULTS);
            close(SurfaceGeometry.dot(inward, normal), -.24, "no approach damping " + normal);
            Vector3f outward = vector(normal, .2f);
            SurfaceAdhesionMath.apply(outward, normal, new Vector3f(), new Vector3f(), .1f, DEFAULTS);
            close(SurfaceGeometry.dot(outward, normal), .11, "bounded outward resistance " + normal);
        }
    }

    private static void escape() {
        for (Direction normal : Direction.values()) {
            for (boolean oblique : new boolean[]{false, true}) {
                Vector3f target = vector(normal, .3f);
                if (oblique) target.add(tangent(normal).mul(.3f));
                Vector3f velocity = new Vector3f();
                float distance = 0;
                for (int tick = 0; tick < 100 && distance <= .35; tick++) {
                    Vector3f delta = FlightVelocityMath.advance(velocity, target, true, false, .1f, .9f);
                    SurfaceAdhesionMath.apply(velocity, normal, target, delta, .1f, DEFAULTS);
                    float outward = SurfaceGeometry.dot(velocity, normal);
                    check(outward > 0, "continuous outward movement " + normal + " oblique=" + oblique);
                    distance += outward;
                }
                check(distance > .35, "escaped " + normal + " oblique=" + oblique);
            }
        }
    }

    private static void tinyAcceleration() {
        Vector3f velocity = new Vector3f(0, .29999f, 0);
        Vector3f target = new Vector3f(0, .3f, 0);
        float previous = velocity.y;
        Vector3f delta = FlightVelocityMath.advance(velocity, target, true, false, .1f, .9f);
        SurfaceAdhesionMath.apply(velocity, Direction.UP, target, delta, .1f, DEFAULTS);
        check(velocity.y > previous, "tiny actual acceleration cannot be fully cancelled");
    }

    private static void braking() {
        Vector3f velocity = new Vector3f(.3f, 0, 0);
        for (int i = 0; i < 6; i++) {
            Vector3f target = new Vector3f(0, .3f, 0);
            Vector3f delta = FlightVelocityMath.advance(velocity, target, true, false, .1f, .9f);
            SurfaceAdhesionMath.apply(velocity, Direction.UP, target, delta, .1f, DEFAULTS);
        }
        close(velocity.x, 0, "space/up-only input parks the tangent drift");
        velocity.set(.3f, 0, 0);
        for (int i = 0; i < 6; i++) SurfaceAdhesionMath.apply(velocity, Direction.UP, new Vector3f(), new Vector3f(), .1f, DEFAULTS);
        close(velocity.x, 0, "no-input parking within six ticks");
        velocity.set(.3f, 0, 0);
        SurfaceAdhesionMath.apply(velocity, Direction.UP, new Vector3f(.3f, 0, 0), new Vector3f(), .1f, DEFAULTS);
        close(velocity.x, .3, "tangent input is not projected away or forcibly braked");
    }

    private static void sprintForces() {
        for (Direction normal : Direction.values()) {
            SurfaceAdhesionState state = attached(normal);
            Vector3f initial = vector(normal, .1f).add(tangent(normal).mul(.3f));
            Vector3f velocity = new Vector3f(initial);
            var surface = state.update(BODY, contact(normal, 0), velocity, true);
            SurfaceAdhesionMath.apply(velocity, surface, new Vector3f(), new Vector3f(), .1f, DEFAULTS);
            close(velocity.distance(initial), 0, "sprint applies no attraction, damping, or parking " + normal);
        }
    }

    private static void flight() {
        Vector3f velocity = new Vector3f(.1f, .2f, .3f);
        FlightVelocityMath.advance(velocity, new Vector3f(), false, false, .1f, .9f);
        close(velocity.y, .2, "free drift retained");
        SurfaceAdhesionMath.apply(velocity, null, new Vector3f(), new Vector3f(), .1f, DEFAULTS);
        close(velocity.y, .2, "no surface leaves drift untouched");
        FlightVelocityMath.advance(velocity, new Vector3f(), false, true, .1f, .9f);
        close(velocity.y, .182, "manual brake remains original 9 percent damping");
        velocity.zero();
        Vector3f delta = FlightVelocityMath.advance(velocity, new Vector3f(.3f, 0, 0), true, false, .1f, .9f);
        close(velocity.x, .1, "original target acceleration cap");
        close(delta.x, .1, "actual propulsion delta exported");
    }

    private static void zeroAcceleration() {
        Vector3f velocity = new Vector3f(.1f, .2f, .3f);
        FlightVelocityMath.advance(velocity, new Vector3f(0, 1, 0), true, false, 0, .9f);
        SurfaceAdhesionMath.apply(velocity, Direction.UP, new Vector3f(), new Vector3f(), 0, DEFAULTS);
        close(velocity.y, .2, "zero configured propulsion creates no auxiliary movement");
        check(Float.isFinite(velocity.x) && Float.isFinite(velocity.z), "finite zero budget");
    }

    private static SurfaceAdhesionState attached(Direction normal) {
        SurfaceAdhesionState state = new SurfaceAdhesionState();
        state.update(BODY, contact(normal, 0), new Vector3f(), false);
        return state;
    }

    private static Map<Direction, SurfaceGeometry.Contact> contact(Direction normal, double gap) {
        return SurfaceGeometry.findContacts(BODY, List.of(obstacle(normal, gap)));
    }

    private static AABB obstacle(Direction normal, double gap) {
        return switch (normal) {
            case EAST -> new AABB(-1 - gap, 0, 0, -gap, 1.2, 1.2);
            case WEST -> new AABB(1.2 + gap, 0, 0, 2.2 + gap, 1.2, 1.2);
            case UP -> new AABB(0, -1 - gap, 0, 1.2, -gap, 1.2);
            case DOWN -> new AABB(0, 1.2 + gap, 0, 1.2, 2.2 + gap, 1.2);
            case SOUTH -> new AABB(0, 0, -1 - gap, 1.2, 1.2, -gap);
            case NORTH -> new AABB(0, 0, 1.2 + gap, 1.2, 1.2, 2.2 + gap);
        };
    }

    private static Vector3f vector(Direction normal, float value) {
        return new Vector3f(normal.getStepX() * value, normal.getStepY() * value, normal.getStepZ() * value);
    }

    private static Vector3f tangent(Direction normal) {
        return normal.getAxis() == Direction.Axis.X ? new Vector3f(0, 0, 1) : new Vector3f(1, 0, 0);
    }

    private static void close(double actual, double expected, String description) {
        check(Math.abs(actual - expected) < .00001, description + ": expected " + expected + ", got " + actual);
    }

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description);
    }
}
