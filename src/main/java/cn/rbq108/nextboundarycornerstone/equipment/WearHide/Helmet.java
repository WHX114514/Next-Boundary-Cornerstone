package cn.rbq108.nextboundarycornerstone.equipment.WearHide;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
//import net.neoforged.api.distmarker.Dist;
//import net.neoforged.bus.api.SubscribeEvent;
//import net.neoforged.fml.common.EventBusSubscriber;
//import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RenderPlayerEvent;

import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
//@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class Helmet {

    // 临时存放被摘下来的头盔（支持多人联机，一人一个格子）
    private static final WeakHashMap<Player, ItemStack> hiddenHelmets = new WeakHashMap<>();

    // 渲染前没收头盔
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();

        if (GlobalVariables.B_LowGravity) {
            //绕过底层限制，直接去玩家的背包内存列表里拿东西(索引3对应头盔栏)
            ItemStack headItem = player.getInventory().armor.get(3);

            if (!headItem.isEmpty()) {
                String itemName = headItem.getItem().toString();

                // 判定头盔是否属于模组自己的东西，否则不渲染（毕竟为了适配现在这个头部运动，要专门写对应的旋转）
                if (!itemName.contains("test:pilot_helmet")) {

                    //找个犄角旮旯塞着
                    hiddenHelmets.put(player, headItem);

                    // 清空背包里的头盔
                    // 绝不能使用setItemSlot（不信的话自己试试），避开了游戏底层的穿脱音效和护甲重算（后注：真的避免了吗）
                    player.getInventory().armor.set(3, ItemStack.EMPTY);
                }
            }
        }
    }

    //  Post后悄悄塞回去
    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();

        // 检查储物柜里有没有扣押他的头盔
        if (hiddenHelmets.containsKey(player)) {

            // 悄悄把头盔塞回玩家的包里，假装什么都没发生过骂我！
            player.getInventory().armor.set(3, hiddenHelmets.get(player));

            // 清理储物柜
            hiddenHelmets.remove(player);
        }
    }
}