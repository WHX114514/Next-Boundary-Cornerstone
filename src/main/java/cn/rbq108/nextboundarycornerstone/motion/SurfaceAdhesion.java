package cn.rbq108.nextboundarycornerstone.motion;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.SurfaceConfig;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.EnumSet;

/** Internal client movement component. It never moves entities or writes global velocity. */
public final class SurfaceAdhesion {
    private static final SurfaceAdhesionState STATE = new SurfaceAdhesionState();
    private static LocalPlayer owner;
    private static Level level;
    private static Direction activeNormal;
    private static final EnumSet<Direction> BLOCKED_NORMALS = EnumSet.noneOf(Direction.class);

    private SurfaceAdhesion() {}

    public static void prepare(LocalPlayer player, Vector3f velocity, boolean grabbingHandle) {
        if (owner != player || level != player.level()) reset();
        owner = player;
        level = player.level();

        List<AABB> shapes = new ArrayList<>();
        // getBlockCollisions supplies world-space, context-sensitive VoxelShapes, not full cubes.
        for (var shape : level.getBlockCollisions(player,
                player.getBoundingBox().inflate(SurfaceGeometry.RELEASE_DISTANCE + SurfaceGeometry.CONTACT_EPSILON))) {
            shapes.addAll(shape.toAabbs());
        }
        Map<Direction, SurfaceGeometry.Contact> contacts = SurfaceGeometry.findContacts(player.getBoundingBox(), shapes);
        Vector3f incomingVelocity = new Vector3f(velocity);
        // Collision reconciliation remains active when surface assistance is disabled.
        SurfaceGeometry.removeInwardVelocity(velocity, BLOCKED_NORMALS);
        BLOCKED_NORMALS.clear();
        SurfaceGeometry.removeInwardVelocity(velocity, contacts);

        if (!GlobalVariables.B_LowGravity || !player.isAlive() || player.isPassenger() || player.noPhysics
                || grabbingHandle || !SurfaceConfig.SPEC.isLoaded() || !SurfaceConfig.SETTINGS.enabled.get()) {
            STATE.reset();
            activeNormal = null;
            return;
        }
        activeNormal = STATE.update(player.getBoundingBox(), contacts, incomingVelocity, GlobalVariables.B_rush);
    }

    public static void apply(Vector3f velocity, Vector3f worldIntent, Vector3f propulsionDelta, float maxAcceleration) {
        if (activeNormal == null) return;
        var config = SurfaceConfig.SETTINGS;
        var settings = new SurfaceAdhesionMath.Settings(config.strengthRatio.get().floatValue(),
                config.normalDamping.get().floatValue(), config.stopTicks.get());
        SurfaceAdhesionMath.apply(velocity, activeNormal, worldIntent, propulsionDelta, maxAcceleration, settings);
    }

    public static void reset() {
        STATE.reset();
        activeNormal = null;
        owner = null;
        level = null;
        BLOCKED_NORMALS.clear();
    }

    public static void recordCollision(LocalPlayer player, Vec3 requested, Vec3 resolved) {
        if (owner != player || level != player.level()) reset();
        owner = player;
        level = player.level();
        BLOCKED_NORMALS.addAll(SurfaceGeometry.blockedNormals(requested, resolved));
    }
}
