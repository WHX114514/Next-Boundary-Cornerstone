package cn.rbq108.nextboundarycornerstone.event;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.client.model.BASIC_BACKPACK_Converted;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//import net.minecraftforge.fml.common.EventBusSubscriber;
//import net.minecraftforge.common.client.event.EntityRenderersEvent;

@Mod.EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class ModClientEvents {


    public static boolean B_CanBackpackGrantGravity = true; // 默认允许背包提供无重力


    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BASIC_BACKPACK_Converted.LAYER_LOCATION, BASIC_BACKPACK_Converted::createBodyLayer);
    }

    //暂时用这个不依赖那个报错类
    @SubscribeEvent
    public static void onClientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        // 那个Event怎么导不进去哇！导啊！导！
    }
}