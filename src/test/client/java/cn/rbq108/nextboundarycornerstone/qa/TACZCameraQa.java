package cn.rbq108.nextboundarycornerstone.qa;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import cn.rbq108.nextboundarycornerstone.event.ClientEvents;
import com.tacz.guns.api.client.other.KeepingItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.IntSupplier;

/** Opt-in camera diagnostics. Never changes production code or installed gun resources. */
final class TACZCameraQa {
    private final IntSupplier phase, ticks, shot;
    private final java.io.PrintWriter frames;
    private boolean probing, matrixDone;
    private float inputYaw, inputPitch, inputRoll;

    static void start(IntSupplier phase, IntSupplier ticks, IntSupplier shot) {
        try { new TACZCameraQa(phase, ticks, shot); }
        catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
    }

    private TACZCameraQa(IntSupplier phase, IntSupplier ticks, IntSupplier shot) throws java.io.IOException {
        this.phase = phase; this.ticks = ticks; this.shot = shot;
        frames = new java.io.PrintWriter(Files.newBufferedWriter(Path.of("../camera-qa-frames.csv")), true);
        frames.println("kind,time,phase,ticks,shot,partial,yaw,pitch,roll,referenceYaw,referencePitch,referenceRoll,errorDegrees,fov,localFrameErrorDegrees");
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, ViewportEvent.ComputeCameraAngles.class, e -> {
            if (probing) return;
            inputYaw = e.getYaw(); inputPitch = e.getPitch(); inputRoll = e.getRoll();
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, ViewportEvent.ComputeCameraAngles.class, this::camera);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, ViewportEvent.ComputeFov.class, e -> {
            if (phase.getAsInt() < 2 || !e.usedConfiguredFov()) return;
            frames.printf(java.util.Locale.ROOT, "fov,%d,%d,%d,%d,%.6f,0,0,0,0,0,0,0,%.9f,0%n",
                    System.nanoTime(), phase.getAsInt(), ticks.getAsInt(), shot.getAsInt(), e.getPartialTick(), e.getFOV());
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END && phase.getAsInt() == 5 && ticks.getAsInt() >= 25 && !matrixDone) {
                matrixDone = true;
                try { matrix(); } catch (Exception ex) { throw new RuntimeException("camera QA matrix", ex); }
            }
        });
    }

    private static ViewportEvent.ComputeCameraAngles event(double partial, float yaw, float pitch, float roll) {
        var mc = Minecraft.getInstance();
        return new ViewportEvent.ComputeCameraAngles(mc.gameRenderer, mc.gameRenderer.getMainCamera(), partial, yaw, pitch, roll);
    }

    private static Quaternionf rotation(ViewportEvent.ComputeCameraAngles e) {
        return new Quaternionf().set(new org.joml.Quaterniond().rotationYXZ(Math.toRadians(-e.getYaw()),
                Math.toRadians(e.getPitch()), Math.toRadians(e.getRoll())));
    }

    private static double error(Quaternionf a, Quaternionf b) {
        double max = 0;
        for (var axis : new Vector3f[] {new Vector3f(1,0,0),new Vector3f(0,1,0),new Vector3f(0,0,1)}) {
            double distance = a.transform(new Vector3f(axis)).distance(b.transform(new Vector3f(axis)));
            max = Math.max(max, Math.toDegrees(2 * Math.asin(Math.min(1, distance / 2))));
        }
        return max;
    }

    private static void nativeAnimation(ViewportEvent.ComputeCameraAngles event) {
        if (!Minecraft.getInstance().options.bobView().get()) return;
        var stack = KeepingItemRenderer.getRenderer().getCurrentItem();
        var renderer = net.minecraftforge.client.extensions.common.IClientItemExtensions.of(stack.getItem()).getCustomRenderer();
        try {
            // Direct renderer call bypasses only our CameraSetupEvent call-site adapter.
            renderer.getClass().getMethod("applyLevelCameraAnimation",ViewportEvent.ComputeCameraAngles.class,
                    net.minecraft.world.item.ItemStack.class,net.minecraft.client.player.LocalPlayer.class)
                    .invoke(renderer,event,stack,Minecraft.getInstance().player);
        } catch (NoSuchMethodException ignored) {
            // Empty hand or a renderer without TaCZ camera animation.
        } catch (ReflectiveOperationException ex) { throw new RuntimeException(ex); }
    }

    private void camera(ViewportEvent.ComputeCameraAngles actual) {
        if (probing || phase.getAsInt() < 2 || phase.getAsInt() >= 99) return;
        var reference = event(actual.getPartialTick(), inputYaw, inputPitch, inputRoll);
        ClientEvents.onComputeCameraAngles(reference);
        var base = rotation(reference);
        var upright = event(actual.getPartialTick(),reference.getYaw(),reference.getPitch(),0);
        var uprightBase = rotation(upright);
        nativeAnimation(upright);
        var localReference = new Quaternionf(base).mul(uprightBase.invert().mul(rotation(upright)));
        // Apply the original TaCZ method once to the flight base, without changing its animation.
        nativeAnimation(reference);
        double localError = error(rotation(actual),localReference);
        if (GlobalVariables.B_LowGravity && localError > .001)
            TACZQaState.failure = "camera animation local-frame error=" + localError;
        frames.printf(java.util.Locale.ROOT, "camera,%d,%d,%d,%d,%.6f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,%.9f,0,%.9f%n",
                System.nanoTime(), phase.getAsInt(), ticks.getAsInt(), shot.getAsInt(), actual.getPartialTick(),
                actual.getYaw(),actual.getPitch(),actual.getRoll(),reference.getYaw(),reference.getPitch(),reference.getRoll(),
                error(rotation(actual),rotation(reference)),localError);
    }

    private void matrix() throws Exception {
        var stack = KeepingItemRenderer.getRenderer().getCurrentItem();
        var renderer = net.minecraftforge.client.extensions.common.IClientItemExtensions
                .of(stack.getItem()).getCustomRenderer();
        // TaCZ's renderer implements an interface from a nested runtime-only library.
        var model = (com.tacz.guns.client.model.BedrockAnimatedModel)renderer.getClass()
                .getMethod("getModel",net.minecraft.world.item.ItemStack.class).invoke(renderer,stack);
        var animation = model.getCameraAnimationObject();
        var savedAnimation = animation.rotationQuaternion;
        var prev = new Quaternionf(GlobalVariables.prevQuat);
        var current = new Quaternionf(GlobalVariables.currentQuat);
        boolean gravity = GlobalVariables.B_LowGravity;
        double dz = GlobalVariables.B_Dz, previousDz = GlobalVariables.prev_B_Dz;
        var results = new ArrayList<java.util.Map<String,Object>>();
        probing = true;
        try {
            float[][] views = {{0,0,0},{-40,23,0},{-40,23,90},{-40,23,180},{30,89.9f,40},{30,89.99f,40},
                    {30,90,40},{30,90.01f,40},{30,90.1f,40},{-120,-65,-90}};
            float[][] animations = {{0,0,0},{.5f,0,0},{0,.5f,0},{0,0,1},{.3f,-.4f,.6f}};
            for (boolean floating : new boolean[] {false,true}) for (var view : views) for (var sample : animations) {
                GlobalVariables.B_LowGravity = floating;
                GlobalVariables.B_Dz = GlobalVariables.prev_B_Dz = 0;
                var base = new Quaternionf().rotationYXZ((float)Math.toRadians(-view[0]),
                        (float)Math.toRadians(view[1]),(float)Math.toRadians(view[2]));
                GlobalVariables.currentQuat.set(base); GlobalVariables.prevQuat.set(base);
                animation.rotationQuaternion = new Quaternionf().rotationXYZ((float)Math.toRadians(sample[0]),
                        (float)Math.toRadians(sample[1]),(float)Math.toRadians(sample[2]));
                var actual = event(.5, floating ? -123 : view[0],floating ? 47 : view[1],floating ? 0 : view[2]);
                var expected = event(.5, actual.getYaw(),actual.getPitch(),actual.getRoll());
                ClientEvents.onComputeCameraAngles(expected);
                double baseError = error(base,rotation(expected));
                var upright = event(.5,expected.getYaw(),expected.getPitch(),0);
                var uprightBase = rotation(upright);
                nativeAnimation(upright);
                var localReference = new Quaternionf(base).mul(uprightBase.invert().mul(rotation(upright)));
                nativeAnimation(expected);
                MinecraftForge.EVENT_BUS.post(actual);
                double localError = error(rotation(actual),localReference);
                if (floating && (baseError > .001 || localError > .001))
                    throw new AssertionError("camera matrix base=" + baseError + " local=" + localError);
                if (!floating && error(rotation(actual),rotation(expected)) > .001)
                    throw new AssertionError("ordinary camera animation changed");
                results.add(java.util.Map.of("floating",floating,"view",view,"animation",sample,
                        "baseErrorDegrees",baseError,"compositionErrorDegrees",error(rotation(actual),rotation(expected)),
                        "localFrameErrorDegrees",localError,
                        "actual",new float[]{actual.getYaw(),actual.getPitch(),actual.getRoll()},
                        "reference",new float[]{expected.getYaw(),expected.getPitch(),expected.getRoll()}));
            }
            var sweep = new ArrayList<java.util.Map<String,Object>>();
            for (boolean animated : new boolean[] {false,true}) {
                Quaternionf previous = null, previousBase = null;
                for (int i = 0; i <= 1000; i++) {
                    float pitch = 89.8f + i * .0004f;
                    var base = new Quaternionf().rotationYXZ((float)Math.toRadians(-30),
                            (float)Math.toRadians(pitch),(float)Math.toRadians(40));
                    GlobalVariables.B_LowGravity = true;
                    GlobalVariables.currentQuat.set(base); GlobalVariables.prevQuat.set(base);
                    animation.rotationQuaternion = new Quaternionf().rotationX(animated ? (float)Math.toRadians(.5) : 0);
                    var actual = event(.5,-123,47,0);
                    MinecraftForge.EVENT_BUS.post(actual);
                    var rendered = rotation(actual);
                    if (previous != null) {
                        double step = error(previous,rendered);
                        if (step > .003) throw new AssertionError("camera pole discontinuity=" + step + " pitch=" + pitch);
                        sweep.add(java.util.Map.of("animated",animated,"pitch",pitch,
                                "requestedStepDegrees",error(previousBase,base),"renderedStepDegrees",step,
                                "baseErrorDegrees",error(base,rendered)));
                    }
                    previous = rendered; previousBase = base;
                }
            }
            Files.writeString(Path.of("../camera-qa-sweep.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(sweep));
        } finally {
            animation.rotationQuaternion = savedAnimation;
            GlobalVariables.prevQuat.set(prev); GlobalVariables.currentQuat.set(current);
            GlobalVariables.B_LowGravity = gravity;
            GlobalVariables.B_Dz = dz; GlobalVariables.prev_B_Dz = previousDz;
            probing = false;
        }
        Files.writeString(Path.of("../camera-qa-matrix.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(results));
        com.mojang.logging.LogUtils.getLogger().info("TACZ_CAMERA_QA_MATRIX_COMPLETE cases={}",results.size());
    }
}
