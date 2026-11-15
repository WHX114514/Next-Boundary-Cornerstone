package cn.rbq108.nextboundarycornerstone.motion;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Launches use the flight view, never the mouse increments retained for vanilla hand sway. */
public final class FlightAim {
    private FlightAim() {}

    public static Quaternionf rotation(Player player) {
        return new Quaternionf(player.level().isClientSide() && player.isLocalPlayer()
                ? GlobalVariables.currentQuat : player.getData(main.PLAYER_ROTATION).getQuaternion()).normalize();
    }

    public static Vec3 rotate(Player player, double x, double y, double z) {
        return new Vec3(rotation(player).transform(new Vector3f((float) x, (float) y, (float) z)));
    }

    public static Vec3 launchDirection(Player player, float pitch, float yaw, float pitchOffset) {
        // Preserve bow spread and the upward potion/XP-bottle lob in the shooter's local frame.
        double localPitch = Math.toRadians(pitch - player.getXRot());
        double localYaw = Math.toRadians(yaw - player.getYRot());
        return rotate(player, -Math.sin(localYaw) * Math.cos(localPitch),
                -Math.sin(localPitch + Math.toRadians(pitchOffset)), Math.cos(localYaw) * Math.cos(localPitch));
    }

    public static Vec3 launchPosition(Player player) {
        return player.getEyePosition().subtract(rotate(player, 0, 0.1, 0));
    }
}
