package cn.rbq108.nextboundarycornerstone.ServeMiao.communication;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

// 只在客户端运行 (Dist.CLIENT)，负责发包
@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class PlayerSent {

    /*
     *  姿态变了就全体广播
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        // 只有当前客户端控制的“本地玩家”才需要发包
        // 非常重要非常重要！不写等着服务器崩溃吧！
        if (player.level().isClientSide() && player.isLocalPlayer()) {

            // 🩺 2. 检查失重状态开关
            if (player.isNoGravity()) {

                //将四元数、UUID 和开关状态打包扔给服务器
                // 这里的 SyncRotationPayload 就是包
                PacketDistributor.sendToServer(new SyncRotationPayload(
                        player.getUUID(),
                        GlobalVariables.currentQuat,
                        player.isNoGravity()
                ));
            }
        }
    }
}