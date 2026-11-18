package cn.rbq108.nextboundarycornerstone.camera;

import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxes;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/** Screen-axis mouse history for first-person rendering, independent of world yaw/pitch. */
public final class HandSway {
    private static LocalPlayer owner;
    private static float pitch, yaw, previousPitch, previousYaw;
    private static boolean renderingBob;

    private HandSway() {}

    public static void reset(LocalPlayer player) {
        owner = player;
        if (player != null) {
            previousPitch = pitch = player.getXRot();
            previousYaw = yaw = player.getYRot();
            player.xBobO = player.xBob = pitch;
            player.yBobO = player.yBob = yaw;
        }
    }

    public static void tick(LocalPlayer player) {
        if (owner != player) reset(player);
        previousPitch = pitch;
        previousYaw = yaw;
    }

    public static void turn(LocalPlayer player, float dx, float dy) {
        if (owner != player) reset(player);
        pitch += dx;
        yaw += dy;
        previousPitch = Mth.clamp(previousPitch + dx, -90, 90);
        previousYaw += dy;
    }

    public static float pitch(float partialTick) { return Mth.lerp(partialTick, previousPitch, pitch); }
    public static float yaw(float partialTick) { return Mth.lerp(partialTick, previousYaw, yaw); }

    /** First-person renderers subtract bob from world angles. Shift both samples into that frame. */
    public static void withRenderBob(LocalPlayer player, float partialTick, Runnable render) {
        if (renderingBob || owner != player || !SegmentedHitboxes.enabled(player)) {
            render.run();
            return;
        }

        float x = player.xBob, oldX = player.xBobO;
        float y = player.yBob, oldY = player.yBobO;
        double pitchOffset = (double) player.getViewXRot(partialTick) - pitch(1);
        double yawOffset = (double) player.getViewYRot(partialTick) - yaw(player.isPassenger() ? partialTick : 1);
        renderingBob = true;
        try {
            player.xBob = (float) (x + pitchOffset);
            player.xBobO = (float) (oldX + pitchOffset);
            player.yBob = (float) (y + yawOffset);
            player.yBobO = (float) (oldY + yawOffset);
            render.run();
        } finally {
            player.xBob = x;
            player.xBobO = oldX;
            player.yBob = y;
            player.yBobO = oldY;
            renderingBob = false;
        }
    }
}
