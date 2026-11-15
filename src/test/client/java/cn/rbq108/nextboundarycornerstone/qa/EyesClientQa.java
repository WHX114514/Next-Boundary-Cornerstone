package cn.rbq108.nextboundarycornerstone.qa;

import cn.rbq108.nextboundarycornerstone.body.BodyEyes;
import cn.rbq108.nextboundarycornerstone.body.OOBB.OBBGeometry;
import cn.rbq108.nextboundarycornerstone.core.Keybinds;
import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/** Uses game input state and actual rendered camera frames; never sends desktop input. */
final class EyesClientQa {
    private static int tick;
    private static Throwable failure;
    private static final Map<String, Integer> frames = new LinkedHashMap<>();

    static void start() { NeoForge.EVENT_BUS.addListener(EyesClientQa::frame); }

    static void tick(Minecraft mc, int time) throws java.io.IOException {
        tick = time;
        if (failure != null) throw new IllegalStateException("eye camera frame", failure);
        if (time == 80) mc.options.setCameraType(CameraType.FIRST_PERSON);
        else if (time == 170) HitboxVisualQa.posture(mc, 1);
        else if (time == 270) Keybinds.B_FREE_CAMERA.setDown(true);
        else if (time == 282) mc.player.turn(300, 166.6667);
        else if (time == 360) require(Math.abs(mc.player.getData(main.PLAYER_ROTATION).poseOrDefault()
                .joints().getFirst().rotation().w) < .99, "independent head actually rotated");
        else if (time == 370) Keybinds.B_FREE_CAMERA.setDown(false);
        else if (time == 400) HitboxVisualQa.posture(mc, 2);
        else if (time == 510) HitboxVisualQa.posture(mc, 0);
        else if (time == 600) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        else if (time == 660) mc.getSingleplayerServer().execute(() -> {
            var level = mc.getSingleplayerServer().overworld();
            for (var pos : BlockPos.betweenClosed(-2, 82, -2, 2, 87, -2))
                level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        });
        else if (time == 740) {
            for (String stage : new String[]{"upright", "rolled", "head-turn", "floating", "third-person", "wall"})
                require(frames.getOrDefault(stage, 0) >= 10, "rendered eye frames present: " + stage);
            Files.writeString(mc.gameDirectory.toPath().resolve("../eye-frames.json"),
                    new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(frames));
            com.mojang.logging.LogUtils.getLogger().info("BODY_EYES_CLIENT_PASS frames={}", frames);
        }
    }

    private static void frame(RenderFrameEvent.Post event) {
        String stage = tick >= 110 && tick <= 160 ? "upright" : tick >= 210 && tick <= 250 ? "rolled"
                : tick >= 320 && tick <= 360 ? "head-turn" : tick >= 460 && tick <= 500 ? "floating"
                : tick >= 620 && tick <= 650 ? "third-person" : tick >= 700 && tick <= 720 ? "wall" : null;
        if (stage == null || failure != null) return;
        try {
            var mc = Minecraft.getInstance();
            var camera = mc.gameRenderer.getMainCamera();
            float partial = camera.getPartialTickTime();
            var eye = mc.player.getEyePosition(partial);
            var data = mc.player.getData(main.PLAYER_ROTATION);
            var pose = data.renderHitboxPose(partial);
            var origin = mc.player.getPosition(partial);
            var head = OBBGeometry.build(pose, mc.player.getBbHeight(), mc.player.getScale(), pose.swimming())
                    .getFirst().move(origin.x, origin.y, origin.z);
            require(head.contains(eye), "eye lies inside actual rendered head");
            require(eye.distanceTo(origin.add(BodyEyes.offset(pose, mc.player.getBbHeight(), mc.player.getScale()))) < 1e-6,
                    "ray origin follows interpolated head");
            double distance = camera.getPosition().distanceTo(eye);
            if (mc.options.getCameraType().isFirstPerson()) require(distance < 1e-5,
                    "actual first-person camera starts at eyes: " + distance);
            else if (stage.equals("wall")) {
                require(mc.level.getBlockState(new BlockPos(0, 85, -2)).is(Blocks.STONE), "rear wall synchronized");
                require(distance > .2 && distance < 2, "native third-person obstruction shortens eye-based boom: " + distance);
            } else require(distance > 3.9 && distance < 4.01, "third-person boom remains four blocks: " + distance);
            frames.merge(stage, 1, Integer::sum);
        } catch (Throwable error) { failure = error; }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
