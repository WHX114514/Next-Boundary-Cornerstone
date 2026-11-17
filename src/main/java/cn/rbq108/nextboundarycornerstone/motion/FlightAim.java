package cn.rbq108.nextboundarycornerstone.motion;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Shared world-space view and local launch offsets. Hand sway never owns logical angles. */
public final class FlightAim {
    private FlightAim() {}

    public static Quaternionf rotation(Player player) {
        return new Quaternionf(player.level().isClientSide() && player.isLocalPlayer()
                ? GlobalVariables.currentQuat : cn.rbq108.nextboundarycornerstone.attachment.PlayerRotationAttachment.get(player).getQuaternion()).normalize();
    }

    public static Vec3 rotate(Player player, double x, double y, double z) {
        return new Vec3(rotation(player).transform(new Vector3f((float) x, (float) y, (float) z)));
    }

    public static void publishAngles(Player player) {
        var forward = rotate(player, 0, 0, 1);
        double horizontal = forward.horizontalDistance();
        // Keep yaw at the exact pole: it is undefined there, while the quaternion remains authoritative.
        if (horizontal > 1e-7) player.setYRot((float) Math.toDegrees(Math.atan2(-forward.x, forward.z)));
        player.setXRot((float) Math.toDegrees(Math.atan2(-forward.y, horizontal)));
    }

    public static Vec3 launchDirection(Player player, float pitch, float yaw, float pitchOffset) {
        // Some items explicitly shoot from xRotO/yRotO. Those are already world angles,
        // not a local spread relative to the current frame (e.g. throwing weapons).
        if ((pitch != player.getXRot() || yaw != player.getYRot())
                && pitch == player.xRotO && yaw == player.yRotO && pitchOffset == 0) {
            return Vec3.directionFromRotation(pitch, yaw);
        }
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
