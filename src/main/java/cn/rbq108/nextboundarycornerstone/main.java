// Reference: https://github.com/enjarai/do-a-barrel-roll

package cn.rbq108.nextboundarycornerstone;

import cn.rbq108.nextboundarycornerstone.item.equipment.BasicBackpack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.ModLoadingContext;
// 注意：网络部分的包在 1.20.1 是完全不同的 (SimpleChannel)，后面需要重写网络注册





// 自己搓的通讯组件
//import cn.rbq108.nextboundarycornerstone.ServeMiao.communication.SyncRotationPayload;
//import cn.rbq108.nextboundarycornerstone.ServeMiao.communication.NetworkHandler;
import org.slf4j.Logger;
// 这里的 MODID 在类里已经定义好了


// The value here should match an entry in the META-INF/mods.toml file
@Mod(main.MODID)
public class main
{
    //不知道为什么后面加的代码要写在前面，但是塞后面会出问题（
    //public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    //哦草原来这坨东西后面写过一次了


    /*public static final DeferredHolder<Item, BasicBackpack> BASIC_BACKPACK =
            ITEMS.register("basic_backpack", () -> new BasicBackpack());
            为什么不能塞这里…？*/


    // Define mod id in a common place for everything to reference
    public static final String MODID = "next_boundary_cornerstone";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creates a new Block
    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
    // Creates a new BlockItem
    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block", () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties()));

    // Creates a new food item
    public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item", () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
            .alwaysEat().nutrition(1).saturationMod(2f).build()))); // 将 alwaysEdible() 改为 alwaysEat()

    public static final RegistryObject<Item> BASIC_BACKPACK = ITEMS.register("basic_backpack", () -> new BasicBackpack());

    // Creates a creative tab with the id "test:example_tab" for the example item, that is placed after the combat tab
    public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.next_boundary_cornerstone")) // 栏位标题
            .withTabsBefore(CreativeModeTabs.COMBAT)
            //把图标换成你的背包（反正只有这一个东西）
            .icon(() -> BASIC_BACKPACK.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                // 把背包塞进这里，仅创造才能拿
                output.accept(BASIC_BACKPACK.get());

                //这是自带的一个能吃的怪东西，不知道意义何在）
                // output.accept(EXAMPLE_ITEM.get());
            }).build());

    /*public static final DeferredHolder<Item, BasicBackpack> BASIC_BACKPACK =
            ITEMS.register("basic_backpack", () -> new BasicBackpack());
            这坨东西本来该放在这里的，但是会红
            */



    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public main()//public main(IEventBus modEventBus, ModContainer modContainer)
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (main) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        //这是默认的配置文件，用不着了
        //modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // 注册配置文件
        // 游戏启动时，就会在 config 文件夹里自动生成一个 next-boundary.toml
        // 这是旧代码ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC, "next-boundary.toml");
        // 用最新的modContainer注册
        //ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC, "next-boundary.toml");//modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC, "next-boundary.toml");
        // 强制指定使用 VariableLibrary 下的 Config.SPEC
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.COMMON,
                cn.rbq108.nextboundarycornerstone.VariableLibrary.Config.SPEC,
                "next-boundary.toml"
        );

        //modEventBus.addListener(this::registerNetwork);//手动把registerNetwork 挂载到模组总线上
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        event.enqueueWork(() -> {
            cn.rbq108.nextboundarycornerstone.ServeMiao.communication.NetworkHandler.register();
        });

        /*下面这一坨好像是没用的代码
        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));*/
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(EXAMPLE_BLOCK_ITEM);
    }

    // 在 main.java 的 FMLCommonSetupEvent 或相关的 ServerStartingEvent 中加入
    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from Next Boundary Cornerstone meow~");

        // 暴力开启服务器飞行权限
        /*
        event.getServer().setAllowFlight(true);
        LOGGER.info("开开开开开开！2341654654");*/
        //显然这个方法行不通（
        //为防其他飞行模组通过mixin注入强行启用飞行权限，故本模组不做强行启用飞行权限的注入，需要腐竹手动开启

    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    //@EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    @Mod.EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }

    // 在 main.java 的 FMLCommonSetupEvent 或相关的 ServerStartingEvent 中加入





/*    //@SubscribeEvent
    public void registerNetwork(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(main.MODID);

        // 注册四元数同步包
        registrar.playBidirectional(
                SyncRotationPayload.TYPE,
                SyncRotationPayload.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        NetworkHandler::handleDataOnClient, // 客户端收到了怎么处理
                        NetworkHandler::handleDataOnServer  // 服务端收到了怎么处理
                )
        );
    }待重写*/

    /*public static final String MODID = "test";


    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);


    public static final DeferredHolder<Item, BasicBackpack> BASIC_BACKPACK =
            ITEMS.register("basic_backpack", () -> new BasicBackpack());


    public main(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);


        ITEMS.register(modEventBus);


    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        //？
    }*/





}
