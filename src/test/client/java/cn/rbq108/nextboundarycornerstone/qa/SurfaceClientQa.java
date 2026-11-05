package cn.rbq108.nextboundarycornerstone.qa;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.SurfaceConfig;
import cn.rbq108.nextboundarycornerstone.core.KeyPollingHandler;
import cn.rbq108.nextboundarycornerstone.core.Keybinds;
import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.motion.SurfaceAdhesion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3f;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Opt-in client loop. Default input is injected by a test-only Mixin, with no focus or OS input. */
@Mod(value = "cornerstone_surface_qa", dist = Dist.CLIENT)
public final class SurfaceClientQa {
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();
    private static final Direction[] FACES = {Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH};
    private static final List<String> CHECKS = new ArrayList<>();
    private static int phase, ticks, faceIndex;
    private static long started;
    private static volatile boolean serverReady;
    private static volatile Throwable serverFailure;
    private static boolean heldUp;

    public static boolean usesSimulatedInput() {
        return Boolean.getBoolean("cornerstone.surface.qa") && !Boolean.getBoolean("cornerstone.surface.physical");
    }

    public static boolean isHeld(net.minecraft.client.KeyMapping mapping) {
        return mapping == Keybinds.B_UP && heldUp;
    }

    public SurfaceClientQa() {
        if (Boolean.getBoolean("cornerstone.surface.qa")) NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, SurfaceClientQa::tick);
    }

    private static void tick(ClientTickEvent.Post event) {
        if (phase == 99) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (started == 0) {
                started = System.currentTimeMillis();
                mc.options.pauseOnLostFocus = false;
                mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
                mc.options.renderDistance().set(4);
                mc.options.simulationDistance().set(5);
            }
            require(System.currentTimeMillis() - started < 600_000, "client QA timeout phase=" + phase);
            if (serverFailure != null) throw new AssertionError("server fixture setup", serverFailure);
            ticks++;
            if (phase == 2) GlobalVariables.B_Vx1 = GlobalVariables.B_Vy1 = GlobalVariables.B_Vz1 = 0;
            if (mc.screen != null && mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")) {
                mc.setScreen(new TitleScreen());
            }
            if (phase == 0 && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                phase = 1;
                var rules = new GameRules();
                rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
                mc.createWorldOpenFlows().createFreshLevel("surface-qa-" + started,
                        new LevelSettings("Surface QA", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT),
                        new WorldOptions(20261105L, false, false),
                        registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                        mc.screen);
            } else if (phase == 1 && mc.player != null && mc.getSingleplayerServer() != null && mc.screen == null) {
                setupFace(mc);
            } else if (phase == 2 && serverReady && ticks > 30 && mc.player != null && mc.screen == null) {
                Direction normal = FACES[faceIndex];
                GlobalVariables.currentQuat.rotationTo(new Vector3f(0, 1, 0), vector(normal));
                GlobalVariables.prevQuat.set(GlobalVariables.currentQuat);
                GlobalVariables.B_Vx1 = GlobalVariables.B_Vy1 = GlobalVariables.B_Vz1 = 0;
                SurfaceAdhesion.reset();
                next(3);
            } else if (phase == 3 && ticks > 12) {
                require(SurfaceConfig.SPEC.isLoaded(), "SERVER settings available on actual client");
                require(activeNormal() == FACES[faceIndex], "actual client capture " + FACES[faceIndex]);
                // Give a real world-space tangent drift, then let normal client ticks stop it.
                Vector3f tangent = FACES[faceIndex].getAxis() == Direction.Axis.X ? new Vector3f(0, 0, .3f) : new Vector3f(.3f, 0, 0);
                GlobalVariables.B_Vx1 = tangent.x;
                GlobalVariables.B_Vy1 = tangent.y;
                GlobalVariables.B_Vz1 = tangent.z;
                next(4);
            } else if (phase == 4 && ticks > 12) {
                Direction normal = FACES[faceIndex];
                require(activeNormal() == normal, "stays attached while parking " + normal);
                Vector3f velocity = new Vector3f(GlobalVariables.B_Vx1, GlobalVariables.B_Vy1, GlobalVariables.B_Vz1);
                velocity.sub(new Vector3f(vector(normal)).mul(velocity.dot(vector(normal))));
                require(velocity.length() < .0051, "real client loop parks tangent drift " + normal);
                CHECKS.add("capture-and-parking-" + normal);
                LOG.info("SURFACE_QA_READY normal={} inputMode={}", normal, usesSimulatedInput() ? "simulated" : "physical");
                heldUp = usesSimulatedInput();
                next(5);
            } else if (phase == 5) {
                if (ticks % 20 == 0) LOG.info("SURFACE_QA_STATE normal={} active={} position={} velocity={},{},{} inputUp={}",
                        FACES[faceIndex], activeNormal(), mc.player.position(), GlobalVariables.B_Vx1, GlobalVariables.B_Vy1, GlobalVariables.B_Vz1, GlobalVariables.B_INy);
                if (activeNormal() == null && separation(mc) > .36) {
                    CHECKS.add((usesSimulatedInput() ? "simulated-input-escape-" : "real-space-key-escape-") + FACES[faceIndex]);
                    LOG.info("SURFACE_QA_ESCAPED normal={} inputMode={}", FACES[faceIndex], usesSimulatedInput() ? "simulated" : "physical");
                    heldUp = false;
                    next(6);
                }
            } else if (phase == 6 && ticks > 10 && !KeyPollingHandler.isPressed(Keybinds.B_UP)) {
                if (++faceIndex == FACES.length) {
                    faceIndex = 0;
                    if (usesSimulatedInput()) startLifecycle(mc);
                    else finish(mc, null);
                } else setupFace(mc);
            } else if (phase == 10 && serverReady && ticks > 20 && activeNormal() == Direction.UP) {
                teleport(mc, .5, 83, .5);
                next(11);
            } else if (phase == 11 && serverReady && Math.abs(mc.player.getY() - 83) < .2) {
                require(activeNormal() == null && rememberedNormal() == null && cooldown() == 0,
                        "teleport packet clears surface and cooldown before a new collision");
                CHECKS.add("teleport-packet-reset");
                teleport(mc, .5, 81, .5);
                next(12);
            } else if (phase == 12 && serverReady && ticks > 15 && activeNormal() == Direction.UP) {
                equipBackpack(mc, false);
                next(13);
            } else if (phase == 13 && serverReady && !GlobalVariables.B_LowGravity) {
                require(activeNormal() == null && rememberedNormal() == null, "exit from low gravity clears surface memory");
                CHECKS.add("exit-low-gravity-reset");
                equipBackpack(mc, true);
                next(14);
            } else if (phase == 14 && serverReady && ticks > 15 && GlobalVariables.B_LowGravity && activeNormal() == Direction.UP) {
                CHECKS.add("restore-low-gravity-capture");
                GlobalVariables.B_Vx5 = .12f;
                GlobalVariables.B_Vy5 = GlobalVariables.B_Vz5 = 0;
                next(15);
            } else if (phase == 15) {
                require(GlobalVariables.B_Vx5 == 0 && GlobalVariables.B_Vy5 == 0 && GlobalVariables.B_Vz5 == 0,
                        "recoil buffer consumed by the next production movement tick");
                require(Math.abs(GlobalVariables.B_Vx1 - .12f) < .0001f, "recoil applied exactly once after surface braking");
                next(16);
            } else if (phase == 16) {
                require(GlobalVariables.B_Vx5 == 0 && GlobalVariables.B_Vx1 > 0 && GlobalVariables.B_Vx1 < .1f,
                        "following tick brakes without replaying recoil");
                CHECKS.add("recoil-consumed-once");
                finish(mc, null);
            }
        } catch (Throwable error) {
            finish(mc, error);
        }
    }

    private static void setupFace(Minecraft mc) {
        heldUp = false;
        serverReady = false;
        GlobalVariables.B_Vx1 = GlobalVariables.B_Vy1 = GlobalVariables.B_Vz1 = 0;
        SurfaceAdhesion.reset();
        var playerId = mc.player.getUUID();
        Direction normal = FACES[faceIndex];
        mc.getSingleplayerServer().execute(() -> {
            try {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);
                var level = player.serverLevel();
                for (BlockPos pos : BlockPos.betweenClosed(-4, 80, -4, 4, 85, 4)) {
                    boolean boundary = pos.getX() == -4 || pos.getX() == 4 || pos.getZ() == -4 || pos.getZ() == 4 || pos.getY() == 80 || pos.getY() == 85;
                    level.setBlockAndUpdate(pos, boundary ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
                player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(main.BASIC_BACKPACK.get()));
                double x = normal == Direction.EAST ? -2.4 : normal == Direction.WEST ? 3.4 : .5;
                double y = normal == Direction.UP ? 81 : normal == Direction.DOWN ? 83.8 : 82;
                double z = normal == Direction.SOUTH ? -2.4 : normal == Direction.NORTH ? 3.4 : .5;
                player.connection.teleport(x, y, z, 0, 0);
                player.inventoryMenu.broadcastChanges();
                serverReady = true;
            } catch (Throwable error) {
                serverFailure = error;
            }
        });
        next(2);
    }

    private static void startLifecycle(Minecraft mc) {
        heldUp = false;
        GlobalVariables.currentQuat.identity();
        GlobalVariables.prevQuat.identity();
        teleport(mc, .5, 81, .5);
        next(10);
    }

    private static void teleport(Minecraft mc, double x, double y, double z) {
        // Deliberately do not call SurfaceAdhesion.reset here: the packet hook must clear it.
        GlobalVariables.B_Vx1 = GlobalVariables.B_Vy1 = GlobalVariables.B_Vz1 = 0;
        serverReady = false;
        var id = mc.player.getUUID();
        mc.getSingleplayerServer().execute(() -> {
            try {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                player.connection.teleport(x, y, z, 0, 0);
                serverReady = true;
            } catch (Throwable error) {
                serverFailure = error;
            }
        });
    }

    private static void equipBackpack(Minecraft mc, boolean equipped) {
        serverReady = false;
        var id = mc.player.getUUID();
        mc.getSingleplayerServer().execute(() -> {
            try {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                player.setItemSlot(EquipmentSlot.CHEST, equipped ? new ItemStack(main.BASIC_BACKPACK.get()) : ItemStack.EMPTY);
                player.inventoryMenu.broadcastChanges();
                serverReady = true;
            } catch (Throwable error) {
                serverFailure = error;
            }
        });
    }

    private static Object state() throws ReflectiveOperationException {
        var field = SurfaceAdhesion.class.getDeclaredField("STATE");
        field.setAccessible(true);
        return field.get(null);
    }

    private static Direction rememberedNormal() throws ReflectiveOperationException {
        Object state = state();
        var method = state.getClass().getDeclaredMethod("rememberedNormal");
        method.setAccessible(true);
        return (Direction) method.invoke(state);
    }

    private static int cooldown() throws ReflectiveOperationException {
        Object state = state();
        var field = state.getClass().getDeclaredField("cooldown");
        field.setAccessible(true);
        return field.getInt(state);
    }

    private static double separation(Minecraft mc) {
        var box = mc.player.getBoundingBox();
        return switch (FACES[faceIndex]) {
            case UP -> box.minY - 81;
            case DOWN -> 85 - box.maxY;
            case EAST -> box.minX + 3;
            case WEST -> 4 - box.maxX;
            case SOUTH -> box.minZ + 3;
            case NORTH -> 4 - box.maxZ;
        };
    }

    private static Direction activeNormal() throws ReflectiveOperationException {
        // Only the test fixture introspects internal state; no production debug API is exposed.
        var field = SurfaceAdhesion.class.getDeclaredField("activeNormal");
        field.setAccessible(true);
        return (Direction) field.get(null);
    }

    private static Vector3f vector(Direction normal) {
        return new Vector3f(normal.getStepX(), normal.getStepY(), normal.getStepZ());
    }

    private static void next(int value) {
        phase = value;
        ticks = 0;
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void finish(Minecraft mc, Throwable error) {
        try {
            Path result = mc.gameDirectory.toPath().resolve("surface-result.json");
            Files.writeString(result, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(
                    Map.of("passed", error == null, "inputMode", usesSimulatedInput() ? "simulated" : "physical",
                            "physicalInputVerified", !usesSimulatedInput() && error == null,
                            "checks", CHECKS, "failure", error == null ? "" : error.toString())));
        } catch (Exception writeFailure) {
            LOG.error("SURFACE_QA_RESULT_WRITE_FAILED", writeFailure);
        }
        heldUp = false;
        if (error == null) LOG.info("SURFACE_CLIENT_QA_PASS inputMode={} checks={}", usesSimulatedInput() ? "simulated" : "physical", CHECKS.size());
        else LOG.error("SURFACE_CLIENT_QA_FAIL phase={} face={}", phase, FACES[faceIndex], error);
        phase = 99;
        mc.stop();
    }
}
