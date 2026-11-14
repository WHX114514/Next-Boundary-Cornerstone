package cn.rbq108.nextboundarycornerstone.attachment;

import cn.rbq108.nextboundarycornerstone.body.SegmentedAABB;
import cn.rbq108.nextboundarycornerstone.body.BodyBoxTag;
import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxGeometry;
import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxPose;
import cn.rbq108.nextboundarycornerstone.body.OOBB.HitboxConfig;
import cn.rbq108.nextboundarycornerstone.body.OOBB.HitboxMode;
import cn.rbq108.nextboundarycornerstone.body.OOBB.OBBGeometry;
import cn.rbq108.nextboundarycornerstone.body.OOBB.OBBHitbox;
import cn.rbq108.nextboundarycornerstone.body.collision.PhysicsBox;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;

public class PlayerRotationAttachment {
    // Transient skeleton/cache: animation snapshots are never saved into player NBT.
    private SegmentedHitboxPose hitboxPose;
    private SegmentedHitboxPose previousHitboxPose;
    private SegmentedHitboxPose renderedHitboxPose;
    private float renderedPartialTick;
    private SegmentedAABB hitbox;
    private SegmentedAABB aabbModel;
    private PhysicsBox collision;
    private boolean bodyBlocked;
    private double boxX, boxY, boxZ;
    private float boxHeight, boxScale;
    private boolean boxSwimming;
    private HitboxMode boxMode;
    private final Quaternionf quaternion = new Quaternionf();
    private boolean lowGravity = false;

    public static final Codec<PlayerRotationAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("x").forGetter(a -> a.quaternion.x),
            Codec.FLOAT.fieldOf("y").forGetter(a -> a.quaternion.y),
            Codec.FLOAT.fieldOf("z").forGetter(a -> a.quaternion.z),
            Codec.FLOAT.fieldOf("w").forGetter(a -> a.quaternion.w),
            Codec.BOOL.fieldOf("lowGravity").forGetter(a -> a.lowGravity)
    ).apply(instance, (x, y, z, w, lg) -> {
        PlayerRotationAttachment a = new PlayerRotationAttachment();
        a.quaternion.set(x, y, z, w);
        a.lowGravity = lg;
        return a;
    }));

    public Quaternionf getQuaternion() {
        return quaternion;
    }

    public void setQuaternion(Quaternionf quat) {
        this.quaternion.set(quat);
        hitbox = null;
        collision = null;
        aabbModel = null;
    }

    public boolean isLowGravity() {
        return lowGravity;
    }

    public void setLowGravity(boolean v) {
        this.lowGravity = v;
        if (!v) setHitboxPose(null);
    }

    public SegmentedHitboxPose getHitboxPose() {
        return hitboxPose;
    }

    public void setHitboxPose(SegmentedHitboxPose pose) {
        previousHitboxPose = hitboxPose;
        hitboxPose = pose;
        hitbox = null;
        collision = null;
        renderedHitboxPose = null;
        aabbModel = null;
    }

    public SegmentedHitboxPose renderHitboxPose(float partialTick) {
        if (hitboxPose == null || previousHitboxPose == null || hitboxPose.slim() != previousHitboxPose.slim()) return hitboxPose;
        if (renderedHitboxPose == null || renderedPartialTick != partialTick) {
            renderedPartialTick = partialTick;
            renderedHitboxPose = SegmentedHitboxPose.interpolate(previousHitboxPose, hitboxPose, partialTick);
        }
        return renderedHitboxPose;
    }

    private void checkGeometryCache(Player player) {
        boolean swimming = player.hasPose(Pose.SWIMMING);
        HitboxMode mode = HitboxConfig.mode();
        if (boxX != player.getX() || boxY != player.getY() || boxZ != player.getZ()
                || boxHeight != player.getBbHeight() || boxScale != player.getScale() || boxSwimming != swimming || boxMode != mode) {
            boxX = player.getX(); boxY = player.getY(); boxZ = player.getZ();
            boxHeight = player.getBbHeight(); boxScale = player.getScale(); boxSwimming = swimming;
            boxMode = mode;
            hitbox = null;
            collision = null;
            aabbModel = null;
        }
    }

    public SegmentedHitboxPose poseOrDefault() {
        return hitboxPose != null ? hitboxPose : SegmentedHitboxPose.standing(quaternion);
    }

    public PhysicsBox collision(Player player) {
        checkGeometryCache(player);
        if (collision == null) {
            var pieces = HitboxConfig.mode() == HitboxMode.AABB ? aabbModel(player).pieces(BodyBoxTag.MOVEMENT_MASK)
                    : SegmentedHitboxGeometry.build(poseOrDefault(), boxHeight, boxScale, boxSwimming, BodyBoxTag.MOVEMENT_MASK)
                    .stream().map(box -> box.move(boxX, boxY, boxZ)).toList();
            collision = new PhysicsBox(pieces, BodyBoxTag.MOVEMENT);
        }
        return collision;
    }

    public SegmentedAABB hitbox(Player player) {
        checkGeometryCache(player);
        if (HitboxConfig.mode() == HitboxMode.AABB) return aabbModel(player);
        if (hitbox == null) {
            var pieces = OBBGeometry.build(poseOrDefault(), boxHeight, boxScale, boxSwimming);
            hitbox = new OBBHitbox(pieces.stream().map(box -> box.move(boxX, boxY, boxZ)).toList(), BodyBoxTag.ALL);
        }
        return hitbox;
    }

    /** All sixteen tagged AABBs, also available to devices regardless of the hit-query mode. */
    public SegmentedAABB aabbModel(Player player) {
        checkGeometryCache(player);
        if (aabbModel == null) {
            var pieces = SegmentedHitboxGeometry.build(poseOrDefault(), boxHeight, boxScale, boxSwimming);
            aabbModel = new SegmentedAABB(pieces.stream().map(box -> box.move(boxX, boxY, boxZ)).toList(), BodyBoxTag.ALL);
        }
        return aabbModel;
    }

    public boolean isBodyBlocked() { return bodyBlocked; }
    public void setBodyBlocked(boolean blocked) { bodyBlocked = blocked; }
}
