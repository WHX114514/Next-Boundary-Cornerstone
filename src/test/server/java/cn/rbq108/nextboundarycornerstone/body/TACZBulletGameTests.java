package cn.rbq108.nextboundarycornerstone.body;

import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.joml.Quaternionf;

@GameTestHolder("next_boundary_cornerstone")
@PrefixGameTestTemplate(false)
public final class TACZBulletGameTests {
    @GameTest(template = "surface_empty")
    public static void officialBulletStartsAtHeadEye(GameTestHelper helper) throws Exception {
        if (ModList.get().isLoaded("tacz")) WithTacz.verify(helper);
        helper.succeed();
    }

    // Keep the optional mod's types out of the enclosing class's method signatures.
    private static final class WithTacz {
        static void verify(GameTestHelper helper) throws Exception {
            var player = helper.makeMockPlayer();
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
            var attachment = cn.rbq108.nextboundarycornerstone.attachment.PlayerRotationAttachment.get(player);
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
