package cn.rbq108.nextboundarycornerstone.qa;

import cn.rbq108.nextboundarycornerstone.attachment.PlayerRotationAttachment;
import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/** Actual local-player replacement and End portal transfers, with an invisible GLFW window. */
public final class PlayerLifecycleClientQa {
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();
    private final long started = System.currentTimeMillis();
    private int phase, ticks, transfers;
    private net.minecraft.client.player.LocalPlayer oldPlayer;
    private CompletableFuture<?> action;

    public PlayerLifecycleClientQa() { MinecraftForge.EVENT_BUS.addListener(this::tick); }

    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }

    private void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || phase == 99) return;
        var mc = Minecraft.getInstance();
        try {
            check(System.currentTimeMillis() - started < 240_000, "lifecycle QA timeout phase=" + phase);
            long window = mc.getWindow().getWindow();
            check(GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_VISIBLE) == GLFW.GLFW_FALSE, "window stays hidden");
            check(GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED) == GLFW.GLFW_FALSE, "window stays unfocused");
            if (mc.screen != null && mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")) mc.setScreen(new TitleScreen());
            if (phase == 0 && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                mc.options.pauseOnLostFocus = false;
                mc.options.framerateLimit().set(60);
                var rules = new GameRules();
                rules.getRule(GameRules.RULE_DO_IMMEDIATE_RESPAWN).set(true, null);
                rules.getRule(GameRules.RULE_KEEPINVENTORY).set(true, null);
                rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
                mc.createWorldOpenFlows().createFreshLevel("lifecycle-qa-" + started,
                        new net.minecraft.world.level.LevelSettings("Lifecycle QA", net.minecraft.world.level.GameType.SURVIVAL, false,
                                net.minecraft.world.Difficulty.PEACEFUL, true, rules, net.minecraft.world.level.WorldDataConfiguration.DEFAULT),
                        new net.minecraft.world.level.levelgen.WorldOptions(20261116L, false, false),
                        registries -> registries.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
                                .getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions());
                phase = 1;
            } else if (phase == 1 && mc.player != null && mc.screen == null && mc.getSingleplayerServer() != null) {
                var id = mc.player.getUUID();
                action = mc.getSingleplayerServer().submit(() -> {
                    var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                    player.teleportTo(10, 100, 10);
                    player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(main.BASIC_BACKPACK.get()));
                });
                phase = 2; ticks = 0;
            } else if (phase == 2 && ++ticks > 60 && action.isDone()) {
                action.join();
                check(mc.player.isNoGravity(), "death starts in zero gravity");
                cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.currentQuat.rotationXYZ(.3f, .4f, 1.1f);
                oldPlayer = mc.player;
                var id = mc.player.getUUID();
                action = mc.getSingleplayerServer().submit(() -> mc.getSingleplayerServer().getPlayerList().getPlayer(id).kill());
                phase = 3; ticks = 0;
            } else if (phase == 3) {
                if (++ticks < 40 || mc.player == null || mc.player == oldPlayer || !mc.player.isAlive()) return;
                action.join();
                // Exercise the exact old-camera-target path even after player replacement.
                var camera = new net.minecraft.client.Camera();
                camera.setup(oldPlayer.level(), oldPlayer, false, false, 1);
                camera.tick();
                check(Float.isFinite(oldPlayer.getEyeHeight()), "old death camera has a finite eye height");
                check(PlayerRotationAttachment.isAvailable(mc.player), "respawned player has live capability");
                LOG.info("PLAYER_LIFECYCLE_DEATH_PASS oldCamera=true respawn=true");
                phase = 4; ticks = 0;
            } else if (phase == 4 && ++ticks > 60) {
                check(mc.player.isNoGravity(), "portal starts in zero gravity");
                oldPlayer = mc.player;
                var id = mc.player.getUUID();
                action = mc.getSingleplayerServer().submit(() -> {
                    var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                    var level = player.serverLevel();
                    player.teleportTo(10.5, 100, 10.5);
                    // Invoke the real End portal block, covering its changeDimension/respawn paths.
                    for (BlockPos pos : BlockPos.betweenClosed(9, 99, 9, 11, 102, 11)) {
                        var state = Blocks.END_PORTAL.defaultBlockState();
                        level.setBlockAndUpdate(pos, state);
                        state.entityInside(level, pos, player);
                        if (player.level() != level || player.isRemoved()) break;
                    }
                });
                phase = 5; ticks = 0;
            } else if (phase == 5) {
                ticks++;
                if (mc.screen instanceof net.minecraft.client.gui.screens.WinScreen) mc.screen.onClose();
                var target = (transfers & 1) == 0 ? Level.END : Level.OVERWORLD;
                if (!action.isDone() || mc.player == null || mc.level.dimension() != target || ticks < 50 || mc.screen != null) return;
                action.join();
                check(PlayerRotationAttachment.isAvailable(mc.player), "destination player capability is available");
                check(Double.isFinite(mc.player.getEyePosition().y), "destination eye is finite");
                check(mc.player.isNoGravity(), "destination retains zero gravity");
                var playerId = mc.player.getUUID();
                mc.getSingleplayerServer().submit(() -> {
                    var player = mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);
                    check(PlayerRotationAttachment.isAvailable(player), "destination server capability is available");
                    check(player.getBoundingBox() instanceof cn.rbq108.nextboundarycornerstone.body.collision.PhysicsBox,
                            "destination server compound collision resumes");
                }).join();
                LOG.info("PLAYER_LIFECYCLE_PORTAL_PASS target={} transfer={}", target.location(), ++transfers);
                if (transfers < 4) { phase = 4; ticks = 0; return; }
                Files.writeString(Path.of("../client-report.txt"), "PASS zero-gravity death/respawn, old camera tick, 2 overworld-to-End and 2 End-to-overworld portals, live client/server capability and collision, hidden=true focused=false\n");
                LOG.info("PLAYER_LIFECYCLE_CLIENT_QA_PASS");
                phase = 99; mc.stop();
            }
        } catch (Throwable failure) {
            LOG.error("PLAYER_LIFECYCLE_CLIENT_QA_FAILED phase={} transfers={}", phase, transfers, failure);
            try { Files.writeString(Path.of("../client-report.txt"), "FAIL " + failure + "\n"); } catch (Exception ignored) {}
            phase = 99; mc.stop();
        }
    }
}
