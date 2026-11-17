package cn.rbq108.nextboundarycornerstone.motion;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Bridges public world velocity writes to the existing propulsion cache, exactly once. */
public final class FlightVelocity {
    private static int internalWrites;

    private FlightVelocity() {}

    public static boolean controls(Entity entity) {
        return entity instanceof LocalPlayer && cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxes.enabled(entity);
    }

    public static void capture(Entity entity, Vec3 velocity) {
        if (internalWrites != 0 || !controls(entity)) return;
        GlobalVariables.B_Vx1 = (float) velocity.x * .91f;
        GlobalVariables.B_Vy1 = (float) velocity.y * .80f;
        GlobalVariables.B_Vz1 = (float) velocity.z * .91f;
    }

    /** Only bracket the particular maintenance setter; callbacks during travel remain observable. */
    public static void internal(Runnable write) {
        internalWrites++;
        try { write.run(); } finally { internalWrites--; }
    }

    public static void apply(LocalPlayer player) {
        internal(() -> player.setDeltaMovement(GlobalVariables.B_Vx1 / .91f,
                GlobalVariables.B_Vy1 / .80f, GlobalVariables.B_Vz1 / .91f));
    }
}
