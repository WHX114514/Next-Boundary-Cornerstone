package cn.rbq108.nextboundarycornerstone.event;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.client.model.BASIC_BACKPACK_Converted;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class ModClientEvents {


    public static boolean B_CanBackpackGrantGravity = true; // 默认允许背包提供无重力

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(
                    LivingEntity entity,
                    ItemStack stack,
                    EquipmentSlot slot,
                    HumanoidModel<?> defaultModel
            ) {
                return new BASIC_BACKPACK_Converted<>(
                        Minecraft.getInstance().getEntityModels().bakeLayer(BASIC_BACKPACK_Converted.LAYER_LOCATION)
                );
            }
        }, main.BASIC_BACKPACK.get());
    }


    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BASIC_BACKPACK_Converted.LAYER_LOCATION, BASIC_BACKPACK_Converted::createBodyLayer);
    }

    //暂时用这个不依赖那个报错类
    @SubscribeEvent
    public static void onClientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        // 那个Event怎么导不进去哇！导啊！导！
    }
}
