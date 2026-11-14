package cn.rbq108.nextboundarycornerstone.body.collision;

import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxPose;
import cn.rbq108.nextboundarycornerstone.body.BodyBoxTag;
import cn.rbq108.nextboundarycornerstone.main;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Quaternionf;

import java.util.UUID;

@GameTestHolder("next_boundary_cornerstone")
@PrefixGameTestTemplate(false)
public final class PhysicalBodyGameTests {
    private record Fixture(ServerPlayer player, EmbeddedChannel channel) implements AutoCloseable {
        @Override public void close() {
            player.serverLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
            channel.finishAndReleaseAll();
        }
    }

    private static Fixture player(GameTestHelper helper, Vec3 relative) {
        var profile = new GameProfile(UUID.randomUUID(), "physical-body-test");
        var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), profile, ClientInformation.createDefault());
        player.setPos(helper.absoluteVec(relative));
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        player.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player,
                CommonListenerCookie.createInitial(profile, false)) {
            @Override public void send(Packet<?> packet) {}
        };
        helper.getLevel().addNewPlayer(player);
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(main.BASIC_BACKPACK.get()));
        player.setNoGravity(true);
        player.setPose(Pose.STANDING);
        player.refreshDimensions();
        player.getData(main.PLAYER_ROTATION).setHitboxPose(SegmentedHitboxPose.standing(new Quaternionf()));
        return new Fixture(player, channel);
    }

    @GameTest(template = "surface_empty")
    public static void oneBlockPassageAndActualHeadCeiling(GameTestHelper helper) {
        try (var fixture = player(helper, new Vec3(3.5, 3, 2.5))) {
            var player = fixture.player();
            for (int y = 2; y <= 5; y++) {
                helper.setBlock(new BlockPos(2, y, 3), Blocks.STONE);
                helper.setBlock(new BlockPos(4, y, 3), Blocks.STONE);
            }
            double originalZ = player.getZ();
            player.move(MoverType.SELF, new Vec3(0, 0, 3));
            helper.assertTrue(Math.abs(player.getZ() - originalZ - 3) < .00001, "full body passes through one-block-wide opening");

            BlockPos ceiling = helper.absolutePos(new BlockPos(3, 5, 5));
            helper.getLevel().setBlockAndUpdate(ceiling, Blocks.STONE.defaultBlockState());
            double expectedRise = ceiling.getY() - player.getBoundingBox().maxY;
            double originalY = player.getY();
            player.move(MoverType.SELF, new Vec3(0, 2, 0));
            helper.assertTrue(Math.abs(player.getY() - originalY - expectedRise) < .00001, "head piece, not old camera box, stops at ceiling");
            helper.succeed();
        }
    }

    @GameTest(template = "surface_empty")
    public static void realServerMovePacketAndGapQueriesUseCompoundShape(GameTestHelper helper) {
        try (var fixture = player(helper, new Vec3(3.5, 3, 2.8))) {
            var player = fixture.player();
            BlockPos corner = helper.absolutePos(new BlockPos(3, 3, 5));
            helper.getLevel().setBlockAndUpdate(corner, Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
            double targetZ = player.getZ() + 2;
            player.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(player.getX(), player.getY(), targetZ, false));
            helper.assertTrue(Math.abs(player.getZ() - targetZ) < .00001, "real server movement validation accepts newly entered envelope gap");
            var body = (PhysicsBox) player.getBoundingBox();
            AABB envelope = CompoundCollisionMath.envelope(body.pieces());
            helper.assertTrue(!helper.getLevel().noCollision(player, envelope), "test slab intersects broad envelope");
            helper.assertTrue(helper.getLevel().noCollision(player, body), "precise world query leaves actual body clear");
            var probe = new PhysicsBox(body.pieces());
            helper.assertTrue(helper.getLevel().noCollision(player, probe), "direct compound block query remains exact");
            try {
                var unionCache = PhysicsBox.class.getDeclaredField("shape");
                unionCache.setAccessible(true);
                helper.assertTrue(unionCache.get(probe) == null, "block query does not construct dense body voxel grid");
            } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
            var slab = helper.getLevel().getBlockState(corner).getCollisionShape(helper.getLevel(), corner).move(corner.getX(), corner.getY(), corner.getZ());
            helper.assertTrue(!Shapes.joinIsNotEmpty(Shapes.create(body), slab, BooleanOp.AND), "vanilla Shapes.create preserves compound voids");
            helper.assertTrue(player.getData(main.PLAYER_ROTATION).hitbox(player).pieces().size() == 16, "hit model remains segmented in both modes");
            BlockPos solid = helper.absolutePos(new BlockPos(4, 6, 4));
            helper.getLevel().setBlockAndUpdate(solid, Blocks.STONE.defaultBlockState());
            var gap = new PhysicsBox(java.util.List.of(
                    new AABB(-.5, 0, 0, -.1, 1, 1).move(solid),
                    new AABB(1.1, 0, 0, 1.5, 1, 1).move(solid)));
            helper.assertTrue(!helper.getLevel().noCollision(player, CompoundCollisionMath.envelope(gap.pieces())), "full stone block is inside query envelope");
            helper.assertTrue(helper.getLevel().noCollision(player, gap), "full-cube fast path also preserves compound gaps");
            helper.assertTrue(helper.getLevel().noBlockCollision(player, gap), "vanilla block-only query preserves compound gaps");
            helper.succeed();
        }
    }

    @GameTest(template = "surface_empty")
    public static void armsKeepHitsAndLabelsButDoNotStopMovement(GameTestHelper helper) {
        try (var fixture = player(helper, new Vec3(3.65, 3, 2.5))) {
            var player = fixture.player();
            var attachment = player.getData(main.PLAYER_ROTATION);
            var full = attachment.aabbModel(player);
            var physical = SegmentedPhysics.box(player);
            helper.assertTrue(physical.tags().equals(BodyBoxTag.MOVEMENT) && physical.pieces().size() == 10,
                    "actual player movement has tagged head, torso and six leg boxes");
            helper.assertTrue(attachment.hitbox(player).tags().equals(BodyBoxTag.ALL), "both hit modes retain all anatomy labels");
            var source = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
            for (BodyBoxTag tag : BodyBoxTag.ALL) {
                if (!tag.matches(BodyBoxTag.ARMS_MASK)) continue;
                AABB arm = full.pieces().get(tag.ordinal());
                var center = arm.getCenter();
                Vec3 start = center.add(0, 0, -2), end = center.add(0, 0, 2);
                helper.assertTrue(!physical.contains(center), "arm is not solid for movement: " + tag);
                helper.assertTrue(SegmentedPhysics.intersectsParts(player, arm.deflate(.02), tag.mask()), "device can query arm tag: " + tag);
                helper.assertTrue(!SegmentedPhysics.intersectsParts(player, arm.deflate(.02), BodyBoxTag.MOVEMENT_MASK),
                        "device mask distinguishes motion boxes from arms");
                var hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(helper.getLevel(), source,
                        start, end, new AABB(start, end).inflate(.001), entity -> entity == player, 0);
                helper.assertTrue(hit != null && hit.getEntity() == player, "real projectile still hits " + tag);
                var pick = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(source, start, end,
                        new AABB(start, end).inflate(.001), entity -> entity == player, 16);
                helper.assertTrue(pick != null && pick.getEntity() == player, "real melee ray still hits " + tag);
            }
            for (var tag : java.util.List.of(BodyBoxTag.LEFT_LEG_MIDDLE, BodyBoxTag.RIGHT_LEG_MIDDLE)) {
                helper.assertTrue(physical.tags().contains(tag) && physical.contains(full.pieces().get(tag.ordinal()).getCenter()),
                        "leg middle remains solid: " + tag);
            }
            for (int y = 2; y <= 5; y++) helper.setBlock(new BlockPos(4, y, 3), Blocks.STONE);
            double originalZ = player.getZ();
            player.move(MoverType.SELF, new Vec3(0, 0, 1));
            helper.assertTrue(Math.abs(player.getZ() - originalZ - 1) < .00001, "arms may enter wall without blocking movement");
            helper.assertTrue(helper.getLevel().noCollision(player), "real world query ignores embedded arms");
            helper.assertTrue(!helper.getLevel().noCollision(player, new PhysicsBox(attachment.aabbModel(player).pieces())),
                    "the former sixteen-box movement body would collide with this wall");
            helper.succeed();
        }
    }

    @GameTest(template = "surface_empty")
    public static void tinyPlayerIntermediateFloorContact(GameTestHelper helper) {
        try (var fixture = player(helper, new Vec3(3.5, 4, 3.5))) {
            var player = fixture.player();
            player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(.0625);
            player.refreshDimensions();
            var from = SegmentedHitboxPose.standing(new Quaternionf().rotateX(-.04f));
            var to = SegmentedHitboxPose.standing(new Quaternionf().rotateX(.04f));
            var middle = SegmentedHitboxPose.interpolate(from, to, .5f);
            double middleBottom = CompoundCollisionMath.envelope(RotationPlanner.boxes(middle, Vec3.ZERO,
                    player.getBbHeight(), player.getScale())).minY;
            BlockPos floor = helper.absolutePos(new BlockPos(3, 3, 3));
            helper.getLevel().setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
            player.setPos(player.getX(), floor.getY() + 1 - middleBottom - .00001, player.getZ());
            player.getData(main.PLAYER_ROTATION).setHitboxPose(from);
            helper.assertTrue(helper.getLevel().noCollision(player), "small player's initial pose clears the real floor");
            helper.assertTrue(helper.getLevel().noCollision(player, new PhysicsBox(RotationPlanner.boxes(to,
                    player.position(), player.getBbHeight(), player.getScale()))), "final pose also clears the real floor");
            helper.assertTrue(!helper.getLevel().noCollision(player, new PhysicsBox(RotationPlanner.boxes(middle,
                    player.position(), player.getBbHeight(), player.getScale()))), "intermediate pose reaches the real floor");
            var result = SegmentedPhysics.acceptPose(player, to);
            helper.assertTrue(!result.bodyBlocked() && result.offset().y > .00001, "server makes room for small player's intermediate turn");
            helper.assertTrue(helper.getLevel().noCollision(player), "small physical body remains clear after turn");
            helper.succeed();
        }
    }

    @GameTest(template = "surface_empty")
    public static void bodyTurnNudgesThenFallsBackToHeadOnly(GameTestHelper helper) {
        try (var fixture = player(helper, new Vec3(3.55, 3, 3.5))) {
            var player = fixture.player();
            for (int y = 1; y <= 6; y++) for (int z = 1; z <= 6; z++) helper.setBlock(new BlockPos(2, y, z), Blocks.STONE);
            double originalX = player.getX();
            var desired = SegmentedHitboxPose.standing(new Quaternionf().rotateZ((float) Math.PI / 4));
            var result = SegmentedPhysics.acceptPose(player, desired);
            helper.assertTrue(!result.bodyBlocked() && player.getX() > originalX + .1, "wall contact during turn displaces player into free space");
            helper.assertTrue(helper.getLevel().noCollision(player), "nudged physical body has no wall penetration");

            player.setPos(helper.absoluteVec(new Vec3(3.5, 3, 3.5)));
            player.getData(main.PLAYER_ROTATION).setHitboxPose(SegmentedHitboxPose.standing(new Quaternionf()));
            for (int y = 1; y <= 6; y++) for (int z = 1; z <= 6; z++) helper.setBlock(new BlockPos(4, y, z), Blocks.STONE);
            Vec3 before = player.position();
            result = SegmentedPhysics.acceptPose(player, SegmentedHitboxPose.standing(new Quaternionf().rotateZ((float) Math.PI / 2)));
            helper.assertTrue(result.bodyBlocked() && player.position().distanceTo(before) < .00001, "two walls prevent moving body to make room");
            helper.assertTrue(Math.abs(result.pose().body().w) > .9999, "body stays in its previous safe orientation");
            helper.assertTrue(Math.abs(result.pose().joints().getFirst().rotation().w) < .99, "head still turns independently");
            helper.assertTrue(helper.getLevel().noCollision(player), "head-only fallback is also nonpenetrating");
            helper.succeed();
        }
    }
}
