package cn.rbq108.nextboundarycornerstone.qa;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.debug;
import cn.rbq108.nextboundarycornerstone.body.SegmentedAABB;
import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxes;
import cn.rbq108.nextboundarycornerstone.body.OOBB.HitboxConfig;
import cn.rbq108.nextboundarycornerstone.body.OOBB.HitboxMode;
import cn.rbq108.nextboundarycornerstone.body.OOBB.OBBHitbox;
import cn.rbq108.nextboundarycornerstone.body.collision.SegmentedPhysics;
import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.nio.file.Files;
import java.util.Map;

/** Isolated background visual fixture, using the actual renderer and Minecraft framebuffer. */
public final class HitboxVisualQa {
    private static int phase, ticks, scenario;
    private static volatile boolean ready;
    private static volatile Throwable failure;
    private static final long STARTED = System.currentTimeMillis();
    private static final boolean SIDE_ONLY = "side".equals(System.getProperty("cornerstone.hitbox.visual.scenario"));
    private static final boolean ROLLED_ONLY = "rolled".equals(System.getProperty("cornerstone.hitbox.visual.scenario"));
    private static final boolean TACZ_ONLY = "tacz".equals(System.getProperty("cornerstone.hitbox.visual.scenario"));
    private static final boolean PHYSICS_ONLY = "physics".equals(System.getProperty("cornerstone.hitbox.visual.scenario"));
    private static final boolean HUD_ONLY = "collision-hud".equals(System.getProperty("cornerstone.hitbox.visual.scenario"));
    private static final boolean POSTURE_ONLY = "posture".equals(System.getProperty("cornerstone.hitbox.visual.scenario"));
    private static final boolean PERFORMANCE_ONLY = "performance".equals(System.getProperty("cornerstone.hitbox.visual.scenario"));
    private static final boolean EYES_ONLY = "eyes".equals(System.getProperty("cornerstone.hitbox.visual.scenario"));
    private static final boolean EYE_WALL_ONLY = "eye-wall".equals(System.getProperty("cornerstone.hitbox.visual.scenario"));

    private HitboxVisualQa() {}

    public static void start() {
        NeoForge.EVENT_BUS.addListener(HitboxVisualQa::tick);
        if (EYES_ONLY) EyesClientQa.start();
        if (EYE_WALL_ONLY) EyeWallClientQa.start();
    }

    private static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (phase == 99) return;
        try {
            if (failure != null) throw new IllegalStateException("visual fixture", failure);
            if (phase < 2 && System.currentTimeMillis() - STARTED > 180_000) throw new IllegalStateException("world startup timeout");
            if (phase == 0) {
                mc.options.pauseOnLostFocus = false;
                mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
                if (mc.screen != null && mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")) {
                    mc.setScreen(new TitleScreen());
                }
                if (mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                    phase = 1;
                    // Copy a real per-world server file; expectedMode below is only an assertion, never a mode override.
                    String worldName = "hitbox-visual-" + STARTED;
                    var serverTemplate = mc.gameDirectory.toPath().resolve("../server-config.toml");
                    if (Files.exists(serverTemplate)) {
                        var serverDir = mc.gameDirectory.toPath().resolve("saves").resolve(worldName).resolve("serverconfig");
                        Files.createDirectories(serverDir);
                        Files.copy(serverTemplate, serverDir.resolve(HitboxConfig.FILE_NAME));
                    }
                    GameRules rules = new GameRules();
                    rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
                    rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
                    mc.createWorldOpenFlows().createFreshLevel(worldName,
                            new LevelSettings("分段受击盒视觉验收", GameType.CREATIVE, false, Difficulty.PEACEFUL,
                                    true, rules, WorldDataConfiguration.DEFAULT),
                            new WorldOptions(20261112L, false, false),
                            registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                                    .value().createWorldDimensions(), mc.screen);
                }
            } else if (phase == 1 && mc.player != null && mc.screen == null && mc.getSingleplayerServer() != null) {
                phase = 2;
                debug.FORCE_LOW_GRAVITY = true;
                var id = mc.player.getUUID();
                mc.getSingleplayerServer().execute(() -> {
                    try {
                        var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                        var level = player.serverLevel();
                        level.setDayTime(6000);
                        for (BlockPos pos : BlockPos.betweenClosed(-10, 80, -10, 10, 80, 10)) {
                            level.setBlockAndUpdate(pos, ((pos.getX() + pos.getZ()) & 1) == 0
                                    ? Blocks.SMOOTH_QUARTZ.defaultBlockState() : Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
                        }
                        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(main.BASIC_BACKPACK.get()));
                        player.setNoGravity(true);
                        player.connection.teleport(.5, 84, .5, 0, 0);
                        player.inventoryMenu.broadcastChanges();
                        ready = true;
                    } catch (Throwable error) { failure = error; }
                });
            } else if (phase == 2 && ready && mc.player != null && mc.screen == null) {
                ticks++;
                GlobalVariables.B_Vx1 = GlobalVariables.B_Vy1 = GlobalVariables.B_Vz1 = 0;
                mc.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                if (ticks == 40) {
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                    mc.getEntityRenderDispatcher().setRenderHitBoxes(true);
                    if (SIDE_ONLY) posture(mc, 2);
                    else if (ROLLED_ONLY || TACZ_ONLY) posture(mc, 1);
                }
                if (EYE_WALL_ONLY) {
                    if (EyeWallClientQa.tick(mc, ticks)) finish(mc, "eye-wall-result.json");
                } else if (EYES_ONLY) {
                    EyesClientQa.tick(mc, ticks);
                    if (ticks == 160) capture(mc, "11-eyes-upright");
                    else if (ticks == 250) capture(mc, "12-eyes-rolled");
                    else if (ticks == 360) capture(mc, "13-eyes-head-turn");
                    else if (ticks == 500) capture(mc, "14-eyes-floating");
                    else if (ticks == 650) capture(mc, "15-eyes-third-person");
                    else if (ticks == 720) capture(mc, "16-eyes-wall");
                    else if (ticks == 740) finish(mc, "eyes-result.json");
                } else if (PERFORMANCE_ONLY) {
                    if (ticks >= 60 && PhysicsClientPerformance.tick(mc)) finish(mc, "performance-result.json");
                } else if (PHYSICS_ONLY || HUD_ONLY) {
                    physicsScenario(mc);
                    if (HUD_ONLY) collisionHudScenario(mc);
                } else if (POSTURE_ONLY) {
                    if (ticks == 100) { checkPosture(mc, false); capture(mc, "08-standing"); }
                    else if (ticks == 110) posture(mc, 2);
                    else if (ticks == 190) { checkPosture(mc, true); capture(mc, "09-floating"); }
                    else if (ticks == 200) posture(mc, 0);
                    else if (ticks == 280) { checkPosture(mc, false); capture(mc, "10-standing-restored"); }
                    else if (ticks == 300) finish(mc, "posture-result.json");
                } else if (SIDE_ONLY || ROLLED_ONLY || TACZ_ONLY) {
                    if (TACZ_ONLY && ticks == 80) TACZClientQa.verify(mc);
                    if (SIDE_ONLY && ticks == 60) {
                        // Use the mod's free-camera separation: body stays horizontal, camera views its side.
                        GlobalVariables.B_Dx = GlobalVariables.B_Dy = GlobalVariables.B_Dz = 0;
                        GlobalVariables.B_FreeCameraToggle = true;
                        GlobalVariables.B_HeadRotationLocked = true;
                        GlobalVariables.lockedHeadRelQuat.identity();
                        GlobalVariables.currentQuat.rotationY(-(float) Math.PI / 2);
                        GlobalVariables.prevQuat.set(GlobalVariables.currentQuat);
                        mc.player.setYRot(90); mc.player.yRotO = 90;
                        mc.player.setXRot(0); mc.player.xRotO = 0;
                    } else if (ticks == 140) capture(mc, SIDE_ONLY ? "04-floating-side" : "02-rolled");
                    else if (ticks == 160) finish(mc, SIDE_ONLY ? "side-result.json" : "rolled-result.json");
                } else {
                    if (ticks == 100) capture(mc, "01-upright");
                    else if (ticks == 110) posture(mc, 1);
                    else if (ticks == 190) capture(mc, "02-rolled");
                    else if (ticks == 200) posture(mc, 2);
                    else if (ticks == 280) capture(mc, "03-floating");
                    else if (ticks == 300) finish(mc, "capture-result.json");
                }
                if (ticks % 20 == 0 && SegmentedHitboxes.enabled(mc.player)) {
                    var boxes = ((SegmentedAABB) SegmentedHitboxes.forHitQuery(mc.player)).pieces();
                    var status = Map.of("scenario", scenario, "noGravity", mc.player.isNoGravity(), "pose", mc.player.getPose().toString(),
                            "camera", mc.options.getCameraType().toString(), "debugBoxes", mc.getEntityRenderDispatcher().shouldRenderHitBoxes(),
                            "language", mc.options.languageCode, "mode", HitboxConfig.mode().name(), "pieces", boxes.size(),
                            "collisionPieces", SegmentedPhysics.box(mc.player).pieces().size(), "boxes", boxes.stream().map(Object::toString).toList());
                    Files.writeString(mc.gameDirectory.toPath().resolve("visual-status.json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(status));
                }
            }
        } catch (Throwable error) {
            com.mojang.logging.LogUtils.getLogger().error("HITBOX_VISUAL_QA_FAILED", error);
            try {
                Files.writeString(mc.gameDirectory.toPath().resolve("../failure.txt"), error.toString());
            } catch (java.io.IOException writeFailure) {
                error.addSuppressed(writeFailure);
            }
            phase = 99;
            mc.stop();
        }
    }

    static void posture(Minecraft mc, int nextScenario) {
        scenario = nextScenario;
        GlobalVariables.currentQuat.identity();
        if (scenario == 1) GlobalVariables.currentQuat.rotateXYZ(.25f, .35f, .7f);
        GlobalVariables.prevQuat.set(GlobalVariables.currentQuat);
        var id = mc.player.getUUID();
        boolean swimming = scenario == 2;
        mc.getSingleplayerServer().execute(() -> {
            var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
            player.setItemSlot(EquipmentSlot.CHEST, swimming ? ItemStack.EMPTY : new ItemStack(main.BASIC_BACKPACK.get()));
            player.inventoryMenu.broadcastChanges();
        });
        mc.player.displayClientMessage(Component.literal("验收姿态：" + (swimming ? "无背包漂浮" : scenario == 1 ? "倾斜翻滚" : "背包直立")), true);
    }

    private static void checkPosture(Minecraft mc, boolean floating) {
        var pose = mc.player.getData(main.PLAYER_ROTATION).poseOrDefault();
        require(pose.swimming() == floating && (floating ? pose.swim() > .99 : pose.swim() < .01),
                "accepted physical posture follows equipment on client");
        require(mc.level.noCollision(mc.player), "posture transition leaves physical body clear");
        var physical = SegmentedPhysics.box(mc.player);
        require(physical.pieces().size() == 10 && physical.tags().equals(cn.rbq108.nextboundarycornerstone.body.BodyBoxTag.MOVEMENT),
                "movement contains tagged head, torso and complete legs only");
        var fullHit = (SegmentedAABB) SegmentedHitboxes.forHitQuery(mc.player);
        require(fullHit.pieces().size() == 16 && fullHit.tags().equals(cn.rbq108.nextboundarycornerstone.body.BodyBoxTag.ALL),
                "all hit pieces and tags survive posture changes");
        var id = mc.player.getUUID();
        mc.getSingleplayerServer().execute(() -> {
            try {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                var serverPose = player.getData(main.PLAYER_ROTATION).poseOrDefault();
                require(serverPose.swimming() == floating && (floating ? serverPose.swim() > .99 : serverPose.swim() < .01),
                        "server receives matching physical posture");
                require(player.serverLevel().noCollision(player), "server posture remains collision-free");
            } catch (Throwable error) { failure = error; }
        });
    }

    private static void capture(Minecraft mc, String name) throws java.io.IOException {
        String expectation = System.getProperty("cornerstone.hitbox.visual.expectedMode", "");
        HitboxMode expected = expectation.isBlank() ? HitboxConfig.mode() : HitboxMode.valueOf(expectation);
        if ((!EYES_ONLY && !HUD_ONLY && mc.options.getCameraType().isFirstPerson()) || !mc.player.isNoGravity()
                || ((SegmentedAABB) SegmentedHitboxes.forHitQuery(mc.player)).pieces().size() != 16
                || SegmentedPhysics.box(mc.player).pieces().size() != 10
                || !HitboxConfig.SPEC.isLoaded() || HitboxConfig.mode() != expected
                || (SegmentedHitboxes.forHitQuery(mc.player) instanceof OBBHitbox) != (expected == HitboxMode.OBB)) {
            throw new IllegalStateException("Visual fixture not synchronized: " + name);
        }
        try (var frame = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            frame.writeToFile(mc.gameDirectory.toPath().resolve("../" + name + ".png"));
        }
        Files.copy(mc.gameDirectory.toPath().resolve("visual-status.json"),
                mc.gameDirectory.toPath().resolve("../" + name + ".json"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private static void finish(Minecraft mc, String file) throws java.io.IOException {
        Files.writeString(mc.gameDirectory.toPath().resolve("../" + file),
                "{\"capturesComplete\":true,\"language\":\"zh_cn\",\"camera\":\"" + mc.options.getCameraType()
                        + "\",\"pieces\":16,\"collisionPieces\":10,\"desktopInput\":false,\"mode\":\"" + HitboxConfig.mode() + "\"}");
        com.mojang.logging.LogUtils.getLogger().info("HITBOX_VISUAL_CAPTURES_COMPLETE mode={}", HitboxConfig.mode());
        phase = 99;
        mc.stop();
    }

    private static void physicsScenario(Minecraft mc) throws java.io.IOException {
        if (ticks == 40) physicsScene(mc, 0);
        else if (ticks == 100) {
            require(ready && mc.level.getBlockState(new BlockPos(-1, 84, 0)).is(Blocks.STONE), "left wall synchronized");
            GlobalVariables.currentQuat.identity().rotateZ((float) Math.PI / 4);
            GlobalVariables.prevQuat.set(GlobalVariables.currentQuat);
        } else if (ticks == 200) {
            var diagnostic = mc.player.getData(main.PLAYER_ROTATION);
            com.mojang.logging.LogUtils.getLogger().info("COLLISION_LOOK_NUDGE_DIAG body={} view={} head={} active={} look={},{},{} pos={} blocked={} trunk={},{},{}",
                    diagnostic.poseOrDefault().body(), GlobalVariables.currentQuat, diagnostic.poseOrDefault().joints().getFirst().rotation(),
                    GlobalVariables.B_CollisionFreeLookActive, GlobalVariables.collisionLookYaw, GlobalVariables.collisionLookPitch,
                    GlobalVariables.collisionLookRoll, mc.player.position(), diagnostic.isBodyBlocked(),
                    cn.rbq108.nextboundarycornerstone.body.trunk.smoothX, cn.rbq108.nextboundarycornerstone.body.trunk.smoothY, cn.rbq108.nextboundarycornerstone.body.trunk.smoothZ);
            require(mc.player.getX() > .7, "real client moved sideways to make room for body turn");
            require(!mc.player.getData(main.PLAYER_ROTATION).isBodyBlocked(), "body turn completes when there is space");
            require(mc.level.noCollision(mc.player), "nudged compound body is not penetrating wall");
            capture(mc, "05-turn-nudge");
            serverBodyCheck(mc, false);
        } else if (ticks == 220) physicsScene(mc, 1);
        else if (ticks == 280) {
            require(ready && Math.abs(mc.player.getData(main.PLAYER_ROTATION).poseOrDefault().body().w) > .999,
                    "body returned upright before narrow-space fixture");
            physicsScene(mc, 2);
        } else if (ticks == 330) {
            require(ready && mc.level.getBlockState(new BlockPos(0, 84, 0)).is(Blocks.GLASS_PANE), "narrow corridor synchronized");
            require(!mc.level.getBlockState(new BlockPos(0, 84, 0)).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.NORTH)
                    && !mc.level.getBlockState(new BlockPos(0, 84, -1)).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SOUTH),
                    "fixture panes do not bridge across the corridor");
            require(mc.level.noCollision(mc.player), "upright body fits corridor before rotation request");
            GlobalVariables.currentQuat.rotationX((float) Math.PI / 2);
            GlobalVariables.prevQuat.set(GlobalVariables.currentQuat);
            mc.player.setXRot(90); mc.player.xRotO = 90;
        } else if (ticks == 420) {
            var pose = mc.player.getData(main.PLAYER_ROTATION).poseOrDefault();
            var sampled = cn.rbq108.nextboundarycornerstone.body.ClientSegmentedHitboxes.sample(mc.player);
            com.mojang.logging.LogUtils.getLogger().info("PHYSICAL_DIAG body={} head={} camera={} rel={} sampledHead={} position={} bodyBlocked={}",
                    pose.body(), pose.joints().getFirst().rotation(), GlobalVariables.currentQuat, GlobalVariables.headRelQuat,
                    sampled.joints().getFirst().rotation(), mc.player.position(), mc.player.getData(main.PLAYER_ROTATION).isBodyBlocked());
            capture(mc, "06-cramped-head-only");
            require(mc.player.getData(main.PLAYER_ROTATION).isBodyBlocked(), "real client holds body at collision limit");
            require(Math.abs(pose.body().dot(GlobalVariables.currentQuat)) < .98, "camera continues independently of blocked body");
            require(Math.abs(pose.joints().getFirst().rotation().w) < .99, "head turns relative to body");
            require(Math.abs(GlobalVariables.currentQuat.dot(new org.joml.Quaternionf().rotateX((float) Math.PI / 2))) > .999,
                    "collision constraint does not reset camera");
            require(!GlobalVariables.B_FreeCameraToggle, "head-only fallback does not toggle user's C mode");
            require(mc.level.noCollision(mc.player), "blocked physical body is nonpenetrating");
            serverBodyCheck(mc, true);
            exerciseLookInputs(mc);
        } else if (ticks >= 425 && ticks <= 450) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.player.turn(0, 2000);
            if (ticks == 450) {
                require(GlobalVariables.B_CollisionFreeLookActive && Math.abs(GlobalVariables.collisionLookPitch - 90) < .001,
                        "first-person pitch remains limited across real physics and rendering ticks");
                require(!GlobalVariables.B_HeadRotationLocked && !GlobalVariables.B_FreeCameraToggle,
                        "automatic look never enables manual fixed-head mode");
                try (var frame = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                    frame.writeToFile(mc.gameDirectory.toPath().resolve("../08-limited-first-person.png"));
                }
                com.mojang.logging.LogUtils.getLogger().info("COLLISION_FREE_LOOK_FRAME_PASS pitch=90 ticks=26 headLocked=false");
            }
        } else if (ticks == 451) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        else if (ticks == 480) physicsScene(mc, 3);
        else if (ticks == 590) {
            var pose = mc.player.getData(main.PLAYER_ROTATION).poseOrDefault();
            require(!mc.player.getData(main.PLAYER_ROTATION).isBodyBlocked()
                    && Math.abs(pose.body().dot(GlobalVariables.currentQuat)) > .999, "body resumes after obstruction removed");
            require(!GlobalVariables.B_CollisionFreeLookActive, "automatic free look exits when body can turn again");
            capture(mc, "07-body-resumed");
        } else if (ticks == 610 && !HUD_ONLY) finish(mc, "physics-result.json");
    }

    private static void collisionHudScenario(Minecraft mc) throws java.io.IOException {
        if (ticks == 451) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            require(cn.rbq108.nextboundarycornerstone.camera.CollisionFreeLook.turn(0, -30,
                    25 - GlobalVariables.collisionLookRoll), "real constrained view accepts roll input");
        } else if (ticks == 459) {
            require(GlobalVariables.B_CollisionFreeLookActive, "positive roll is shown in automatic collision look");
            capture(mc, "09-hud-roll-positive");
        } else if (ticks == 460) cn.rbq108.nextboundarycornerstone.camera.CollisionFreeLook.turn(0, 0,
                -25 - GlobalVariables.collisionLookRoll);
        else if (ticks == 475) capture(mc, "10-hud-roll-negative");
        else if (ticks == 480) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        else if (ticks == 595) mc.options.setCameraType(CameraType.FIRST_PERSON);
        else if (ticks == 598) capture(mc, "11-hud-space-restored");
        else if (ticks == 600) cn.rbq108.nextboundarycornerstone.core.Keybinds.B_FREE_CAMERA.setDown(true);
        else if (ticks == 615) mc.player.turn(30, 20);
        else if (ticks == 640) {
            require(GlobalVariables.B_FreeCameraActive && !GlobalVariables.B_CollisionFreeLookActive,
                    "manual free look does not enter collision HUD mode");
            capture(mc, "12-hud-manual-free-look");
        } else if (ticks == 650) cn.rbq108.nextboundarycornerstone.core.Keybinds.B_FREE_CAMERA.setDown(false);
        else if (ticks == 680) finish(mc, "collision-hud-result.json");
    }

    private static void physicsScene(Minecraft mc, int scene) {
        ready = false;
        if (scene < 2) {
            GlobalVariables.currentQuat.identity();
            GlobalVariables.prevQuat.identity();
            mc.player.setYRot(0); mc.player.yRotO = 0;
            mc.player.setXRot(0); mc.player.xRotO = 0;
        }
        var id = mc.player.getUUID();
        mc.getSingleplayerServer().execute(() -> {
            try {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                var level = player.serverLevel();
                for (BlockPos pos : BlockPos.betweenClosed(-2, 82, -2, 2, 87, 2)) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                if (scene < 3) player.connection.teleport(scene == 0 ? .55 : .5, 84, scene == 2 ? 0 : .5, 0, 0);
                if (scene == 0) {
                    for (BlockPos pos : BlockPos.betweenClosed(-1, 82, -2, -1, 87, 2)) level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
                } else if (scene == 2) {
                    var pane = Blocks.GLASS_PANE.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST, true)
                            .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WEST, true);
                    for (int x = -2; x <= 2; x++) for (int y = 82; y <= 87; y++) {
                        // Keep the prescribed east/west pane shape; neighbor updates must not bridge the corridor.
                        //上下翻了大半天实在没看懂这是在干什么
                        level.setBlock(new BlockPos(x, y, -1), pane, 2 | 16);
                        level.setBlock(new BlockPos(x, y, 0), pane, 2 | 16);
                    }
                }
                ready = true;
            } catch (Throwable error) { failure = error; }
        });
    }

    private static void exerciseLookInputs(Minecraft mc) {
        require(GlobalVariables.B_CollisionFreeLookActive && !GlobalVariables.B_HeadRotationLocked,
                "blocked body enters ordinary automatic free look, not fixed-head orbit");
        var cameraType = mc.options.getCameraType();
        var view = new org.joml.Quaternionf(GlobalVariables.currentQuat);
        var previousView = new org.joml.Quaternionf(GlobalVariables.prevQuat);
        float x = mc.player.getXRot(), y = mc.player.getYRot(), oldX = mc.player.xRotO, oldY = mc.player.yRotO;
        float freeYaw = GlobalVariables.B_freeLookYaw, freePitch = GlobalVariables.B_freeLookPitch;
        float mouseX = GlobalVariables.mouseDeltaX, mouseY = GlobalVariables.mouseDeltaY;
        try {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            for (int i = 0; i < 20; i++) mc.player.turn(2000, 2000);
            require(Math.abs(GlobalVariables.collisionLookYaw - 110) < .001
                    && Math.abs(GlobalVariables.collisionLookPitch - 90) < .001, "actual mouse path saturates instead of orbiting without limits");
            mc.player.turn(-20, -20);
            require(Math.abs(GlobalVariables.collisionLookYaw - 107) < .001
                    && Math.abs(GlobalVariables.collisionLookPitch - 87) < .001, "reverse mouse input immediately leaves collision look limit");
            GlobalVariables.B_FreeCameraActive = true;
            GlobalVariables.B_freeLookYaw = GlobalVariables.B_freeLookPitch = 0;
            mc.player.turn(2000, 2000);
            require(GlobalVariables.B_freeLookYaw == 110 && GlobalVariables.B_freeLookPitch == 90,
                    "manual ordinary C look retains its existing limits");
            GlobalVariables.B_HeadRotationLocked = true;
            var expected = new org.joml.Quaternionf(GlobalVariables.currentQuat).rotateX((float) Math.toRadians(300));
            mc.player.turn(0, 2000);
            require(Math.abs(expected.dot(GlobalVariables.currentQuat)) > .99999,
                    "explicit fixed-head mode still uses unrestricted quaternion camera rotation");
            com.mojang.logging.LogUtils.getLogger().info("COLLISION_FREE_LOOK_INPUT_PASS firstPersonYaw=110 pitch=90 reverseYaw=107 reversePitch=87 manualModesPreserved=true");
        } finally {
            GlobalVariables.B_FreeCameraActive = false;
            GlobalVariables.B_HeadRotationLocked = false;
            GlobalVariables.B_freeLookYaw = freeYaw;
            GlobalVariables.B_freeLookPitch = freePitch;
            GlobalVariables.mouseDeltaX = mouseX;
            GlobalVariables.mouseDeltaY = mouseY;
            mc.player.setXRot(x); mc.player.setYRot(y); mc.player.xRotO = oldX; mc.player.yRotO = oldY;
            mc.options.setCameraType(cameraType);
            GlobalVariables.currentQuat.set(view);
            GlobalVariables.prevQuat.set(previousView);
            cn.rbq108.nextboundarycornerstone.camera.CollisionFreeLook.accept(mc.player.getData(main.PLAYER_ROTATION).poseOrDefault(), true);
        }
    }

    private static void serverBodyCheck(Minecraft mc, boolean constrained) {
        var id = mc.player.getUUID();
        mc.getSingleplayerServer().execute(() -> {
            try {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                require(SegmentedPhysics.box(player).pieces().size() == 10 && player.serverLevel().noCollision(player), "server physical body matches clear client geometry");
                if (constrained) require(Math.abs(player.getData(main.PLAYER_ROTATION).poseOrDefault().body()
                        .dot(player.getData(main.PLAYER_ROTATION).getQuaternion())) < .98, "server receives head-only body/view separation");
            } catch (Throwable error) { failure = error; }
        });
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
