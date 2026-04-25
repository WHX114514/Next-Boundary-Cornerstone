package cn.rbq108.nextboundarycornerstone.ServeMiao.communication;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Quaternionf;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncRotationPayload {
    public final UUID playerId;
    public final Quaternionf quat;
    public final boolean lowGravity;

    // 发送包时用的构造函数
    public SyncRotationPayload(UUID playerId, Quaternionf quat, boolean lowGravity) {
        this.playerId = playerId;
        this.quat = quat;
        this.lowGravity = lowGravity;
    }

    // 接收包时用的解码函数 (替代 1.21 的 StreamCodec)
    public SyncRotationPayload(FriendlyByteBuf buf) {
        this.playerId = buf.readUUID();
        this.quat = new Quaternionf(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
        this.lowGravity = buf.readBoolean();
    }

    // 序列化打包
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerId);
        buf.writeFloat(quat.x);
        buf.writeFloat(quat.y);
        buf.writeFloat(quat.z);
        buf.writeFloat(quat.w);
        buf.writeBoolean(lowGravity);
    }

    // 核心处理逻辑：双端统一在这里处理 (替代 1.21 的 handleDataOnClient/Server)
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isServer()) {
                // 如果是服务端收到了包：负责广播给所有在线玩家
                NetworkHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), this);
            } else {
                // 如果是客户端收到了包：记录到本地渲染库中
                NetworkHandler.REMOTE_ROTATIONS.put(this.playerId, this.quat);
                NetworkHandler.REMOTE_GRAVITY_STATES.put(this.playerId, this.lowGravity);
            }
        });
        ctx.get().setPacketHandled(true); // 标记为已处理
    }
}