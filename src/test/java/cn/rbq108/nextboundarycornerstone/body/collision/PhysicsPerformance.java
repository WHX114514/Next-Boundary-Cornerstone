package cn.rbq108.nextboundarycornerstone.body.collision;

import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxGeometry;
import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxPose;
import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.joml.Quaternionf;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/** Opt-in benchmark with allocation counters and JFR; never part of check or the mod jar. */
public final class PhysicsPerformance {
    private static volatile Object consumed;
    private static final com.sun.management.ThreadMXBean ALLOCATIONS =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

    public static void main(String[] args) throws Exception {
        Path prefix = Path.of(args[0]);
        Files.createDirectories(prefix.toAbsolutePath().getParent());
        var results = new LinkedHashMap<String, Object>();
        try (var recording = new Recording(Configuration.getConfiguration("profile"))) {
            recording.enable("jdk.ObjectAllocationSample").withStackTrace();
            recording.start();
            results.put("rotatingUnion", measure(120, index -> {
                var pose = pose(index);
                var box = new PhysicsBox(SegmentedHitboxGeometry.build(pose, 1.2f, 1, false));
                consumed = box.inflate(.3501).shape();
            }));
            var walls = List.of(Shapes.create(new AABB(-2, -4, -4, -.5, 4, 4)),
                    Shapes.create(new AABB(.5, -4, -4, 2, 4, 4)));
            results.put("crampedRotation", measure(120, index -> consumed = RotationPlanner.plan(
                    SegmentedHitboxPose.standing(new Quaternionf()), pose(index), Vec3.ZERO, 1.2f, 1, walls)));
            recording.stop();
            recording.dump(Path.of(prefix + ".jfr"));
        }
        Files.writeString(Path.of(prefix + ".json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(results));
        System.out.println("PHYSICS_PROFILE " + new com.google.gson.Gson().toJson(results));
    }

    private static SegmentedHitboxPose pose(int index) {
        return SegmentedHitboxPose.standing(new Quaternionf().rotateXYZ(.17f + index * .003f, .31f + index * .005f, .43f + index * .004f));
    }

    private static Object measure(int iterations, java.util.function.IntConsumer action) {
        for (int i = 0; i < 20; i++) action.accept(i);
        long[] times = new long[iterations];
        long startBytes = ALLOCATIONS.getThreadAllocatedBytes(Thread.currentThread().threadId());
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            action.accept(i);
            times[i] = System.nanoTime() - start;
        }
        long bytes = ALLOCATIONS.getThreadAllocatedBytes(Thread.currentThread().threadId()) - startBytes;
        Arrays.sort(times);
        return java.util.Map.of("iterations", iterations, "meanMs", Arrays.stream(times).average().orElseThrow() / 1e6,
                "p95Ms", times[(int) (iterations * .95)] / 1e6, "allocatedBytesPerCall", bytes / iterations);
    }
}
