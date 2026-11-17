package cn.rbq108.nextboundarycornerstone.motion;

import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxPose;
import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

@GameTestHolder("next_boundary_cornerstone")
@PrefixGameTestTemplate(false)
public final class FlightProjectileGameTests {
    private static final float RAD = (float) (Math.PI / 180);

    private static Player player(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var pos = helper.absolutePos(new BlockPos(1, 3, 1));
        player.setPos(pos.getX(), pos.getY(), pos.getZ());
        player.setNoGravity(true);
        player.getData(main.PLAYER_ROTATION).setLowGravity(true);
        return player;
    }

    private static void aim(Player player, Quaternionf view) {
        player.getData(main.PLAYER_ROTATION).setQuaternion(view);
        // Body and view deliberately differ, as when looking freely while the body is blocked.
        player.getData(main.PLAYER_ROTATION).setHitboxPose(SegmentedHitboxPose.standing(new Quaternionf()));
        player.setXRot(47);
        player.setYRot(-123);
    }

    private static Vec3 rotated(Quaternionf rotation, float x, float y, float z) {
        return new Vec3(new Vector3f(x, y, z).rotate(rotation));
    }

    @GameTest(template = "surface_empty")
    public static void publicAnglesAndPreviousFrameLaunchesAgree(GameTestHelper helper) {
        var player = player(helper);
        for (var view : List.of(new Quaternionf().rotationYXZ(.7f,.4f,1.5707964f),
                new Quaternionf().rotationYXZ(-.8f,2.1f,3.1415927f),
                new Quaternionf().rotationYXZ(.3f,1.5707f,.8f))) {
            aim(player, view);
            FlightAim.publishAngles(player);
            var forward = rotated(view,0,0,1);
            helper.assertTrue(player.getLookAngle().distanceTo(forward) < 1e-6, "server look vector follows quaternion");
            helper.assertTrue(Vec3.directionFromRotation(player.getXRot(),player.getYRot()).distanceTo(forward) < 1.75e-4,
                    "public angles agree within vanilla sine-table precision");
            player.xRotO = -27; player.yRotO = 63;
            var shot = new Snowball(helper.getLevel(),player);
            shot.shootFromRotation(player,player.xRotO,player.yRotO,0,2,0);
            helper.getLevel().addFreshEntity(shot);
            helper.assertTrue(shot.getDeltaMovement().distanceTo(Vec3.directionFromRotation(-27,63).scale(2)) < 1e-5,
                    "explicit previous-frame angles are not rotated again after join");
            shot.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "surface_empty")
    public static void realLaunchesFollowViewWithRolledOffsetsAndInheritedVelocity(GameTestHelper helper) {
        var player = player(helper);
        var views = List.of(new Quaternionf(), new Quaternionf().rotateY(90 * RAD),
                new Quaternionf().rotateX(90 * RAD), new Quaternionf().rotateX(-90 * RAD),
                new Quaternionf().rotationYXZ(65 * RAD, 30 * RAD, 90 * RAD),
                new Quaternionf().rotationYXZ(-120 * RAD, -65 * RAD, 180 * RAD),
                new Quaternionf().rotationYXZ(25 * RAD, 135 * RAD, -90 * RAD));
        for (var view : views) {
            aim(player, view);
            player.setDeltaMovement(.13, .21, -.08);
            for (boolean grounded : new boolean[] {false, true}) {
                player.setOnGround(grounded);
                var movement = player.getKnownMovement();
                var inherited = new Vec3(movement.x, grounded ? 0 : movement.y, movement.z);
                var projectiles = List.<Projectile>of(
                        new Arrow(helper.getLevel(), player, new ItemStack(Items.ARROW), null),
                        new Snowball(helper.getLevel(), player), new ThrownEnderpearl(helper.getLevel(), player),
                        new ThrownPotion(helper.getLevel(), player), new ThrownExperienceBottle(helper.getLevel(), player));
                for (var projectile : projectiles) {
                    var expectedOrigin = player.getEyePosition().subtract(rotated(view, 0, .1f, 0));
                    helper.assertTrue(projectile.position().distanceTo(expectedOrigin) < 1e-6,
                            "constructor uses lateral head eye and local down: " + projectile.getType()
                                    + " actual=" + projectile.position() + " expected=" + expectedOrigin + " view=" + view);
                    float lob = projectile instanceof ThrownPotion || projectile instanceof ThrownExperienceBottle ? -20 : 0;
                    projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), lob, 2.5f, 0);
                    var expected = rotated(view, 0, (float) -Math.sin(lob * RAD), 1).normalize().scale(2.5).add(inherited);
                    helper.assertTrue(projectile.getDeltaMovement().distanceTo(expected) < 1e-6,
                            "view aim, item lob, speed and inherited motion: " + projectile.getType());
                    // Reassigning ownership during a deflection must never teleport a flying projectile.
                    projectile.setPos(player.position().add(5, 6, 7));
                    var position = projectile.position();
                    projectile.setOwner(player);
                    helper.assertTrue(projectile.position().equals(position), "owner changes preserve projectile position");
                }
            }
            player.setDeltaMovement(Vec3.ZERO);
            var spread = new Arrow(helper.getLevel(), player, new ItemStack(Items.ARROW), null);
            spread.shootFromRotation(player, player.getXRot(), player.getYRot() + 10, 0, 3, 0);
            var expected = rotated(view, (float) -Math.sin(10 * RAD), 0, (float) Math.cos(10 * RAD)).scale(3);
            helper.assertTrue(spread.getDeltaMovement().distanceTo(expected) < 1e-6, "bow side spread follows rolled view up");
        }
        player.setNoGravity(false);
        player.setDeltaMovement(Vec3.ZERO);
        player.setXRot(30);
        player.setYRot(90);
        var ordinary = new Snowball(helper.getLevel(), player);
        ordinary.shootFromRotation(player, 30, 90, 0, 1.5f, 0);
        helper.assertTrue(ordinary.getDeltaMovement().distanceTo(new Vec3(-Math.cos(30 * RAD), -.5, 0).scale(1.5)) < 1e-4,
                "ordinary gravity retains vanilla direction");
        helper.assertTrue(ordinary.position().distanceTo(new Vec3(player.getX(), player.getEyeY() - .1f, player.getZ())) < 1e-6,
                "ordinary gravity retains vanilla origin");
        com.mojang.logging.LogUtils.getLogger().info("FLIGHT_PROJECTILE_LAUNCH_PASS orientations={} projectileCases=70", views.size());
        helper.succeed();
    }

    @GameTest(template = "surface_empty")
    public static void actualPotionBowAndCrossbowItemsUseFlightAim(GameTestHelper helper) {
        var player = player(helper);
        var view = new Quaternionf().rotationYXZ(70 * RAD, -25 * RAD, 90 * RAD);
        aim(player, view);
        var area = new AABB(player.position(), player.position()).inflate(5);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SPLASH_POTION));
        Items.SPLASH_POTION.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        var potions = helper.getLevel().getEntitiesOfClass(ThrownPotion.class, area, p -> p.getOwner() == player);
        helper.assertTrue(potions.size() == 1, "actual item spawns potion");
        var potionDirection = rotated(view, 0, (float) Math.sin(20 * RAD), 1).normalize();
        helper.assertTrue(potions.getFirst().getDeltaMovement().normalize().dot(potionDirection) > .995,
                "actual potion launch follows view with its normal lob");
        potions.forEach(Projectile::discard);

        var bow = new ItemStack(Items.BOW);
        player.setItemInHand(InteractionHand.MAIN_HAND, bow);
        player.getInventory().setItem(1, new ItemStack(Items.ARROW, 10));
        ((BowItem) Items.BOW).releaseUsing(bow, helper.getLevel(), player, Items.BOW.getUseDuration(bow, player) - 20);
        var arrows = helper.getLevel().getEntitiesOfClass(AbstractArrow.class, area, p -> p.getOwner() == player);
        helper.assertTrue(arrows.size() == 1, "actual bow spawns arrow");
        var forward = rotated(view, 0, 0, 1);
        helper.assertTrue(arrows.getFirst().getDeltaMovement().normalize().dot(forward) > .995, "actual bow follows view");
        arrows.forEach(Projectile::discard);

        var crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(List.of(new ItemStack(Items.ARROW))));
        player.setItemInHand(InteractionHand.MAIN_HAND, crossbow);
        ((CrossbowItem) Items.CROSSBOW).performShooting(helper.getLevel(), player, InteractionHand.MAIN_HAND, crossbow, 3.15f, 0, null);
        var bolts = helper.getLevel().getEntitiesOfClass(AbstractArrow.class, area, p -> p.getOwner() == player);
        helper.assertTrue(bolts.size() == 1, "actual crossbow spawns arrow");
        helper.assertTrue(bolts.getFirst().getDeltaMovement().normalize().dot(forward) > .99999, "crossbow uses flight view on server");
        bolts.forEach(Projectile::discard);
        com.mojang.logging.LogUtils.getLogger().info("FLIGHT_PROJECTILE_ITEMS_PASS potion=true bow=true crossbow=true");
        helper.succeed();
    }
}
