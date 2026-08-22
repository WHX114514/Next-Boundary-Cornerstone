package cn.rbq108.nextboundarycornerstone.equipment.WearHide;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.WeakHashMap;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class Helmet {

    // 涓存椂瀛樻斁琚憳涓嬫潵鐨勫ご鐩旓紙鏀寔澶氫汉鑱旀満锛屼竴浜轰竴涓牸瀛愶級
    private static final WeakHashMap<Player, ItemStack> hiddenHelmets = new WeakHashMap<>();

    // 娓叉煋鍓嶆病鏀跺ご鐩?
    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();

        if (GlobalVariables.B_LowGravity) {
            //缁曡繃搴曞眰闄愬埗锛岀洿鎺ュ幓鐜╁鐨勮儗鍖呭唴瀛樺垪琛ㄩ噷鎷夸笢瑗?绱㈠紩3瀵瑰簲澶寸洈鏍?
            ItemStack headItem = player.getInventory().armor.get(3);

            if (!headItem.isEmpty()) {
                String itemName = headItem.getItem().toString();

                // 鍒ゅ畾澶寸洈鏄惁灞炰簬妯＄粍鑷繁鐨勪笢瑗匡紝鍚﹀垯涓嶆覆鏌擄紙姣曠珶涓轰簡閫傞厤鐜板湪杩欎釜澶撮儴杩愬姩锛岃涓撻棬鍐欏搴旂殑鏃嬭浆锛?
                if (!itemName.contains("test:pilot_helmet") && !itemName.contains("extravehicular_spacesuit_helmet")) {

                    //鎵句釜鐘勮鏃棷濉炵潃
                    hiddenHelmets.put(player, headItem);

                    // 娓呯┖鑳屽寘閲岀殑澶寸洈
                    // 缁濅笉鑳戒娇鐢╯etItemSlot锛堜笉淇＄殑璇濊嚜宸辫瘯璇曪級锛岄伩寮€浜嗘父鎴忓簳灞傜殑绌胯劚闊虫晥鍜屾姢鐢查噸绠楋紙鍚庢敞锛氱湡鐨勯伩鍏嶄簡鍚楋級
                    player.getInventory().armor.set(3, ItemStack.EMPTY);
                }
            }
        }
    }

    //  Post鍚庢倓鎮勫鍥炲幓
    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();

        // 妫€鏌ュ偍鐗╂煖閲屾湁娌℃湁鎵ｆ娂浠栫殑澶寸洈
        if (hiddenHelmets.containsKey(player)) {

            // 鎮勬倓鎶婂ご鐩斿鍥炵帺瀹剁殑鍖呴噷锛屽亣瑁呬粈涔堥兘娌″彂鐢熻繃楠傛垜锛?
            player.getInventory().armor.set(3, hiddenHelmets.get(player));

            // 娓呯悊鍌ㄧ墿鏌?
            hiddenHelmets.remove(player);
        }
    }
}