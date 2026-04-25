package cn.rbq108.nextboundarycornerstone.ServeMiao.communication;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class PlayerSent {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;

        if (player.level().isClientSide() && player.isLocalPlayer()) {
            if (GlobalVariables.B_LowGravity) {
                // 1.20.1 专属发包方式
                NetworkHandler.CHANNEL.sendToServer(new SyncRotationPayload(
                        player.getUUID(),
                        GlobalVariables.currentQuat,
                        GlobalVariables.B_LowGravity
                ));
            }
        }
    }
}