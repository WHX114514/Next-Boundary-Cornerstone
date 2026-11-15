package cn.rbq108.nextboundarycornerstone.body;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Eye-local occupancy checks; entity dimensions are only the old model pivot in zero gravity. */
public final class EyeBlockChecks {
    private static final double RADIUS = .005;
    public record BlockingBlock(BlockState state, BlockPos pos) {}
    private EyeBlockChecks() {}

    public static AABB probe(Vec3 eye, float scale) {
        double radius = RADIUS * scale;
        return new AABB(eye.x - radius, eye.y - radius, eye.z - radius,
                eye.x + radius, eye.y + radius, eye.z + radius);
    }

    @Nullable
    public static BlockingBlock viewBlocking(BlockGetter level, Vec3 eye, float scale) {
        var probe = probe(eye, scale);
        var pos = new BlockPos.MutableBlockPos();
        for (int x = net.minecraft.util.Mth.floor(probe.minX); x <= net.minecraft.util.Mth.floor(probe.maxX); x++)
            for (int y = net.minecraft.util.Mth.floor(probe.minY); y <= net.minecraft.util.Mth.floor(probe.maxY); y++)
                for (int z = net.minecraft.util.Mth.floor(probe.minZ); z <= net.minecraft.util.Mth.floor(probe.maxZ); z++) {
                    pos.set(x, y, z);
                    var state = level.getBlockState(pos);
                    if (state.getRenderShape() != RenderShape.INVISIBLE && state.isViewBlocking(level, pos))
                        return new BlockingBlock(state, pos.immutable());
                }
        return null;
    }

    public static boolean suffocating(Player player) {
        if (player.noPhysics) return false;
        var probe = probe(player.getEyePosition(), 1.0f);
        for (var pos : BlockPos.betweenClosed(BlockPos.containing(probe.minX, probe.minY, probe.minZ),
                BlockPos.containing(probe.maxX, probe.maxY, probe.maxZ))) {
            var state = player.level().getBlockState(pos);
            if (state.isAir() || !state.isSuffocating(player.level(), pos)) continue;
            for (var box : state.getCollisionShape(player.level(), pos).toAabbs())
                if (probe.intersects(box.move(pos))) return true;
        }
        return false;
    }
}
