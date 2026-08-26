package cn.rbq108.nextboundarycornerstone.event;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 太空无重力状态下的挖掘速度修正类
 * 负责抵消原版在空气中 (!onGround) 自动将挖掘速度除以 5 的惩罚机制
 */
@EventBusSubscriber(modid = main.MODID) // 必须双端运行，防止客户端和服务端挖掘速度不同步导致“回档”或“幽灵方块”
public class MiningSpeedEvents {

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        var player = event.getEntity();
        // 只有当开启无重力操纵时才触发修正
        if (player.isNoGravity()) {
            
            // 处于太空悬浮状态下，player.onGround() 必然为 false
            // 原版会在此前将速度 / 5.0f，因此我们乘以 5.0f 抵消其惩罚
            if (!player.onGround()) {
                event.setNewSpeed(event.getNewSpeed() * 5.0f);
            }
        }
    }
}
