package cn.rbq108.nextboundarycornerstone.camera;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import cn.rbq108.nextboundarycornerstone.body.FreeLookMath;
import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxPose;
import net.minecraft.client.Minecraft;
import org.joml.Quaternionf;

/** Collision-owned ordinary free look, independent of the user's C/Shift toggle. */
public final class CollisionFreeLook {
    private static final Quaternionf BODY = new Quaternionf();
    private static FreeLookMath.Angles angles = new FreeLookMath.Angles(0, 0, 0);
    private CollisionFreeLook() {}

    public static boolean isLimiting() {
        return GlobalVariables.B_CollisionFreeLookActive && !GlobalVariables.B_FreeCameraActive
                && !GlobalVariables.B_HeadRotationLocked;
    }

    public static boolean turn(float yaw, float pitch, float roll) {
        if (!isLimiting()) return false;
        angles = angles.turn(yaw, pitch, roll, firstPerson());
        applyView();
        return true;
    }

    public static void accept(SegmentedHitboxPose pose, boolean blocked) {
        if (!blocked || GlobalVariables.B_FreeCameraActive || GlobalVariables.B_HeadRotationLocked) {
            reset();
            return;
        }
        var expected = new Quaternionf(BODY).mul(angles.rotation());
        if (!GlobalVariables.B_CollisionFreeLookActive || Math.abs(BODY.dot(pose.body())) < .999999f
                || Math.abs(expected.dot(GlobalVariables.currentQuat)) < .999999f) {
            angles = FreeLookMath.relative(pose.body(), GlobalVariables.currentQuat);
        }
        BODY.set(pose.body());
        angles = angles.limited(firstPerson());
        GlobalVariables.B_CollisionFreeLookActive = true;
        Quaternionf before = new Quaternionf(GlobalVariables.currentQuat);
        applyView();
        GlobalVariables.headRelQuat.set(FreeLookMath.headRelative(pose));
        if (Math.abs(before.dot(GlobalVariables.currentQuat)) < .999999f) {
            // Do not interpolate through the rejected, out-of-range view this frame.
            GlobalVariables.prevQuat.set(GlobalVariables.currentQuat);
        }
    }

    private static boolean firstPerson() { return Minecraft.getInstance().options.getCameraType().isFirstPerson(); }
    private static void applyView() {
        GlobalVariables.currentQuat.set(BODY).mul(angles.rotation()).normalize();
        GlobalVariables.collisionLookYaw = angles.yaw();
        GlobalVariables.collisionLookPitch = angles.pitch();
        GlobalVariables.collisionLookRoll = angles.roll();
    }
    public static void reset() {
        GlobalVariables.B_CollisionFreeLookActive = false;
        GlobalVariables.collisionLookYaw = GlobalVariables.collisionLookPitch = GlobalVariables.collisionLookRoll = 0;
    }
}
