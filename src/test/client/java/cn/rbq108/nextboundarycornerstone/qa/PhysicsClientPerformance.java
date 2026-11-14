package cn.rbq108.nextboundarycornerstone.qa;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxPose;
import cn.rbq108.nextboundarycornerstone.body.trunk;
import cn.rbq108.nextboundarycornerstone.main;
import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Quaternionf;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** Actual hidden client, fixed scene/input schedule, frame times plus per-thread allocations. */
public final class PhysicsClientPerformance {
    private static final com.sun.management.ThreadMXBean ALLOCATIONS =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    private static final Map<String, Object> RESULTS = new LinkedHashMap<>();
    private static final ArrayList<Long> FRAMES = new ArrayList<>();
    private static Recording recording;
    private static int scene, ticks;
    private static long started, sceneStarted, previousFrame, sampledAt, sampledBytes;
    private static volatile boolean ready = true;

    private PhysicsClientPerformance() {}

    public static boolean tick(Minecraft mc) throws Exception {
        long now = System.nanoTime();
        if (started == 0) {
            started = sceneStarted = now;
            mc.options.enableVsync().set(false);
            mc.options.framerateLimit().set(120);
            mc.options.renderDistance().set(4);
            mc.options.simulationDistance().set(5);
            recording = new Recording(Configuration.getConfiguration("profile"));
            recording.start();
            NeoForge.EVENT_BUS.addListener(PhysicsClientPerformance::frame);
        }
        if (now - started > 180_000_000_000L) throw new AssertionError("performance fixture timeout");
        if (!ready) { sceneStarted = now; return false; }
        ticks++;
        double seconds = (now - sceneStarted) / 1e9;
        if (scene > 0) {
            GlobalVariables.currentQuat.rotationYXZ((float) (seconds * .6), (float) (.25 * Math.sin(seconds * .5)),
                    (float) (.35 * Math.sin(seconds * .7)));
            GlobalVariables.prevQuat.set(GlobalVariables.currentQuat);
        }
        if (seconds >= 7) {
            previousFrame = 0;
            if (sampledAt == 0 || FRAMES.isEmpty()) throw new AssertionError("no frame samples");
            long bytes = ALLOCATIONS.getThreadAllocatedBytes(Thread.currentThread().threadId()) - sampledBytes;
            double elapsed = (now - sampledAt) / 1e9;
            long[] sorted = FRAMES.stream().mapToLong(Long::longValue).sorted().toArray();
            String name = new String[] {"stationaryAir", "rotatingAir", "rotatingLeaves"}[scene];
            var result = Map.of("frames", sorted.length, "seconds", elapsed, "fps", sorted.length / elapsed,
                    "p95FrameMs", sorted[(int) (sorted.length * .95)] / 1e6,
                    "renderThreadAllocatedMBps", bytes / elapsed / 1_000_000, "ticks", ticks);
            RESULTS.put(name, result);
            com.mojang.logging.LogUtils.getLogger().info("PHYSICS_CLIENT_PERF scene={} stats={}", name, result);
            scene++;
            sampledAt = sampledBytes = 0;
            FRAMES.clear();
            ticks = 0;
            sceneStarted = now;
            if (scene == 3) {
                var directory = mc.gameDirectory.toPath().resolve("..");
                Files.writeString(directory.resolve("performance.json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(RESULTS));
                recording.stop();
                recording.dump(directory.resolve("performance.jfr"));
                recording.close();
                return true;
            }
            if (scene == 2) {
                ready = false;
                GlobalVariables.currentQuat.identity();
                GlobalVariables.prevQuat.identity();
                trunk.constrainTo(new Quaternionf());
                var upright = SegmentedHitboxPose.standing(new Quaternionf());
                mc.player.getData(main.PLAYER_ROTATION).setHitboxPose(upright);
                var id = mc.player.getUUID();
                mc.getSingleplayerServer().execute(() -> {
                    var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                    player.getData(main.PLAYER_ROTATION).setHitboxPose(upright);
                    player.connection.teleport(.5, 84, .5, 0, 0);
                    for (int x : new int[] {-1, 1}) for (int y = 82; y <= 87; y++) for (int z = -2; z <= 2; z++)
                        player.serverLevel().setBlockAndUpdate(new BlockPos(x, y, z), Blocks.OAK_LEAVES.defaultBlockState());
                    ready = true;
                });
            }
        }
        return false;
    }

    private static void frame(RenderFrameEvent.Post event) {
        long now = System.nanoTime();
        if (scene >= 3 || !ready || now - sceneStarted < 2_000_000_000L) { previousFrame = 0; return; }
        if (sampledAt == 0) {
            sampledAt = now;
            sampledBytes = ALLOCATIONS.getThreadAllocatedBytes(Thread.currentThread().threadId());
        }
        if (previousFrame != 0) FRAMES.add(now - previousFrame);
        previousFrame = now;
    }
}
