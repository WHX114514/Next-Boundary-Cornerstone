package cn.rbq108.nextboundarycornerstone.ServeMiao.communication;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkHandler {
    private static final String PROTOCOL = "compound-physics-forge-9";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(main.MODID, "main_channel"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        CHANNEL.messageBuilder(SyncRotationPayload.class, 0)
                .encoder(SyncRotationPayload::write).decoder(SyncRotationPayload::read)
                .consumerMainThread((payload, supplier) -> {
                    var context = new PacketContext(supplier.get());
                    if (supplier.get().getDirection().getReceptionSide().isServer()) handleDataOnServer(payload, context);
                    else handleDataOnClient(payload, context);
                }).add();
        CHANNEL.messageBuilder(SyncConfigPayload.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncConfigPayload::write).decoder(SyncConfigPayload::read)
                .consumerMainThread((payload, supplier) -> handleConfigOnClient(payload, new PacketContext(supplier.get())))
                .add();
    }

    public static void sendToAllPlayers(Object payload) { CHANNEL.send(PacketDistributor.ALL.noArg(), payload); }
    public static void sendToPlayer(net.minecraft.server.level.ServerPlayer player, Object payload) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }

    public record PacketContext(NetworkEvent.Context context) {
        public void enqueueWork(Runnable action) { context.enqueueWork(action); }
        public net.minecraft.world.entity.player.Player player() {
            return context.getDirection().getReceptionSide().isServer() ? context.getSender() : ClientPackets.player();
        }
    }


    //其他玩家的姿态记忆库：UUID转旋转四元数
    public static final ConcurrentHashMap<UUID, Quaternionf> REMOTE_ROTATIONS = new ConcurrentHashMap<>();
    //其他玩家的失重状态库
    public static final ConcurrentHashMap<UUID, Boolean> REMOTE_GRAVITY_STATES = new ConcurrentHashMap<>();

    // 缓存服务器的“旁观者模式原版恢复”配置（默认关闭，联机连接时由服务器包更新）
    public static boolean SERVER_RESTORE_VANILLA_SPECTATOR = false;

    // 客户端收到服务器配置同步包时的处理
    public static void handleConfigOnClient(final SyncConfigPayload payload, final PacketContext context) {
        context.enqueueWork(() -> {
            SERVER_RESTORE_VANILLA_SPECTATOR = payload.restoreVanillaSpectator();
        });
    }

    public static boolean isSpectatorAndRestored(net.minecraft.world.entity.player.Player player) {
        if (player.isSpectator()) {
            if (player.level().isClientSide()) {
                return SERVER_RESTORE_VANILLA_SPECTATOR;
            } else {
                return cn.rbq108.nextboundarycornerstone.VariableLibrary.Config.PHYSICS.restoreVanillaSpectator.get();
            }
        }
        return false;
    }

    //客户端收到包后的动作（画别人的时候用）
    public static void handleDataOnClient(final SyncRotationPayload payload, final PacketContext context) {
        context.enqueueWork(() -> {
            // 记录下这个玩家的旋转和状态
            REMOTE_ROTATIONS.put(payload.playerId(), payload.quat());
            REMOTE_GRAVITY_STATES.put(payload.playerId(), payload.lowGravity());
            var local = context.player();
            if (local == null) return;
            var target = local.level().getPlayerByUUID(payload.playerId());
            if (target != null && target != local) {
                var attachment = cn.rbq108.nextboundarycornerstone.attachment.PlayerRotationAttachment.get(target);
                attachment.setQuaternion(payload.quat());
                attachment.setLowGravity(payload.lowGravity());
                attachment.setHitboxPose(payload.hitboxPose());
                attachment.setBodyBlocked(payload.bodyConstrained());
            } else if (target == local && payload.bodyConstrained()) {
                PlayerSent.acceptServerConstraint(payload.hitboxPose());
            }
        });
    }

    //服务端收到包后的动作（负责转发给所有人）
    public static void handleDataOnServer(final SyncRotationPayload payload, final PacketContext context) {
        context.enqueueWork(() -> {
            net.minecraft.world.entity.player.Player player = context.player();
            if (player != null && player.getUUID().equals(payload.playerId())) {
                boolean lowGravity = player.isNoGravity() && !isSpectatorAndRestored(player);
                if (lowGravity != payload.lowGravity()) return;
                cn.rbq108.nextboundarycornerstone.attachment.PlayerRotationAttachment attachment = cn.rbq108.nextboundarycornerstone.attachment.PlayerRotationAttachment.get(player);
                attachment.setLowGravity(lowGravity);
                boolean constrained = false;
                var accepted = payload.hitboxPose();
                if (lowGravity) {
                    var result = cn.rbq108.nextboundarycornerstone.body.collision.SegmentedPhysics.acceptPose(player, accepted);
                    accepted = result.pose();
                    // Only replace local animation when the server actually rejects its pose.
                    // A predicted nudge keeps the requested pose; echoing it as a constraint
                    // would rewind the client's limbs by one network round trip.
                    constrained = result.bodyBlocked() && !accepted.equals(payload.hitboxPose());
                    if (result.offset().lengthSqr() > 1.0e-12 && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        // This is server-owned physical movement, already predicted by the client. Advance the
                        // validator baseline rather than teleporting every tick and clearing surface contact.
                        // A genuinely divergent client position still goes through vanilla collision validation.
                        serverPlayer.connection.resetPosition();
                    }
                } else {
                    attachment.setHitboxPose(null);
                    attachment.setBodyBlocked(false);
                }
                attachment.setQuaternion(payload.quat());
                cn.rbq108.nextboundarycornerstone.ServeMiao.communication.NetworkHandler.sendToAllPlayers(
                        new SyncRotationPayload(player.getUUID(), payload.quat(), lowGravity, accepted, constrained));
            }
        });
    }
}
