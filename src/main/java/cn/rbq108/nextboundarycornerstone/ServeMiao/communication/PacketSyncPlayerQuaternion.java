package cn.rbq108.nextboundarycornerstone.ServeMiao.communication;

import cn.rbq108.nextboundarycornerstone.capability.PlayerRotationProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Quaternionf;
import java.util.function.Supplier;

public class PacketSyncPlayerQuaternion {
    private final float x, y, z, w;

    public PacketSyncPlayerQuaternion(Quaternionf quat) {
        this.x = quat.x;
        this.y = quat.y;
        this.z = quat.z;
        this.w = quat.w;
    }

    public PacketSyncPlayerQuaternion(FriendlyByteBuf buf) {
        this.x = buf.readFloat();
        this.y = buf.readFloat();
        this.z = buf.readFloat();
        this.w = buf.readFloat();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeFloat(x);
        buf.writeFloat(y);
        buf.writeFloat(z);
        buf.writeFloat(w);
    }

    // 处理收到四元数变更
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                player.getCapability(PlayerRotationProvider.PLAYER_ROTATION).ifPresent(cap -> {
                    cap.setQuaternion(new Quaternionf(x, y, z, w));
                });
            }
        });
        return true;
    }
}