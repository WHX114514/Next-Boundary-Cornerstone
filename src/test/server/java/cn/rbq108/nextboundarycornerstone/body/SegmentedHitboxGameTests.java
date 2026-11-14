package cn.rbq108.nextboundarycornerstone.body;

import cn.rbq108.nextboundarycornerstone.ServeMiao.communication.SyncRotationPayload;
import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.body.OOBB.HitboxConfig;
import cn.rbq108.nextboundarycornerstone.body.OOBB.HitboxMode;
import cn.rbq108.nextboundarycornerstone.body.OOBB.OBBHitbox;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Quaternionf;

@GameTestHolder("next_boundary_cornerstone")
@PrefixGameTestTemplate(false)
public final class SegmentedHitboxGameTests {
    @GameTest(template = "surface_empty")
    public static void actualProjectileQueriesUsePiecesAndFindExtendedHead(GameTestHelper helper) {
        // Register in the level directly: no fake network login/unsupported custom-payload handshake.
        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "hitbox-test");
        var player = new net.minecraft.server.level.ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                profile, net.minecraft.server.level.ClientInformation.createDefault());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        var channel = new io.netty.channel.embedded.EmbeddedChannel(connection);
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection,
                player, net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false)) {
            @Override
            public void send(net.minecraft.network.protocol.Packet<?> packet) {}
        };
        try {
            var base = helper.absolutePos(new BlockPos(2, 2, 2));
            player.setPos(base.getX(), base.getY(), base.getZ());
            helper.getLevel().addNewPlayer(player);
            helper.assertTrue(helper.getLevel().players().contains(player), "fixture player is registered in tested level");
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(main.BASIC_BACKPACK.get()));
            player.setNoGravity(true);
            player.setPose(Pose.STANDING);
            player.refreshDimensions();
            var attachment = player.getData(main.PLAYER_ROTATION);
            attachment.setHitboxPose(SegmentedHitboxPose.standing(new Quaternionf()));
            AABB movement = player.getBoundingBox();
            var hitbox = (SegmentedAABB) SegmentedHitboxes.forHitQuery(player);
            helper.assertTrue(HitboxConfig.SPEC.isLoaded(), "hitbox SERVER config loaded on dedicated server");
            helper.assertTrue((hitbox instanceof OBBHitbox) == (HitboxConfig.mode() == HitboxMode.OBB),
                    "shape comes from actual server file, not player payload");
            helper.assertTrue(hitbox.pieces().size() == 16, "server has sixteen AABBs without client classes");
            helper.assertTrue(SegmentedHitboxes.forHitQuery(player) == hitbox, "unchanged queries reuse geometry");
            helper.assertTrue(player.getBoundingBox() == movement, "hit queries do not replace movement box");

            Vec3 head = hitbox.pieces().getFirst().getCenter();
            Vec3 start = head.add(0, 0, -2), end = head.add(0, 0, 2);
            AABB area = new AABB(start, end).inflate(.01);
            helper.assertTrue(movement instanceof cn.rbq108.nextboundarycornerstone.body.collision.PhysicsBox,
                    "actual movement uses a compound box");
            helper.assertTrue(hitbox != movement && ((SegmentedAABB) movement).pieces().size() == 10,
                    "hit model keeps sixteen boxes while movement omits both arms");
            helper.assertTrue(hitbox.tags().equals(BodyBoxTag.ALL), "both hit modes expose all sixteen anatomy tags");
            if (HitboxConfig.mode() == HitboxMode.AABB) {
                for (int i = 0; i < BodyBoxTag.MOVEMENT.size(); i++) helper.assertTrue(
                        ((SegmentedAABB) movement).pieces().get(i) == hitbox.pieces().get(BodyBoxTag.MOVEMENT.get(i).ordinal()),
                        "AABB hit/movement share retained box geometry without copying");
            }
            var source = helper.makeMockPlayer(GameType.SURVIVAL);
            helper.assertTrue(hitbox.clip(start, end).isPresent(), "head geometry intersects exact world-space ray");
            helper.assertTrue(SegmentedHitboxes.candidates(helper.getLevel(), source, area, entity -> entity == player,
                    helper.getLevel().getEntities(source, area, entity -> entity == player)).contains(player), "broad phase contains extended head");
            var hit = ProjectileUtil.getEntityHitResult(helper.getLevel(), source, start, end, area, entity -> entity == player, 0);
            helper.assertTrue(hit != null && hit.getEntity() == player, "server projectile hits extended head via broad-phase supplement");
            var picked = ProjectileUtil.getEntityHitResult(source, start, end, area, entity -> entity == player, 16);
            helper.assertTrue(picked != null && picked.getEntity() == player, "client-style melee/picking ray hits head");

            Vec3 gapStart = new Vec3(player.getX() + .55, head.y, start.z);
            Vec3 gapEnd = new Vec3(gapStart.x, head.y, end.z);
            AABB gapArea = new AABB(gapStart, gapEnd).inflate(.01);
            helper.assertTrue(ProjectileUtil.getEntityHitResult(helper.getLevel(), source, gapStart, gapEnd, gapArea,
                    entity -> entity == player, 0) == null, "empty space within old box is not a projectile hit");
            helper.assertTrue(ProjectileUtil.getEntityHitResult(source, gapStart, gapEnd, gapArea,
                    entity -> entity == player, 16) == null, "empty space is not a melee/picking hit");
            helper.assertTrue(ProjectileUtil.getEntityHitResult(helper.getLevel(), source, start, end, area,
                    entity -> false, 0) == null, "candidate supplement preserves caller predicate");

            // A head twisted 45 degrees has corners in its AABB that are outside its OBB.
            var neutral = SegmentedHitboxPose.standing(new Quaternionf());
            var joints = new java.util.ArrayList<>(neutral.joints());
            joints.set(0, new SegmentedHitboxPose.Joint(0, 0, 0, new Quaternionf().rotateY((float) Math.PI / 4)));
            attachment.setHitboxPose(new SegmentedHitboxPose(new Quaternionf(), 0, false, joints));
            Vec3 corner = head.add(.30, 0, .30);
            Vec3 cornerStart = corner.add(0, -.04, 0), cornerEnd = corner.add(0, .04, 0);
            AABB cornerArea = new AABB(cornerStart, cornerEnd).inflate(.001);
            var cornerHit = ProjectileUtil.getEntityHitResult(helper.getLevel(), source, cornerStart, cornerEnd,
                    cornerArea, entity -> entity == player, 0);
            boolean aabbMode = HitboxConfig.mode() == HitboxMode.AABB;
            helper.assertTrue((cornerHit != null) == aabbMode, "real projectile envelope-corner result follows server mode");
            var cornerPick = ProjectileUtil.getEntityHitResult(source, cornerStart, cornerEnd, cornerArea, entity -> entity == player, 1);
            helper.assertTrue((cornerPick != null) == aabbMode, "real melee/picking result follows server mode");
            var selected = (SegmentedAABB) SegmentedHitboxes.forHitQuery(player);
            helper.assertTrue(selected.clipHead(cornerStart, cornerEnd).isPresent() == aabbMode,
                    "headshot geometry also follows server mode");
            com.mojang.logging.LogUtils.getLogger().info("HITBOX_SERVER_MODE_PASS mode={}", HitboxConfig.mode());

            player.setPos(player.getX() + 1, player.getY(), player.getZ());
            var moved = (SegmentedAABB) SegmentedHitboxes.forHitQuery(player);
            helper.assertTrue(Math.abs(moved.pieces().getFirst().getCenter().x - head.x - 1) < .00001, "cached pieces follow movement");
            player.setNoGravity(false);
            helper.assertTrue(SegmentedHitboxes.forHitQuery(player) == player.getBoundingBox(), "normal gravity uses vanilla box");
            helper.succeed();
        } finally {
            helper.getLevel().removePlayerImmediately(player, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            channel.finishAndReleaseAll();
        }
    }

    @GameTest(template = "surface_empty")
    public static void posePacketRoundTripsAndResetHasNoSkeleton(GameTestHelper helper) {
        var id = java.util.UUID.randomUUID();
        var pose = SegmentedHitboxPose.standing(new Quaternionf().rotateXYZ(.3f, .5f, .7f));
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            pose = new SegmentedHitboxPose(pose.body(), 0, false, pose.joints(), true);
            new SyncRotationPayload(id, new Quaternionf(), true, pose, true).write(buf);
            var decoded = SyncRotationPayload.read(buf);
            helper.assertTrue(decoded.playerId().equals(id) && decoded.lowGravity() && decoded.hitboxPose().joints().size() == 6,
                    "pose and identity survive packet round trip");
            helper.assertTrue(Math.abs(decoded.hitboxPose().body().dot(pose.body())) > .99999, "body quaternion survives packet round trip");
            helper.assertTrue(decoded.bodyConstrained() && decoded.hitboxPose().swimming(),
                    "server constraint and accepted swimming posture survive packet round trip");
            helper.assertTrue(buf.readableBytes() == 0, "decoder consumes exact skeleton payload");
            buf.clear();
            new SyncRotationPayload(id, new Quaternionf(), false, null).write(buf);
            decoded = SyncRotationPayload.read(buf);
            helper.assertTrue(!decoded.lowGravity() && decoded.hitboxPose() == null && buf.readableBytes() == 0, "exit packet clears skeleton");
            helper.succeed();
        } finally {
            buf.release();
        }
    }
}
