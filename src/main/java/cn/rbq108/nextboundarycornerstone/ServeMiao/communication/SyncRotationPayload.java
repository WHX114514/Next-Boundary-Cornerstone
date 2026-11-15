package cn.rbq108.nextboundarycornerstone.ServeMiao.communication;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxPose;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import java.util.UUID;

public record SyncRotationPayload(UUID playerId, Quaternionf quat, boolean lowGravity, SegmentedHitboxPose hitboxPose, boolean bodyConstrained) {
    public SyncRotationPayload(UUID playerId, Quaternionf quat, boolean lowGravity, SegmentedHitboxPose hitboxPose) {
        this(playerId, quat, lowGravity, hitboxPose, false);
    }


    // 序列化与反序列化（人话：解包）

    public static SyncRotationPayload read(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        Quaternionf rotation = SegmentedHitboxPose.normalized(new Quaternionf(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat()));
        boolean lowGravity = buf.readBoolean();
        var pose = lowGravity ? SegmentedHitboxPose.read(buf) : null;
        return new SyncRotationPayload(id, rotation, lowGravity, pose, buf.readBoolean());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(playerId);
        buf.writeFloat(quat.x);
        buf.writeFloat(quat.y);
        buf.writeFloat(quat.z);
        buf.writeFloat(quat.w);
        buf.writeBoolean(lowGravity);
        if (lowGravity) hitboxPose.write(buf);
        buf.writeBoolean(bodyConstrained);
    }


}
