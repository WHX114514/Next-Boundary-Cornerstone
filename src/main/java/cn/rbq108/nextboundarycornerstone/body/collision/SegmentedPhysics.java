package cn.rbq108.nextboundarycornerstone.body.collision;

import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxPose;
import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxes;
import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public final class SegmentedPhysics {
    private SegmentedPhysics() {}

    public static boolean enabled(Player player) {
        return SegmentedHitboxes.enabled(player) && player.isAlive() && !player.noPhysics
                && !player.isSpectator() && !player.isPassenger();
    }

    public static PhysicsBox box(Player player) { return player.getData(main.PLAYER_ROTATION).collision(player); }

    public static List<VoxelShape> obstacles(Player player, AABB area) {
        var result = new ArrayList<VoxelShape>(player.level().getEntityCollisions(player, area));
        for (VoxelShape shape : player.level().getBlockCollisions(player, area)) result.add(shape);
        var border = player.level().getWorldBorder();
        if (border.isInsideCloseToBorder(player, area)) result.add(border.getCollisionShape());
        return result;
    }

    public static Vec3 clipMovement(Player player, Vec3 requested) {
        List<AABB> pieces = box(player).pieces();
        AABB area = CompoundCollisionMath.envelope(pieces).expandTowards(requested);
        return CompoundCollisionMath.clip(pieces, obstacles(player, area), requested);
    }

    public static RotationPlanner.Result acceptPose(Player player, SegmentedHitboxPose desired) {
        var attachment = player.getData(main.PLAYER_ROTATION);
        if (!enabled(player)) {
            attachment.setHitboxPose(desired);
            attachment.setBodyBlocked(false);
            return new RotationPlanner.Result(desired, Vec3.ZERO, false);
        }
        SegmentedHitboxPose previous = attachment.poseOrDefault();
        List<AABB> sweep = RotationPlanner.sweep(previous, desired, player.position(), player.getBbHeight(), player.getScale());
        AABB area = CompoundCollisionMath.envelope(sweep).inflate(.5001 * player.getScale());
        var scene = obstacles(player, area);
        var result = RotationPlanner.plan(previous, desired, player.position(), player.getBbHeight(), player.getScale(), scene);
        if (result.offset().lengthSqr() > 1.0e-16) {
            Vec3 origin = player.position();
            player.move(MoverType.SELF, result.offset());
            if (player.position().subtract(origin).distanceToSqr(result.offset()) > 1.0e-10) {
                // Another movement hook clipped the proposed correction; never install an intersecting body.
                result = new RotationPlanner.Result(previous, player.position().subtract(origin), true);
            }
        }
        attachment.setHitboxPose(result.pose());
        attachment.setBodyBlocked(result.bodyBlocked());
        return result;
    }
}
