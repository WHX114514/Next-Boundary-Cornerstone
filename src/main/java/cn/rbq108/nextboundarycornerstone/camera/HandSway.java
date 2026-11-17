package cn.rbq108.nextboundarycornerstone.camera;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/** Screen-axis mouse history for first-person rendering, independent of world yaw/pitch. */
public final class HandSway {
    private static LocalPlayer owner;
    private static float pitch, yaw, previousPitch, previousYaw;

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
}
