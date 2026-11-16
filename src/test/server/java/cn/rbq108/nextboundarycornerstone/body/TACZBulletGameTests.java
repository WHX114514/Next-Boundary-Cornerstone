package cn.rbq108.nextboundarycornerstone.body;

import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Quaternionf;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3f;

@GameTestHolder("next_boundary_cornerstone")
@PrefixGameTestTemplate(false)
public final class TACZBulletGameTests {
    @GameTest(template = "surface_empty")
    public static void officialBulletStartsAtHeadEye(GameTestHelper helper) throws Exception {
        if (ModList.get().isLoaded("tacz")) WithTacz.verify(helper);
        helper.succeed();
    }

    @GameTest(template = "surface_empty")
    public static void officialBulletLaunchesUseFlightViewOnce(GameTestHelper helper) {
        if (ModList.get().isLoaded("tacz")) WithTacz.verifyLaunches(helper);
        helper.succeed();
    }

    // Keep the optional mod's types out of the enclosing class's method signatures.
    private static final class WithTacz {
        static void verifyLaunches(GameTestHelper helper) {
            var player = helper.makeMockPlayer(GameType.CREATIVE);
            player.setPos(helper.absoluteVec(new Vec3(2, 5, 2)));
            var id = ResourceLocation.parse("tacz:m4a1");
            var gun = com.tacz.guns.api.item.builder.GunItemBuilder.create().setId(id).build();
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, gun);
            var data = com.tacz.guns.api.TimelessAPI.getCommonGunIndex(id).orElseThrow().getGunData();
            var operator = com.tacz.guns.api.entity.IGunOperator.fromLivingEntity(player);
            var cache = new com.tacz.guns.resource.modifier.AttachmentCacheProperty();
            cache.eval(gun, data);
            operator.updateCacheProperty(cache);
            var attachment = player.getData(main.PLAYER_ROTATION);
            var views = new Quaternionf[] {new Quaternionf().rotateZ((float) Math.PI / 2),
                    new Quaternionf().rotationYXZ(.7f, .4f, (float) Math.PI),
                    new Quaternionf().rotationYXZ(-.5f, 2.1f, -.8f),
                    new Quaternionf().rotationYXZ(.6f, -.3f, 1.1f)};
            int cases = 0;
            for (var view : views) {
                player.setNoGravity(true);
                player.setXRot(47);
                player.setYRot(-123);
                attachment.setQuaternion(view);
                attachment.setHitboxPose(SegmentedHitboxPose.standing(new Quaternionf()));
                for (boolean grounded : new boolean[] {false, true}) {
                    player.setOnGround(grounded);
                    var stationary = new Vec3[3];
                    for (Vec3 motion : new Vec3[] {Vec3.ZERO, new Vec3(.13, .21, -.08)}) {
                        player.setDeltaMovement(motion);
                        for (int path = 0; path < 3; path++) {
                            var bullet = new com.tacz.guns.entity.EntityKineticBullet(helper.getLevel(), player, gun,
                                    data.getAmmoId(), id, true, data, data.getBulletData());
                            bullet.getRandom().setSeed(20261116L);
                            float speed = 3;
                            Vec3 expected;
                            if (path == 2) {
                                // The custom overload bypasses vanilla, including its launch Mixin.
                                bullet.shootFromRotation(player, player.getXRot() + 6, player.getYRot() - 4,
                                        0, speed, new Vector2d(.35, -.22));
                                var local = new Vector3d(.35, -.22, 8)
                                        .rotateX(Math.toRadians(6)).rotateY(Math.toRadians(4)).normalize().mul(speed);
                                expected = new Vec3(view.transform(new Vector3f((float) local.x, (float) local.y, (float) local.z)));
                            } else {
                                // Exercise the real gun's default spread dispatcher, not a fabricated velocity.
                                ((com.tacz.guns.api.item.gun.AbstractGunItem) gun.getItem()).doBulletSpread(
                                        operator.getDataHolder(), gun, player, bullet, 0, speed, path == 0 ? 0 : .5f,
                                        player.getXRot(), player.getYRot());
                                expected = new Vec3(view.transform(new Vector3f(0, 0, speed)));
                            }
                            // Vanilla adds random spread after normalization; preserve that speed variation too.
                            var inherited = new Vec3(motion.x, grounded ? 0 : motion.y, motion.z);
                            double launchSpeed = bullet.getDeltaMovement().subtract(inherited).length();
                            helper.assertTrue(helper.getLevel().addFreshEntity(bullet), "launched bullet joins level");
                            var actual = bullet.getDeltaMovement();
                            String label = "path=" + path + " grounded=" + grounded + " motion=" + motion + " view=" + view;
                            helper.assertTrue(Math.abs(actual.length() - launchSpeed) < 1e-5, "generated speed retained: " + label);
                            if (motion == Vec3.ZERO) stationary[path] = actual;
                            else helper.assertTrue(actual.distanceTo(stationary[path]) < 1e-6, "moving and stationary launches agree: " + label);
                            if (path == 1) {
                                helper.assertTrue(actual.normalize().dot(expected.normalize()) > .995, "spread stays around flight aim: " + label);
                                helper.assertTrue(actual.distanceTo(expected) > 1e-7, "nonzero spread retained: " + label);
                            } else {
                                helper.assertTrue(actual.distanceTo(expected) < 1e-5,
                                        "one flight rotation after join: " + label + " actual=" + actual + " expected=" + expected);
                            }
                            bullet.discard();
                            cases++;
                        }
                    }
                }
            }
            // A stale flight quaternion must not affect ordinary gravity or the exit from flight.
            player.setNoGravity(false);
            for (boolean grounded : new boolean[] {false, true}) {
                player.setOnGround(grounded);
                var motion = new Vec3(.13, .21, -.08);
                player.setDeltaMovement(motion);
                var inherited = new Vec3(motion.x, grounded ? 0 : motion.y, motion.z);
                for (boolean custom : new boolean[] {false, true}) {
                    var bullet = new com.tacz.guns.entity.EntityKineticBullet(helper.getLevel(), player, gun,
                            data.getAmmoId(), id, true, data, data.getBulletData());
                    if (custom) bullet.shootFromRotation(player, 47, -123, 0, 3, new Vector2d());
                    else bullet.shootFromRotation(player, 47, -123, 0, 3, 0);
                    var beforeJoin = bullet.getDeltaMovement();
                    helper.assertTrue(helper.getLevel().addFreshEntity(bullet), "ordinary bullet joins level");
                    helper.assertTrue(bullet.getDeltaMovement().distanceTo(beforeJoin) < 1e-9, "ordinary join preserves velocity");
                    helper.assertTrue(beforeJoin.distanceTo(Vec3.directionFromRotation(47, -123).scale(3).add(inherited)) < 1e-3,
                            "ordinary gravity retains TaCZ aim and inherited motion: custom=" + custom + " velocity=" + beforeJoin);
                    bullet.discard();
                    cases++;
                }
            }
            com.mojang.logging.LogUtils.getLogger().info("TACZ_BULLET_LAUNCH_QA_PASS cases={}", cases);
        }

        static void verify(GameTestHelper helper) throws Exception {
            var player = helper.makeMockPlayer(GameType.CREATIVE);
            var id = ResourceLocation.parse("tacz:m4a1");
            var gun = com.tacz.guns.api.item.builder.GunItemBuilder.create().setId(id).build();
            var data = com.tacz.guns.api.TimelessAPI.getCommonGunIndex(id).orElseThrow().getGunData();
            var cache = new com.tacz.guns.resource.modifier.AttachmentCacheProperty();
            cache.eval(gun, data);
            com.tacz.guns.api.entity.IGunOperator.fromLivingEntity(player).updateCacheProperty(cache);
            var startField = com.tacz.guns.entity.EntityKineticBullet.class.getDeclaredField("startPos");
            startField.setAccessible(true);
            player.setPos(20_000_000, 84, -20_000_000);
            player.xOld = player.getX() - .6;
            player.yOld = player.getY() + .2;
            player.zOld = player.getZ() + .4;
            var attachment = player.getData(main.PLAYER_ROTATION);
            Quaternionf[] rotations = {new Quaternionf(), new Quaternionf().rotateZ((float) Math.PI / 2),
                    new Quaternionf().rotateX((float) Math.PI / 2), new Quaternionf().rotateXYZ(.7f, -.5f, 1.1f)};
            for (boolean floating : new boolean[]{false, true}) {
                player.setPose(floating ? Pose.SWIMMING : Pose.STANDING);
                for (Quaternionf rotation : rotations) {
                    player.setNoGravity(true);
                    attachment.setQuaternion(rotation);
                    var pose = SegmentedHitboxPose.standing(rotation);
                    var joints = new java.util.ArrayList<>(pose.joints());
                    var head = joints.get(0);
                    joints.set(0, new SegmentedHitboxPose.Joint(head.x(), head.y(), head.z(),
                            new Quaternionf().rotateYXZ(.4f, -.3f, .2f)));
                    attachment.setHitboxPose(new SegmentedHitboxPose(rotation, floating ? 1 : 0, false, joints, floating));
                    var bullet = new com.tacz.guns.entity.EntityKineticBullet(helper.getLevel(), player, gun,
                            data.getAmmoId(), id, true, data, data.getBulletData());
                    Vec3 eye = player.getEyePosition();
                    helper.assertTrue(bullet.position().distanceTo(eye) < 1e-7,
                            "official constructor uses full head eye, including lateral displacement");
                    helper.assertTrue(((Vec3) startField.get(bullet)).distanceTo(eye) < 1e-7,
                            "damage falloff starts at corrected eye");
                    // Exercise the real join event too: it must not rotate the corrected origin again.
                    bullet.setDeltaMovement(0, 0, 3);
                    helper.assertTrue(helper.getLevel().addFreshEntity(bullet), "bullet enters the level");
                    helper.assertTrue(bullet.position().distanceTo(eye) < 1e-7, "join handler preserves head origin");
                    bullet.discard();
                }
            }
            player.setNoGravity(false);
            var normal = new com.tacz.guns.entity.EntityKineticBullet(helper.getLevel(), player, gun,
                    data.getAmmoId(), id, true, data, data.getBulletData());
            var expected = new Vec3((player.xOld + player.getX()) / 2,
                    (player.yOld + player.getY()) / 2 + player.getEyeHeight(), (player.zOld + player.getZ()) / 2);
            helper.assertTrue(normal.position().distanceTo(expected) < 1e-7, "ordinary gravity preserves TaCZ origin");
            com.mojang.logging.LogUtils.getLogger().info("TACZ_BULLET_ORIGIN_QA_PASS cases=9");
        }
    }
}
