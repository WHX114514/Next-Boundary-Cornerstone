package cn.rbq108.nextboundarycornerstone.qa;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxPose;
import cn.rbq108.nextboundarycornerstone.body.OOBB.HitboxConfig;
import cn.rbq108.nextboundarycornerstone.body.collision.SegmentedPhysics;
import cn.rbq108.nextboundarycornerstone.core.Keybinds;
import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.qa.mixin.EyeOverlayAccessor;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Quaternionf;

import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/** Contact, motion and head turn against real walls, with positive embedded-eye controls. */
final class EyeWallClientQa {
    private static int scene = -1, age;
    private static volatile boolean ready;
    private static volatile Throwable failure;
    private static final Map<String, Integer> frames = new LinkedHashMap<>();
    private static final String[] NAMES = {"face-wall", "back-wall", "side-wall", "wall-head-turn", "rolled-wall", "embedded-eye"};

    static void start() { NeoForge.EVENT_BUS.addListener(EyeWallClientQa::frame); }

    static boolean tick(Minecraft mc, int ticks) throws java.io.IOException {
        if (failure != null) throw new IllegalStateException("wall eye check", failure);
        if (ticks < 60) return false;
        var expected = System.getProperty("cornerstone.hitbox.visual.expectedMode", "");
        require(expected.isBlank() || HitboxConfig.mode().name().equals(expected), "actual server hitbox mode matches expectation");
        if (scene < 0) setup(mc, 0);
        if (!ready) return false;
        age++;
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        if (scene < 5 && age < 65) mc.player.move(MoverType.SELF, scene == 2 ? new Vec3(.04, 0, 0)
                : new Vec3(0, 0, scene == 1 ? -.04 : .04));
        if (scene == 3 && age == 65) Keybinds.B_FREE_CAMERA.setDown(true);
        if (scene == 3 && age == 75) mc.player.turn(220, 100);
        if (age == 115) {
            require(frames.getOrDefault(NAMES[scene], 0) > 10, "render checks ran for " + NAMES[scene]);
            try (var frame = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                frame.writeToFile(mc.gameDirectory.toPath().resolve("../" + NAMES[scene] + ".png"));
            }
            Keybinds.B_FREE_CAMERA.setDown(false);
            if (scene == 5) {
                Files.writeString(mc.gameDirectory.toPath().resolve("../eye-wall-frames.json"),
                        new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(frames));
                com.mojang.logging.LogUtils.getLogger().info("EYE_WALL_CLIENT_PASS frames={}", frames);
                return true;
            }
            setup(mc, scene + 1);
        }
        return false;
    }

    private static void setup(Minecraft mc, int next) {
        ready = false; scene = next; age = 0;
        GlobalVariables.currentQuat.identity();
        if (scene == 4) GlobalVariables.currentQuat.rotateZ(.7f);
        GlobalVariables.prevQuat.set(GlobalVariables.currentQuat);
        mc.player.setYRot(0); mc.player.yRotO = 0; mc.player.setXRot(0); mc.player.xRotO = 0;
        var id = mc.player.getUUID();
        mc.getSingleplayerServer().execute(() -> {
            try {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                var level = player.serverLevel();
                for (var pos : BlockPos.betweenClosed(-3, 82, -3, 3, 88, 3)) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                var min = new BlockPos(scene == 2 ? 1 : -3, 82, scene == 1 ? -1 : scene == 2 ? -3 : 1);
                var max = new BlockPos(scene == 2 ? 1 : 3, 88, scene == 1 ? -1 : scene == 2 ? 3 : 1);
                for (var pos : BlockPos.betweenClosed(min, max)) level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
                player.getData(main.PLAYER_ROTATION).setHitboxPose(SegmentedHitboxPose.standing(new Quaternionf(GlobalVariables.currentQuat)));
                player.connection.teleport(.5, 84, .5, 0, 0);
                if (scene == 5) {
                    // Deliberate external placement inside a block: detection must stay enabled.
                    player.connection.teleport(.5, 84, 1.5, 0, 0);
                }
                ready = true;
            } catch (Throwable error) { failure = error; }
        });
    }

    private static void frame(RenderFrameEvent.Post event) {
        if (!ready || age < 20 || age > 115 || failure != null) return;
        try {
            var mc = Minecraft.getInstance();
            var camera = mc.gameRenderer.getMainCamera();
            Vec3 eye = mc.player.getEyePosition(camera.getPartialTickTime());
            require(camera.getPosition().distanceTo(eye) < 1e-5, "camera stays at eye anchor");
            var overlay = EyeOverlayAccessor.overlay(mc.player);
            if (scene == 5) require(overlay != null && mc.player.isInWall(), "embedded eye still receives overlay and suffocation");
            else {
                require(mc.level.noCollision(mc.player), "physical head remains outside wall");
                require(!mc.level.getBlockState(BlockPos.containing(eye)).is(Blocks.STONE), "actual camera eye is outside stone");
                var near = camera.getNearPlane();
                for (var point : new Vec3[]{near.getTopLeft(), near.getTopRight(), near.getBottomLeft(), near.getBottomRight(), near.getPointOnPlane(0, 0)})
                    require(!mc.level.getBlockState(BlockPos.containing(eye.add(point))).is(Blocks.STONE), "near plane stays outside wall");
                require(overlay == null, "false block overlay at " + NAMES[scene] + " eye=" + eye + " overlay=" + overlay);
                require(!mc.player.isInWall(), "false suffocation at " + NAMES[scene]);
                require(SegmentedPhysics.box(mc.player).pieces().size() == 10, "unchanged physical model");
            }
            frames.merge(NAMES[scene], 1, Integer::sum);
        } catch (Throwable error) {
            failure = error;
            var mc = Minecraft.getInstance();
            try (var frame = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                frame.writeToFile(mc.gameDirectory.toPath().resolve("../failure-wall.png"));
            } catch (Throwable ignored) {}
        }
    }

    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
