package cn.rbq108.nextboundarycornerstone.ServeMiao.communication;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.joml.Quaternionf;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkHandler {

    //其他玩家的姿态记忆库：UUID转旋转四元数
    public static final ConcurrentHashMap<UUID, Quaternionf> REMOTE_ROTATIONS = new ConcurrentHashMap<>();
    //其他玩家的失重状态库
    public static final ConcurrentHashMap<UUID, Boolean> REMOTE_GRAVITY_STATES = new ConcurrentHashMap<>();

    //客户端收到包后的动作（画别人的时候用）
    public static void handleDataOnClient(final SyncRotationPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            // 记录下这个玩家的旋转和状态
            REMOTE_ROTATIONS.put(payload.playerId(), payload.quat());
            REMOTE_GRAVITY_STATES.put(payload.playerId(), payload.lowGravity());
        });
    }

    //服务端收到包后的动作（负责转发给所有人）
    public static void handleDataOnServer(final SyncRotationPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            net.minecraft.world.entity.player.Player player = context.player();
            if (player != null) {
                cn.rbq108.nextboundarycornerstone.attachment.PlayerRotationAttachment attachment = player.getData(cn.rbq108.nextboundarycornerstone.main.PLAYER_ROTATION);
                attachment.setQuaternion(payload.quat());
                attachment.setLowGravity(payload.lowGravity());
            }
            //context.broadcast(payload);这个不能用，会坏
            //要使用 PacketDistributor 广播给所有在线玩家
            // 这一行代码会把 A 玩家的姿态数据，再同步给服务器里的所有人
            net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(payload);
        });
    }
}