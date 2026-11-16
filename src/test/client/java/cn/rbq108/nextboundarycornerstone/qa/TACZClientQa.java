package cn.rbq108.nextboundarycornerstone.qa;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import cn.rbq108.nextboundarycornerstone.attachment.PlayerRotationAttachment;
import cn.rbq108.nextboundarycornerstone.body.ClientSegmentedHitboxes;
import cn.rbq108.nextboundarycornerstone.body.HandRenderContext;
import cn.rbq108.nextboundarycornerstone.body.OOBB.HitboxConfig;
import cn.rbq108.nextboundarycornerstone.main;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.event.common.GunReloadEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.api.item.gun.FireMode;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/** Real input API, real reload events, actual player animations and network sync; no desktop input. */
public final class TACZClientQa {
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();
    private final long started = System.currentTimeMillis();
    private final int scenarioCount = Integer.getInteger("cornerstone.qa.taczCases", 32);
    private final ArrayList<String> observations = new ArrayList<>();
    private int phase, ticks, scenario, reloadEvents, firstHands, thirdChecks;
    private boolean seenReload, seenAmmo, pastOldLimit;
    private CompletableFuture<?> serverSetup;
    private float minArmZ;
    private int shotScenario;
    private volatile java.util.UUID shooterId;
    private volatile CompletableFuture<String> shotCheck;
    private volatile boolean shotAiming;
    private net.minecraft.world.phys.Vec3 shotLaunchVelocity;

    public TACZClientQa() {
        require(ModList.get().isLoaded("tacz"), "taczQa requires -PtaczRuntime=true");
        require(scenarioCount >= 2 && scenarioCount <= 32, "taczQaCases must be between 2 and 32");
        TACZQaState.active = true;
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        MinecraftForge.EVENT_BUS.addListener(this::reload);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, EntityJoinLevelEvent.class, event -> bullet(event, true));
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, EntityJoinLevelEvent.class, event -> bullet(event, false));
    }

    private void bullet(EntityJoinLevelEvent event, boolean beforeHandlers) {
        var check = shotCheck;
        if (check == null || check.isDone() || event.getLevel().isClientSide()
                || !(event.getEntity() instanceof com.tacz.guns.entity.EntityKineticBullet bullet)
                || !(bullet.getOwner() instanceof net.minecraft.world.entity.player.Player player)
                || !player.getUUID().equals(shooterId)) return;
        try {
            if (beforeHandlers) {
                var motion = player.getDeltaMovement();
                shotLaunchVelocity = bullet.getDeltaMovement().subtract(motion.x, player.onGround() ? 0 : motion.y, motion.z);
                return;
            }
            require(player.isNoGravity(), "trajectory shot remains in flight");
            var operator = IGunOperator.fromLivingEntity(player);
            require(operator.getSynIsAiming() == shotAiming, "actual server ADS/hip state synchronized");
            var view = PlayerRotationAttachment.get(player).getQuaternion();
            var expected = new net.minecraft.world.phys.Vec3(view.transform(new org.joml.Vector3f(0, 0, 1)));
            var actual = bullet.getDeltaMovement();
            double alignment = actual.normalize().dot(expected.normalize());
            require(alignment > .98, "actual server bullet follows view: dot=" + alignment + " velocity=" + actual);
            require(actual.distanceTo(shotLaunchVelocity) < 1e-6, "actual server bullet retains spread velocity without inherited motion");
            check.complete("dot=" + alignment + " speed=" + actual.length() + " aimingProgress=" + operator.getSynAimingProgress()
                    + " shooterMotion=" + player.getDeltaMovement());
        } catch (Throwable failure) {
            check.completeExceptionally(failure);
        }
    }

    private void reload(GunReloadEvent event) {
        if (event.getLogicalSide().isClient() && event.getEntity() == Minecraft.getInstance().player) reloadEvents++;
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    private boolean firstPerson() { return (scenario & 1) == 0; }
    private boolean emptyReload() { return (scenario & 2) != 0; }
    private boolean slim() { return (scenario & 4) != 0; }
    private String gun() { return (scenario & 8) == 0 ? "ak47" : "glock_17"; }
    private boolean gravity() { return (scenario & 16) != 0; }

    private String label() {
        return gun() + "-" + (emptyReload() ? "empty" : "tactical") + "-" + (slim() ? "slim" : "normal")
                + "-" + (firstPerson() ? "first" : "third") + "-" + (gravity() ? "gravity" : "zero");
    }

    private void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || phase == 99) return;
        var mc = Minecraft.getInstance();
        try {
            require(System.currentTimeMillis() - started < 480_000, "TaCZ QA timeout phase=" + phase + " scenario=" + scenario);
            long window = mc.getWindow().getWindow();
            require(GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_VISIBLE) == GLFW.GLFW_FALSE, "QA window became visible");
            require(GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED) == GLFW.GLFW_FALSE, "QA window acquired focus");
            require(TACZQaState.failure == null, String.valueOf(TACZQaState.failure));
            if ((phase == 5 || phase == 6) && mc.player != null) {
                // Hold a deliberately different vanilla rotation while each real shot settles.
                GlobalVariables.currentQuat.rotationYXZ(.7f, .4f, shotScenario < 2 ? 1.5707964f : 3.1415927f);
                GlobalVariables.prevQuat.set(GlobalVariables.currentQuat);
                mc.player.setXRot(47);
                mc.player.setYRot(-123);
            }
            if (mc.screen != null && mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen"))
                mc.setScreen(new TitleScreen());
            if (phase == 0 && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
                mc.options.pauseOnLostFocus = false;
                mc.options.framerateLimit().set(60);
                mc.options.enableVsync().set(false);
                var rules = new net.minecraft.world.level.GameRules();
                rules.getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false, null);
                mc.createWorldOpenFlows().createFreshLevel("tacz-qa-" + started,
                        new net.minecraft.world.level.LevelSettings("TaCZ QA", net.minecraft.world.level.GameType.CREATIVE, false,
                                net.minecraft.world.Difficulty.PEACEFUL, true, rules, net.minecraft.world.level.WorldDataConfiguration.DEFAULT),
                        new net.minecraft.world.level.levelgen.WorldOptions(20261116L, false, false),
                        registries -> registries.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
                                .getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions());
                phase = 1;
            } else if (phase == 1 && mc.player != null && mc.getSingleplayerServer() != null && mc.screen == null) {
                require(HitboxConfig.mode().name().equals(System.getProperty("cornerstone.qa.hitboxMode")), "actual SERVER hitbox config");
                setupScenario(); phase = 2; ticks = 0;
            } else if (phase == 2 && ++ticks > 40 && serverSetup.isDone()) {
                require(mc.player.isNoGravity() != gravity(), "backpack/gravity synchronized");
                require(IGun.getIGunOrNull(mc.player.getMainHandItem()) != null, "real gun synchronized");
                // Set orientation after the gravity transition has initialized
                // its camera state; otherwise entry into flight can reset it.
                GlobalVariables.currentQuat.rotationXYZ(.15f, .25f, .6f);
                GlobalVariables.prevQuat.set(GlobalVariables.currentQuat);
                GlobalVariables.B_FreeCameraActive = slim() && !gravity();
                TACZQaState.checkingHands = true;
                IClientPlayerGunOperator.fromLocalPlayer(mc.player).reload();
                seenReload = seenAmmo = false;
                firstHands = TACZQaState.leftHands;
                phase = 3; ticks = 0;
            } else if (phase == 3) {
                ticks++;
                var operator = IGunOperator.fromLivingEntity(mc.player);
                seenReload |= operator.getSynReloadState().getStateType().isReloading();
                var item = mc.player.getMainHandItem();
                seenAmmo |= IGun.getIGunOrNull(item).getCurrentAmmoCount(item) > (emptyReload() ? 0 : 5);
                if (!gravity()) {
                    var pose = PlayerRotationAttachment.get(mc.player).getHitboxPose();
                    require(pose != null && pose.slim() == slim(), "live skeleton samples current skin");
                    minArmZ = Math.min(minArmZ, pose.joints().get(2).z());
                    pastOldLimit |= Math.abs(pose.joints().get(2).z()) > 8;
                    require(!ClientSegmentedHitboxes.isSampling() && !HandRenderContext.active(), "animation scopes clear each tick");
                    if (!firstPerson()) checkBodyModel();
                }
                if (ticks == 20 || ticks == 40) screenshot(label() + "-" + ticks);
                if (ticks > 90 && seenReload && seenAmmo && !operator.getSynReloadState().getStateType().isReloading()) {
                    if (firstPerson()) require(TACZQaState.leftHands > firstHands, "real first-person left hand rendered");
                    require(reloadEvents > scenario, "real client reload event fired");
                    observations.add(label() + " PASS frames=" + ticks);
                    LOG.info("TACZ_QA_SCENARIO_PASS {}", label());
                    if (++scenario < scenarioCount) { setupScenario(); phase = 2; ticks = 0; }
                    else { phase = 4; ticks = 0; }
                }
                require(ticks < 180, "reload failed to finish: " + label() + " seenReload=" + seenReload + " seenAmmo=" + seenAmmo);
            } else if (phase == 4) {
                // Real ADS/hip shots at two rolls; observe server bullet velocity after production handlers.
                scenario = 0; setupScenario(); phase = 5; ticks = 0;
                shooterId = mc.player.getUUID();
            } else if (phase == 5 && ++ticks > 40 && serverSetup.isDone()) {
                serverSetup.join();
                shotAiming = (shotScenario & 1) == 0;
                com.tacz.guns.client.input.AimKey.AIM_KEY.setDown(shotAiming);
                IClientPlayerGunOperator.fromLocalPlayer(mc.player).aim(shotAiming);
                phase = 6; ticks = 0;
            } else if (phase == 6 && ++ticks > 20) {
                shotCheck = new CompletableFuture<>();
                var result = IClientPlayerGunOperator.fromLocalPlayer(mc.player).shoot();
                require(result == ShootResult.SUCCESS, "real trajectory shot: " + result);
                var id = mc.player.getUUID();
                serverSetup = mc.getSingleplayerServer().submit(() -> {
                    var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                    var pose = PlayerRotationAttachment.get(player).getHitboxPose();
                    require(pose != null, "server received skeleton");
                    require(PlayerRotationAttachment.get(player).hitbox(player).pieces().size() == 16, "server retains all hitbox segments");
                    require(PlayerRotationAttachment.get(player).collision(player).pieces().size() == 10, "arms remain excluded from movement");
                });
                phase = 7; ticks = 0;
            } else if (phase == 7) {
                require(++ticks < 160, "actual server bullet was not observed");
                if (!serverSetup.isDone() || !shotCheck.isDone() || ticks <= 20) return;
                serverSetup.join();
                String shot = "trajectory-" + shotScenario + "-" + ((shotScenario & 1) == 0 ? "ads" : "hip") + " PASS " + shotCheck.join();
                observations.add(shot);
                LOG.info("TACZ_TRAJECTORY_QA_PASS {}", shot);
                com.tacz.guns.client.input.AimKey.AIM_KEY.setDown(false);
                IClientPlayerGunOperator.fromLocalPlayer(mc.player).aim(false);
                if (++shotScenario < 4) { phase = 5; ticks = 0; return; }
                boolean animator = ModList.get().isLoaded("playeranimator");
                if (animator) require(pastOldLimit, "real reload exercised old |z| > 8 crash boundary, minZ=" + minArmZ);
                require(thirdChecks > 0, "third-person synchronized skeleton checked");
                var report = "PASS TaCZ " + ModList.get().getModContainerById("tacz").orElseThrow().getModInfo().getVersion()
                        + ", PlayerAnimator=" + animator + ", mode=" + HitboxConfig.mode() + ", reloadCases=" + scenarioCount + ", reloadEvents=" + reloadEvents
                        + ", leftHands=" + TACZQaState.leftHands + ", rightHands=" + TACZQaState.rightHands
                        + ", bodyChecks=" + thirdChecks + ", oldBoundaryExercised=" + pastOldLimit + ", minArmZ=" + minArmZ
                        + ", hidden=true, focused=false, trajectoryShots=" + shotScenario + ", serverSkeleton=16/10\n";
                Files.writeString(Path.of("../client-report.txt"), report);
                Files.writeString(Path.of("../tacz-scenarios.txt"), String.join("\n", observations));
                LOG.info("TACZ_CLIENT_QA_PASS {}", report); phase = 99; mc.stop();
            }
        } catch (Throwable failure) {
            com.tacz.guns.client.input.AimKey.AIM_KEY.setDown(false);
            LOG.error("TACZ_CLIENT_QA_FAILED phase={} scenario={}", phase, scenario, failure);
            try { Files.writeString(Path.of("../client-report.txt"), "FAIL " + failure + "\n"); } catch (Exception ignored) {}
            phase = 99; mc.stop();
        }
    }

    private void setupScenario() {
        var mc = Minecraft.getInstance();
        TACZQaState.checkingHands = false;
        GlobalVariables.B_FreeCameraActive = false;
        TACZQaState.slim = slim();
        mc.options.setCameraType(firstPerson() ? CameraType.FIRST_PERSON : CameraType.THIRD_PERSON_BACK);
        // Roll exercises the same hand transform while the gun animation changes.
        GlobalVariables.currentQuat.rotationXYZ(.15f, .25f, .6f);
        GlobalVariables.prevQuat.set(GlobalVariables.currentQuat);
        var id = mc.player.getUUID();
        boolean normalGravity = gravity(), empty = emptyReload();
        String gun = gun();
        serverSetup = mc.getSingleplayerServer().submit(() -> {
            var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
            player.teleportTo(0, 100, 0);
            player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            player.setItemSlot(EquipmentSlot.CHEST, normalGravity ? ItemStack.EMPTY : new ItemStack(main.BASIC_BACKPACK.get()));
            var stack = GunItemBuilder.create().setId(new ResourceLocation("tacz", gun)).setAmmoCount(empty ? 0 : 5)
                    .setAmmoInBarrel(!empty).setFireMode(FireMode.SEMI).build();
            require(!stack.isEmpty(), "default gun resource exists: " + gun);
            player.setItemSlot(EquipmentSlot.MAINHAND, stack);
            IGunOperator.fromLivingEntity(player).draw(player::getMainHandItem);
        });
    }

    private void checkBodyModel() {
        var mc = Minecraft.getInstance();
        var renderer = (net.minecraft.client.renderer.entity.player.PlayerRenderer)mc.getEntityRenderDispatcher().getRenderer(mc.player);
        var model = renderer.getModel();
        float frame = mc.getFrameTime();
        // Invoke the actual transformed model, including PlayerAnimator and the production pose mixin.
        model.setupAnim(mc.player, 0, 0, mc.player.tickCount + frame, 0, 0);
        var expected = PlayerRotationAttachment.get(mc.player).renderHitboxPose(frame);
        var left = expected.joints().get(2);
        require(Math.abs(model.leftArm.z - left.z()) < .0001, "full-body arm pivot follows accepted skeleton");
        var actualRotation = new org.joml.Quaternionf().rotationZYX(model.leftArm.zRot, model.leftArm.yRot, model.leftArm.xRot);
        require(Math.abs(actualRotation.dot(left.rotation())) > .9999f,
                "full-body arm rotation follows accepted skeleton: actual=" + actualRotation + ", expected=" + left.rotation()
                        + ", euler=" + model.leftArm.xRot + "," + model.leftArm.yRot + "," + model.leftArm.zRot);
        thirdChecks++;
    }

    private static void screenshot(String name) throws java.io.IOException {
        var mc = Minecraft.getInstance();
        var dir = Path.of("../tacz-frames"); Files.createDirectories(dir);
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(dir.resolve(name + ".png")); }
    }
}
