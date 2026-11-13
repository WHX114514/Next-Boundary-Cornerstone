package cn.rbq108.nextboundarycornerstone.attachment;

import cn.rbq108.nextboundarycornerstone.body.SegmentedAABB;
import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxGeometry;
import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxPose;
import cn.rbq108.nextboundarycornerstone.body.OOBB.HitboxConfig;
import cn.rbq108.nextboundarycornerstone.body.OOBB.HitboxMode;
import cn.rbq108.nextboundarycornerstone.body.OOBB.OBBGeometry;
import cn.rbq108.nextboundarycornerstone.body.OOBB.OBBHitbox;
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
        renderedHitboxPose = null;
    }

    public SegmentedHitboxPose renderHitboxPose(float partialTick) {
        if (hitboxPose == null || previousHitboxPose == null || hitboxPose.slim() != previousHitboxPose.slim()) return hitboxPose;
        if (renderedHitboxPose == null || renderedPartialTick != partialTick) {
            renderedPartialTick = partialTick;
            renderedHitboxPose = SegmentedHitboxPose.interpolate(previousHitboxPose, hitboxPose, partialTick);
        }
        return renderedHitboxPose;
    }

    public SegmentedAABB hitbox(Player player) {
        boolean swimming = player.hasPose(Pose.SWIMMING);
        HitboxMode mode = HitboxConfig.mode();
        if (hitbox == null || boxX != player.getX() || boxY != player.getY() || boxZ != player.getZ()
                || boxHeight != player.getBbHeight() || boxScale != player.getScale() || boxSwimming != swimming || boxMode != mode) {
            boxX = player.getX(); boxY = player.getY(); boxZ = player.getZ();
            boxHeight = player.getBbHeight(); boxScale = player.getScale(); boxSwimming = swimming;
            boxMode = mode;
            var pose = hitboxPose != null ? hitboxPose
                    : SegmentedHitboxPose.standing(quaternion);
            if (mode == HitboxMode.OBB) {
                var pieces = OBBGeometry.build(pose, boxHeight, boxScale, swimming);
                hitbox = new OBBHitbox(pieces.stream().map(box -> box.move(boxX, boxY, boxZ)).toList());
            } else {
                var pieces = SegmentedHitboxGeometry.build(pose, boxHeight, boxScale, swimming);
                hitbox = new SegmentedAABB(pieces.stream().map(box -> box.move(boxX, boxY, boxZ)).toList());
            }
        }
        return hitbox;
    }
}
