package cn.rbq108.nextboundarycornerstone.qa;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.debug;
import cn.rbq108.nextboundarycornerstone.body.SegmentedAABB;
import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxes;
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

    private HitboxVisualQa() {}

    public static void start() {
        NeoForge.EVENT_BUS.addListener(HitboxVisualQa::tick);
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
                    GameRules rules = new GameRules();
                    rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
                    rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
                    mc.createWorldOpenFlows().createFreshLevel("hitbox-visual-" + STARTED,
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
                }
                if (SIDE_ONLY) {
                    if (ticks == 60) {
                        // Use the mod's free-camera separation: body stays horizontal, camera views its side.
                        GlobalVariables.B_Dx = GlobalVariables.B_Dy = GlobalVariables.B_Dz = 0;
                        GlobalVariables.B_FreeCameraToggle = true;
                        GlobalVariables.B_HeadRotationLocked = true;
                        GlobalVariables.lockedHeadRelQuat.identity();
                        GlobalVariables.currentQuat.rotationY(-(float) Math.PI / 2);
                        GlobalVariables.prevQuat.set(GlobalVariables.currentQuat);
                        mc.player.setYRot(90); mc.player.yRotO = 90;
                        mc.player.setXRot(0); mc.player.xRotO = 0;
                    } else if (ticks == 140) capture(mc, "04-floating-side");
                    else if (ticks == 160) finish(mc, "side-result.json");
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
                            "language", mc.options.languageCode, "pieces", boxes.size(), "boxes", boxes.stream().map(Object::toString).toList());
                    Files.writeString(mc.gameDirectory.toPath().resolve("visual-status.json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(status));
                }
            }
        } catch (Throwable error) {
            com.mojang.logging.LogUtils.getLogger().error("HITBOX_VISUAL_QA_FAILED", error);
            phase = 99;
            mc.stop();
        }
    }

    private static void posture(Minecraft mc, int nextScenario) {
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

    private static void capture(Minecraft mc, String name) throws java.io.IOException {
        if (mc.options.getCameraType().isFirstPerson() || !mc.player.isNoGravity()
                || ((SegmentedAABB) SegmentedHitboxes.forHitQuery(mc.player)).pieces().size() != 16) {
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
                "{\"capturesComplete\":true,\"language\":\"zh_cn\",\"camera\":\"THIRD_PERSON_BACK\",\"pieces\":16,\"desktopInput\":false}");
        com.mojang.logging.LogUtils.getLogger().info("HITBOX_VISUAL_CAPTURES_COMPLETE");
        phase = 99;
        mc.stop();
    }
}
