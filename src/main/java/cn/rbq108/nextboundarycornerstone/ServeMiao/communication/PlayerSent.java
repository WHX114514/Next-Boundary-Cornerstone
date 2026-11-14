package cn.rbq108.nextboundarycornerstone.ServeMiao.communication;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.minecraft.client.Minecraft;
import cn.rbq108.nextboundarycornerstone.body.collision.SegmentedPhysics;
import cn.rbq108.nextboundarycornerstone.body.trunk;
import net.neoforged.neoforge.network.PacketDistributor;

// 只在客户端运行 (Dist.CLIENT)，负责发包
@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class PlayerSent {

    private static boolean lastLowGravity = false;

    /*
     *  姿态变了就全体广播
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.isPaused()) { if (player == null) lastLowGravity = false; return; }

        // 只有当前客户端控制的“本地玩家”才需要发包
        // 非常重要非常重要！不写等着服务器崩溃吧！
        if (player.level().isClientSide() && player.isLocalPlayer()) {
            boolean currentLowGravity = player.isNoGravity() && !NetworkHandler.isSpectatorAndRestored(player);

            // 如果处于失重状态，或者失重状态刚刚结束（发送最后一包重置远端重力与动作状态）
            if (currentLowGravity || lastLowGravity) {
                var pose = currentLowGravity ? player.getData(main.PLAYER_ROTATION).poseOrDefault() : null;
                if (pose == null) {
                    player.getData(main.PLAYER_ROTATION).setHitboxPose(null);
                    player.getData(main.PLAYER_ROTATION).setBodyBlocked(false);
                }
                //将四元数、UUID 和开关状态打包扔给服务器
                // 这里的 SyncRotationPayload 就是包
                PacketDistributor.sendToServer(new SyncRotationPayload(
                        player.getUUID(),
                        GlobalVariables.currentQuat,
                        currentLowGravity,
                        pose
                ));
            }
            lastLowGravity = currentLowGravity;
        }
    }

    public static void updatePhysicalPose() {
        var player = Minecraft.getInstance().player;
        if (player == null || !player.isNoGravity() || NetworkHandler.isSpectatorAndRestored(player)) return;
        var desired = cn.rbq108.nextboundarycornerstone.body.ClientSegmentedHitboxes.sample(player);
        var result = SegmentedPhysics.acceptPose(player, desired);
        if (result.bodyBlocked()) trunk.constrainTo(result.pose().body());
    }

    public static void acceptServerConstraint(cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxPose pose) {
        var player = Minecraft.getInstance().player;
        if (player == null || !player.isNoGravity() || pose == null) return;
        player.getData(main.PLAYER_ROTATION).setHitboxPose(pose);
        player.getData(main.PLAYER_ROTATION).setBodyBlocked(true);
        trunk.constrainTo(pose.body());
    }
}
