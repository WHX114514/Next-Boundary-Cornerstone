package cn.rbq108.nextboundarycornerstone.ServeMiao.communication;

import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.joml.Quaternionf;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkHandler {

    // 其他玩家的姿态记忆库
    public static final ConcurrentHashMap<UUID, Quaternionf> REMOTE_ROTATIONS = new ConcurrentHashMap<>();
    // 其他玩家的失重状态库
    public static final ConcurrentHashMap<UUID, Boolean> REMOTE_GRAVITY_STATES = new ConcurrentHashMap<>();

    private static final String PROTOCOL_VERSION = "1";

    // 1.20.1 核心：创建网络频道
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(main.MODID, "main_channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    // 给 main 调用的注册方法
    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(SyncRotationPayload.class, id++)
                .encoder(SyncRotationPayload::encode)
                .decoder(SyncRotationPayload::new)
                .consumerMainThread(SyncRotationPayload::handle)
                .add();
    }
}