package cn.rbq108.nextboundarycornerstone.qa;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import cn.rbq108.nextboundarycornerstone.attachment.PlayerRotationAttachment;
import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Hidden client/server tests of public direction and velocity entry points, without optional mods. */
public final class FlightCompatibilityClientQa {
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();
    private static final boolean BASELINE = Boolean.getBoolean("cornerstone.qa.baseline");
    private final long started = System.currentTimeMillis();
    private final List<Map<String, Object>> results = new ArrayList<>();
    private int phase, ticks, scenario, failures;
    private Vec3 expected, startPosition;
    private OptionalLaunchQa optional;
    private RehookedMovementQa rehooked;
    private final FlightControlQa control = new FlightControlQa();

    public FlightCompatibilityClientQa() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::tick);
    }

    private void check(boolean ok, String label, Object actual) {
        var row = new LinkedHashMap<String, Object>();
        row.put("case", scenario); row.put("check", label); row.put("pass", ok); row.put("actual", actual.toString());
        results.add(row);
        if (!ok) { failures++; LOG.error("FLIGHT_COMPAT_CHECK_FAIL {} actual={}", label, actual); }
    }

    private void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || phase == 99) return;
        var mc = Minecraft.getInstance();
        try {
            if (System.currentTimeMillis() - started > 540_000) throw new AssertionError("compat QA timeout phase=" + phase);
            long window = mc.getWindow().getWindow();
            if (GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_VISIBLE) != 0 || GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED) != 0)
                throw new AssertionError("window visible or focused");
            if (mc.screen != null && mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")) mc.setScreen(new TitleScreen());
            if (phase == 0 && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                mc.options.pauseOnLostFocus = false;
                mc.options.framerateLimit().set(60);
                var rules = new GameRules();
                rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
                mc.createWorldOpenFlows().createFreshLevel("compat-qa-" + started,
                        new net.minecraft.world.level.LevelSettings("Compatibility QA", GameType.SURVIVAL, false,
                                net.minecraft.world.Difficulty.PEACEFUL, true, rules, net.minecraft.world.level.WorldDataConfiguration.DEFAULT),
                        new net.minecraft.world.level.levelgen.WorldOptions(20261117L, false, false),
                        registries -> registries.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
                                .getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions());
                phase = 1;
            } else if (phase == 1 && mc.player != null && mc.screen == null && mc.getSingleplayerServer() != null) {
                server(player -> {
                    player.teleportTo(0, 120, 0);
                    player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(main.BASIC_BACKPACK.get()));
                });
                phase = 2; ticks = 0;
            } else if (phase == 2 && ++ticks > 60) {
                if (!mc.player.isNoGravity()) throw new AssertionError("backpack did not activate zero gravity");
                phase = 3; ticks = 0;
            } else if (phase == 3) {
                if (ticks++ == 0) {
                    GlobalVariables.B_Vx1 = GlobalVariables.B_Vy1 = GlobalVariables.B_Vz1 = 0;
                    mc.player.setDeltaMovement(Vec3.ZERO);
                    if (scenario < 6) {
                        var views = new Quaternionf[] {new Quaternionf(), new Quaternionf().rotateZ((float)Math.PI/2),
                                new Quaternionf().rotateZ((float)Math.PI), new Quaternionf().rotationYXZ(.7f,.4f,1.1f),
                                new Quaternionf().rotationYXZ(.3f,1.5707f,.8f), new Quaternionf().rotationYXZ(-.8f,-1.5707f,-.5f)};
                        GlobalVariables.currentQuat.set(views[scenario]);
                        GlobalVariables.prevQuat.set(views[scenario]);
                        mc.player.turn(13, -7);
                        expected = new Vec3(GlobalVariables.currentQuat.transform(new Vector3f(0,0,1))).normalize();
                        check(mc.player.getLookAngle().distanceTo(expected) < 2e-6, "current look agrees with flight", mc.player.getLookAngle());
                        check(Vec3.directionFromRotation(mc.player.getXRot(),mc.player.getYRot()).distanceTo(expected) < 1.75e-4,
                                "public angles agree with flight", List.of(mc.player.getXRot(),mc.player.getYRot()));
                        // This packet path must also precede arbitrary custom mod action packets.
                        cn.rbq108.nextboundarycornerstone.ServeMiao.communication.PlayerSent.sendRotation();
                    } else {
                        expected = new Vec3(.4,.3,.2);
                        switch (scenario - 6) {
                            case 0 -> mc.player.setDeltaMovement(expected);
                            case 1 -> mc.player.setDeltaMovement(.4,.3,.2);
                            case 2 -> mc.player.push(.4,.3,.2);
                            case 3 -> mc.player.lerpMotion(.4,.3,.2);
                            case 4 -> { mc.player.push(.1,.1,.1); mc.player.push(.3,.2,.1); }
                            case 5 -> { mc.player.setDeltaMovement(expected); mc.player.setDeltaMovement(Vec3.ZERO); expected = Vec3.ZERO; }
                        }
                        startPosition = mc.player.position();
                    }
                } else if (ticks == 3) {
                    if (scenario < 6) {
                        server(player -> check(player.getLookAngle().distanceTo(expected) < 2e-6,
                                "server look agrees with flight", player.getLookAngle()));
                    } else {
                        var delta = mc.player.position().subtract(startPosition).scale(.5);
                        check(delta.distanceTo(expected) < 2e-5, "external velocity survives two movement ticks", delta);
                    }
                    if (++scenario == 12) { phase = 4; ticks = 0; }
                    else ticks = 0;
                }
            } else if (phase == 4) {
                // Remain airborne beyond vanilla's 80-tick floating timeout.
                ++ticks;
                if(ticks==5) {
                    mc.options.keyJump.setDown(true);mc.options.keyShift.setDown(true);mc.options.keySprint.setDown(true);
                    startPosition=mc.player.position();
                }
                if(ticks==8) {
                    check(mc.options.keyJump.isDown() && mc.player.input.jumping,"jump remains observable",mc.player.input.jumping);
                    check(mc.options.keyShift.isDown() && mc.player.input.shiftKeyDown,"sneak remains observable",mc.player.input.shiftKeyDown);
                    check(mc.options.keySprint.isDown(),"sprint remains observable",mc.options.keySprint.isDown());
                    check(!mc.player.isCrouching() && !mc.player.isSprinting(),"vanilla crouch and sprint suppressed",List.of(mc.player.isCrouching(),mc.player.isSprinting()));
                    check(mc.player.position().distanceTo(startPosition)<1e-6,"no duplicate vanilla jump acceleration",mc.player.position().subtract(startPosition));
                    mc.options.keyJump.setDown(false);mc.options.keyShift.setDown(false);mc.options.keySprint.setDown(false);
                }
                if (ticks < 210) return;
                server(player -> {
                    check(!player.hasDisconnected(), "zero gravity remains connected", ticks);
                    check(!player.getAbilities().mayfly && !player.getAbilities().flying,
                            "survival zero gravity needs no flight abilities", List.of(player.getAbilities().mayfly,player.getAbilities().flying));
                });
                optional = new OptionalLaunchQa((ok,label) -> check(ok,"optional launch",label));
                phase=5;
            } else if (phase == 5 && optional.tick()) {
                rehooked=new RehookedMovementQa((ok,label) -> check(ok,"Rehooked movement",label));
                phase=6;
            } else if (phase == 6 && rehooked.tick()) {
                phase=7;
            } else if (phase == 7 && control.tick()) {
                var json = new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(results);
                Files.writeString(Path.of("../compat-results.json"), json);
                if (!BASELINE && failures > 0) throw new AssertionError(failures + " compatibility assertions failed");
                Files.writeString(Path.of("../client-report.txt"), "PASS " + (BASELINE ? "baseline recorded" : "flight compatibility")
                        + " checks=" + results.size() + " failures=" + failures + " hidden=true focused=false\n");
                phase = 99; mc.stop();
            }
        } catch (Throwable failure) {
            LOG.error("FLIGHT_COMPAT_CLIENT_QA_FAILED", failure);
            try {
                Files.writeString(Path.of("../compat-results.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(results));
                Files.writeString(Path.of("../client-report.txt"), "FAIL " + failure);
            } catch (Exception ignored) {}
            phase = 99; mc.stop();
        }
    }

    private static void server(java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> action) {
        var mc = Minecraft.getInstance(); var id = mc.player.getUUID();
        mc.getSingleplayerServer().submit(() -> action.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(id))).join();
    }
}
