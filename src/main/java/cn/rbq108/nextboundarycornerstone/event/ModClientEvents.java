package cn.rbq108.nextboundarycornerstone.event;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.client.model.BASIC_BACKPACK_Converted;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ModClientEvents {


    public static boolean B_CanBackpackGrantGravity = true; // 默认允许背包提供无重力

        @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BASIC_BACKPACK_Converted.LAYER_LOCATION, BASIC_BACKPACK_Converted::createBodyLayer);
    }

    //暂时用这个不依赖那个报错类
    @SubscribeEvent
    public static void onClientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        if (net.minecraftforge.fml.ModList.get().isLoaded("tacz")) {
            event.enqueueWork(cn.rbq108.nextboundarycornerstone.TACZ.recoil::register);
        }
    }
}
