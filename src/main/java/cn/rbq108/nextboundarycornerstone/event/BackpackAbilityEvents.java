package cn.rbq108.nextboundarycornerstone.event;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;

/*
 * 无重力操作开关的主部分（由于来不及写失重判定，所以现在直接由穿戴“简易操作背包”进入失重
 * 负责检测穿脱状态，并控制 B_LowGravity 的生死大权呜~
 */
@EventBusSubscriber(modid = main.MODID) // 这里不加Dist.CLIENT，因为重力状态通常需要同步
public class BackpackAbilityEvents {

    @SubscribeEvent
    public static void onArmorChange(LivingEquipmentChangeEvent event) {
        // 只管自己的B_LowGravity
        if (!(event.getEntity() instanceof Player player)) return;

        // 只检查胸甲槽位
        if (event.getSlot() == EquipmentSlot.CHEST) {

            // 检查穿的是不是咱自己的背包
            boolean isWearingBackpack = event.getTo().is(main.BASIC_BACKPACK.get());

            if (isWearingBackpack) {
                if (GlobalVariables.B_CanBackpackGrantGravity) {
                    GlobalVariables.B_LowGravity = true; // 开启 6DOF 无重力模式喵！
                }
            } else {
                //脱下来就B_LowGravity设为0，变回原版操作
                GlobalVariables.B_LowGravity = false;
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;

        // 1. 客户端每帧自己检查一遍背上的衣服
        boolean isWearingBackpack = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST)
                .is(cn.rbq108.nextboundarycornerstone.main.BASIC_BACKPACK.get());

        // ⚡ 核心修复：只有当控制权在主模组手里时，才去根据有没有穿背包来刷新全局重力状态
        if (cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_CanBackpackGrantGravity) {
            cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_LowGravity = isWearingBackpack;
        }

        // 2. 负责在穿背包瞬间的速度继承逻辑
        if (GlobalVariables.B_LowGravity && !GlobalVariables.prevLowGravity) {
            // 这里留给你的速度继承逻辑...
        }
        GlobalVariables.prevLowGravity = GlobalVariables.B_LowGravity;
    }

//    @SubscribeEvent
//    public static void onClientTick(ClientTickEvent.Post event) {
//        var mc = Minecraft.getInstance();
//        var player = mc.player;
//        if (player == null) return;
//
//        // 客户端每帧自己检查一遍背上的衣服，把B_LowGravity同步到本地内存
//        boolean isWearingBackpack = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(cn.rbq108.nextboundarycornerstone.main.BASIC_BACKPACK.get());
//
//        // 这是外部禁用开关，写附属模组的时候开启就行，然后失重判定就由附属模组接管
//        cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_LowGravity = isWearingBackpack && cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_CanBackpackGrantGravity;
//
//        //负责在穿背包的时候（刚进入失重操作），为避免直接套用上次失重操作退出瞬间速度，先将原版游戏的三轴速度丢给模组自己的三轴速度，从而实现速度继承（不然要是上次退出前速度飞快，下次穿戴就会直接飞出去，飞起来！）
//        /*把原版游戏的三轴速度丢给
//        三轴速度（地面参考系）
//        B_Vx1
//        B_Vy1
//        B_Vz1
//        这三个变量
//
//         */
//        if (GlobalVariables.B_LowGravity && !GlobalVariables.prevLowGravity) {
//            //不过，我似乎把这个漏了？
//
//        }
//        GlobalVariables.prevLowGravity = GlobalVariables.B_LowGravity;
//
//
//    }
}