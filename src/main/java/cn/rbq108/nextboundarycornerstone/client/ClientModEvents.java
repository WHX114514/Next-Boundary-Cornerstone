package cn.rbq108.nextboundarycornerstone.client;

import cn.rbq108.nextboundarycornerstone.client.model.BASIC_BACKPACK_Converted;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 注意这里的 modid 必须和你主类声明的 MODID 完全一致
// value = Dist.CLIENT 确保了这个类绝不会在独立服务端端运行
@Mod.EventBusSubscriber(modid = "next_boundary_cornerstone", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // 在这里把你的背包模型图层注册进游戏
        event.registerLayerDefinition(
                BASIC_BACKPACK_Converted.LAYER_LOCATION,
                BASIC_BACKPACK_Converted::createBodyLayer
        );
    }
}