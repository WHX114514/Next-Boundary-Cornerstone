package cn.rbq108.nextboundarycornerstone.attachment;

import cn.rbq108.nextboundarycornerstone.body.SegmentedAABB;
import cn.rbq108.nextboundarycornerstone.body.BodyBoxTag;
import cn.rbq108.nextboundarycornerstone.body.BodyEyes;
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
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class PlayerRotationAttachment {
    /** Removed/dead players may remain camera targets while Forge invalidates their capabilities. */
    public static boolean isAvailable(net.minecraft.world.entity.Entity entity) {
        return entity instanceof Player && entity.isAlive() && !entity.isRemoved()
                && entity.getCapability(cn.rbq108.nextboundarycornerstone.capability.PlayerRotationProvider.PLAYER_ROTATION).isPresent();
    }

    public static PlayerRotationAttachment get(net.minecraft.world.entity.Entity entity) {
        return entity.getCapability(cn.rbq108.nextboundarycornerstone.capability.PlayerRotationProvider.PLAYER_ROTATION)
                .orElseThrow(() -> new IllegalStateException("Missing player rotation capability: " + entity.getUUID()));
    }

    // Transient skeleton/cache: animation snapshots are never saved into player NBT.
    private SegmentedHitboxPose hitboxPose;
    private SegmentedHitboxPose previousHitboxPose;
    private SegmentedHitboxPose renderedHitboxPose;
    private float renderedPartialTick;
    private SegmentedAABB hitbox;
    private SegmentedAABB aabbModel;
    private PhysicsBox collision;
    private Vec3 eyeOffset, renderedEyeOffset;
    private float eyePartialTick;
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
            Codec.BOOL.optionalFieldOf("lowGravity", false).forGetter(a -> a.lowGravity)
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
        eyeOffset = renderedEyeOffset = null;
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
        eyeOffset = renderedEyeOffset = null;
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
                || boxHeight != player.getBbHeight() || boxScale != 1.0f || boxSwimming != swimming || boxMode != mode) {
            boxX = player.getX(); boxY = player.getY(); boxZ = player.getZ();
            boxHeight = player.getBbHeight(); boxScale = 1.0f; boxSwimming = swimming;
            boxMode = mode;
            hitbox = null;
            collision = null;
            aabbModel = null;
            eyeOffset = renderedEyeOffset = null;
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

    public Vec3 eyeOffset(Player player) {
        checkGeometryCache(player);
        if (eyeOffset == null) eyeOffset = BodyEyes.offset(poseOrDefault(), boxHeight, boxScale);
        return eyeOffset;
    }

    public Vec3 renderEyeOffset(Player player, float partialTick) {
        checkGeometryCache(player);
        if (renderedEyeOffset == null || eyePartialTick != partialTick) {
            eyePartialTick = partialTick;
            var pose = renderHitboxPose(partialTick);
            renderedEyeOffset = BodyEyes.offset(pose != null ? pose : poseOrDefault(), boxHeight, boxScale);
        }
        return renderedEyeOffset;
    }
}
