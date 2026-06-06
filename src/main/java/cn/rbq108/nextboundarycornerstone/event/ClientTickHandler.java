package cn.rbq108.nextboundarycornerstone.event;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;

import static cn.rbq108.nextboundarycornerstone.ServeMiao.communication.NetworkHandler.sendToServer;

@Mod.EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class ClientTickHandler {
    private static final Quaternionf lastSentQuat = new Quaternionf();

    @SubscribeEvent
    public static void onClientPlayerTick(TickEvent.PlayerTickEvent event) {
        // 客户端主周期 & 本地玩家
        if (event.phase == TickEvent.Phase.END && event.player.level().isClientSide() && event.player == Minecraft.getInstance().player) {
            if (GlobalVariables.B_LowGravity) {
                Quaternionf currentQuat = GlobalVariables.currentQuat;

                // 四元数变化 发包通知
                if (!currentQuat.equals(lastSentQuat)) {
                    sendToServer(currentQuat);
                    lastSentQuat.set(currentQuat);
                }
            }
        }
    }
}