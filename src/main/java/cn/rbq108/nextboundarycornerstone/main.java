package cn.rbq108.nextboundarycornerstone;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.Config;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import cn.rbq108.nextboundarycornerstone.item.equipment.BasicBackpack;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;

@Mod(main.MODID)
public class main {
    public static final String MODID = "next_boundary_cornerstone";
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block", () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item", () -> new Item(new Item.Properties().food(new FoodProperties.Builder().alwaysEat().nutrition(1).saturationMod(2f).build())));
    public static final RegistryObject<BasicBackpack> BASIC_BACKPACK = ITEMS.register("basic_backpack", BasicBackpack::new);
    public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.next_boundary_cornerstone"))
            .withTabsBefore(CreativeModeTabs.COMBAT).icon(() -> BASIC_BACKPACK.get().getDefaultInstance())
            .displayItems((parameters, output) -> output.accept(BASIC_BACKPACK.get())).build());

    public main() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        CREATIVE_MODE_TABS.register(bus);
        cn.rbq108.nextboundarycornerstone.effect.ExplosionParticles.register(bus);
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) ->
                event.enqueueWork(cn.rbq108.nextboundarycornerstone.ServeMiao.communication.NetworkHandler::register));
        bus.addListener((net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent event) ->
                event.register(cn.rbq108.nextboundarycornerstone.capability.PlayerRotationCapability.class));
        var context = ModLoadingContext.get();
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC, "next-boundary.toml");
        context.registerConfig(ModConfig.Type.SERVER, cn.rbq108.nextboundarycornerstone.VariableLibrary.SurfaceConfig.SPEC, "next-boundary-surface-server.toml");
        context.registerConfig(ModConfig.Type.SERVER, cn.rbq108.nextboundarycornerstone.body.OOBB.HitboxConfig.SPEC,
                cn.rbq108.nextboundarycornerstone.body.OOBB.HitboxConfig.FILE_NAME);
        GlobalVariables.B_TACZ = net.minecraftforge.fml.ModList.get().isLoaded("tacz") ? 1 : 0;
    }
}
