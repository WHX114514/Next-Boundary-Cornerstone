package cn.rbq108.nextboundarycornerstone.motion;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.SurfaceConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Map;

@GameTestHolder("next_boundary_cornerstone")
@PrefixGameTestTemplate(false)
public final class SurfaceRuntimeGameTests {
    @GameTest(template = "surface_empty")
    public static void serverConfigLoadsWithoutClientClasses(GameTestHelper helper) {
        helper.assertTrue(SurfaceConfig.SPEC.isLoaded(), "SERVER config loaded on dedicated GameTest server");
        helper.assertTrue(SurfaceConfig.SETTINGS.enabled.get(), "surface assistance defaults enabled");
        helper.assertTrue(Math.abs(SurfaceConfig.SETTINGS.strengthRatio.get() - .4) < .00001, "default strength");
        helper.assertTrue(SurfaceConfig.SETTINGS.stopTicks.get() == 6, "default parking time");
        helper.succeed();
    }

    @GameTest(template = "surface_empty")
    public static void realSlabStairAndFluidShapes(GameTestHelper helper) {
        BlockPos base = helper.absolutePos(new BlockPos(2, 2, 2));
        var level = helper.getLevel();
        level.setBlockAndUpdate(base, Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        AABB feetOnSlab = new AABB(base.getX() + .1, base.getY() + .5, base.getZ() + .1,
                base.getX() + .9, base.getY() + 1.7, base.getZ() + .9);
        var contacts = probe(helper, feetOnSlab);
        helper.assertTrue(contacts.containsKey(Direction.UP) && contacts.get(Direction.UP).gap() == 0,
                "actual slab top at half block height supports body");
        SurfaceAdhesionState state = new SurfaceAdhesionState();
        helper.assertTrue(state.update(feetOnSlab, contacts, new Vector3f(), false) == Direction.UP, "slab captures after touch");
        level.setBlockAndUpdate(base, Blocks.AIR.defaultBlockState());
        helper.assertTrue(state.update(feetOnSlab, probe(helper, feetOnSlab), new Vector3f(), false) == null,
                "removed support applies no force immediately");

        level.setBlockAndUpdate(base, Blocks.STONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST)
                .setValue(StairBlock.HALF, Half.BOTTOM));
        AABB feetOnStair = new AABB(base.getX() + .6, base.getY() + 1, base.getZ() + .1,
                base.getX() + .9, base.getY() + 2.2, base.getZ() + .9);
        contacts = probe(helper, feetOnStair);
        helper.assertTrue(contacts.containsKey(Direction.UP) && contacts.get(Direction.UP).gap() == 0,
                "real stair upper tread supports body");
        helper.assertTrue(contacts.size() == 1, "stair internal shape faces do not capture");
        level.setBlockAndUpdate(base, Blocks.WATER.defaultBlockState());
        helper.assertTrue(probe(helper, feetOnSlab).isEmpty(), "water has no supporting collision shape");
        helper.succeed();
    }

    @GameTest(template = "surface_empty")
    public static void realBlockSeamAndOutwardEscape(GameTestHelper helper) {
        BlockPos base = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(base, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(base.east(), Blocks.STONE.defaultBlockState());
        AABB body = new AABB(base.getX() + .4, base.getY() + 1, base.getZ() + .1,
                base.getX() + 1.6, base.getY() + 2.2, base.getZ() + .9);
        SurfaceAdhesionState state = new SurfaceAdhesionState();
        helper.assertTrue(state.update(body, probe(helper, body), new Vector3f(), false) == Direction.UP,
                "actual seam under the footprint captures");
        Vector3f velocity = new Vector3f();
        Vector3f intent = new Vector3f(.2f, .2f, 0);
        var settings = new SurfaceAdhesionMath.Settings(.4f, .3f, 6);
        double separation = 0;
        for (int tick = 0; tick < 100; tick++) {
            var normal = state.update(body, probe(helper, body), velocity, false);
            if (normal == null) break;
            var delta = FlightVelocityMath.advance(velocity, intent, true, false, .1f, .9f);
            SurfaceAdhesionMath.apply(velocity, normal, intent, delta, .1f, settings);
            body = body.move(velocity.x, velocity.y, velocity.z);
            separation += velocity.y;
        }
        helper.assertTrue(separation > .35 && state.rememberedNormal() == null,
                "oblique thrust escapes actual block support within a finite number of ticks");
        helper.succeed();
    }

    private static Map<Direction, SurfaceGeometry.Contact> probe(GameTestHelper helper, AABB body) {
        var boxes = new ArrayList<AABB>();
        for (var shape : helper.getLevel().getBlockCollisions(null, body.inflate(.3501))) boxes.addAll(shape.toAabbs());
        return SurfaceGeometry.findContacts(body, boxes);
    }
}
