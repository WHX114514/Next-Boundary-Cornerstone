package cn.rbq108.nextboundarycornerstone.body;

import cn.rbq108.nextboundarycornerstone.ServeMiao.communication.NetworkHandler;
import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class SegmentedHitboxes {
    private SegmentedHitboxes() {}

    public static boolean enabled(Entity entity) {
        return entity instanceof Player player && player.isNoGravity()
                && !NetworkHandler.isSpectatorAndRestored(player);
    }

    public static AABB forHitQuery(Entity entity) {
        return enabled(entity) ? entity.getData(main.PLAYER_ROTATION).hitbox((Player) entity) : entity.getBoundingBox();
    }

    /** Limbs can extend outside the movement box; add such players once, retaining vanilla filters. */
    public static List<Entity> candidates(Level level, Entity source, AABB area,
                                           Predicate<? super Entity> predicate, List<Entity> vanilla) {
        List<Entity> result = vanilla;
        for (Player player : level.players()) {
            if (player == source || player.isRemoved() || !enabled(player) || vanilla.contains(player)) continue;
            double radius = 3 * player.getScale();
            if (!area.intersects(player.getX() - radius, player.getY() - radius, player.getZ() - radius,
                    player.getX() + radius, player.getY() + radius, player.getZ() + radius)) continue;
            if (forHitQuery(player).intersects(area) && predicate.test(player)) {
                if (result == vanilla) result = new ArrayList<>(vanilla);
                result.add(player);
            }
        }
        return result;
    }
}
