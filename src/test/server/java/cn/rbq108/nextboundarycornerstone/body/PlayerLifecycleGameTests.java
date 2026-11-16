package cn.rbq108.nextboundarycornerstone.body;

import cn.rbq108.nextboundarycornerstone.attachment.PlayerRotationAttachment;
import cn.rbq108.nextboundarycornerstone.capability.PlayerRotationProvider;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.joml.Quaternionf;

@GameTestHolder("next_boundary_cornerstone")
@PrefixGameTestTemplate(false)
public final class PlayerLifecycleGameTests {
    @GameTest(template = "surface_empty")
    public static void unavailableRotationUsesVanillaEyesAndBox(GameTestHelper helper) {
        var player = helper.makeMockPlayer();
        player.setNoGravity(true);
        PlayerRotationAttachment.get(player).setQuaternion(new Quaternionf().rotateZ(1.2f));
        player.invalidateCaps();
        // Camera ticks can still read the old player after its capabilities were invalidated.
        helper.assertTrue(Float.isFinite(player.getEyeHeight()), "invalidated player's eye height is readable");
        helper.assertTrue(Double.isFinite(player.getEyePosition().y), "invalidated player's eye position is readable");
        helper.assertTrue(!SegmentedHitboxes.enabled(player), "invalid capability disables custom geometry");
        helper.assertTrue(!(player.getBoundingBox() instanceof cn.rbq108.nextboundarycornerstone.body.collision.PhysicsBox),
                "fluid and movement queries use the vanilla box during transfer");
        player.remove(Entity.RemovalReason.CHANGED_DIMENSION);
        helper.assertTrue(Float.isFinite(player.getEyeHeight()), "removed camera target remains readable");
        helper.succeed();
    }

    @GameTest(template = "surface_empty")
    public static void rotationCanReviveAcrossRepeatedDimensionTransfers(GameTestHelper helper) {
        var player = helper.makeMockPlayer();
        player.setNoGravity(true);
        var rotation = PlayerRotationAttachment.get(player);
        var view = new Quaternionf().rotationXYZ(.3f, -.4f, 1.1f);
        rotation.setQuaternion(view);
        rotation.setLowGravity(true);
        for (int i = 0; i < 3; i++) {
            var previous = player.getCapability(PlayerRotationProvider.PLAYER_ROTATION);
            player.invalidateCaps();
            helper.assertTrue(!previous.isPresent(), "old optional stays invalid");
            player.reviveCaps();
            helper.assertTrue(player.getCapability(PlayerRotationProvider.PLAYER_ROTATION).isPresent(), "capability revives");
            helper.assertTrue(PlayerRotationAttachment.get(player) == rotation, "revival preserves the backend");
            helper.assertTrue(rotation.getQuaternion().equals(view) && rotation.isLowGravity(), "rotation and gravity retained");
            helper.assertTrue(!previous.isPresent(), "renewal does not revive stale external references");
        }
        helper.succeed();
    }

    @GameTest(template = "surface_empty")
    public static void deathAndEndReturnClonesCopyOnlyPersistentRotation(GameTestHelper helper) {
        for (boolean death : new boolean[] {true, false}) {
          for (boolean invalidated : new boolean[] {true, false}) {
            var oldPlayer = helper.makeMockPlayer();
            var newPlayer = helper.makeMockPlayer();
            var old = PlayerRotationAttachment.get(oldPlayer);
            old.setQuaternion(new Quaternionf().rotateXYZ(.2f, .5f, .8f));
            old.setLowGravity(true);
            old.setHitboxPose(SegmentedHitboxPose.standing(old.getQuaternion()));
            old.setBodyBlocked(true);
            if (invalidated) oldPlayer.invalidateCaps();
            var event = new PlayerEvent.Clone(newPlayer, oldPlayer, death);
            // Check our restoration contract directly: other mods may revive or invalidate
            // the source themselves during full event-bus dispatch.
            cn.rbq108.nextboundarycornerstone.event.CapabilityEventHandler.clone(event);
            var copy = PlayerRotationAttachment.get(newPlayer);
            helper.assertTrue(copy != old && copy.getQuaternion().equals(old.getQuaternion()) && copy.isLowGravity(), "clone retains persistent state");
            helper.assertTrue(copy.getHitboxPose() == null && !copy.isBodyBlocked(), "clone has independent transient state");
            helper.assertTrue(oldPlayer.getCapability(PlayerRotationProvider.PLAYER_ROTATION).isPresent() != invalidated,
                    "cloning restores the original capability validity");
            if (invalidated) {
                copy.setQuaternion(new Quaternionf());
                copy.setLowGravity(false);
                MinecraftForge.EVENT_BUS.post(event);
                helper.assertTrue(copy.getQuaternion().equals(old.getQuaternion()) && copy.isLowGravity(),
                        "full event bus copies persistent state with optional mods present");
            }
          }
        }
        helper.succeed();
    }
}
