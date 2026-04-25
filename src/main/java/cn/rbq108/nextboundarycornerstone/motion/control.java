/*
该类负责接收键盘和鼠标的输入信息

 */
package cn.rbq108.nextboundarycornerstone.motion;

import cn.rbq108.nextboundarycornerstone.core.Keybinds;
import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//import net.minecraftforge.fml.common.EventBusSubscriber;
//import net.minecraftforge.common.client.event.ClientTickEvent;

@Mod.EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class control {


    public static int B_INx = 0;
    public static int B_INy = 0;
    public static int B_INz = 0;
    public static int B_INroll = 0;
    public static boolean B_bag_pressed = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // X-axis: Left (+1) and Right (-1)
        B_INx = 0;
        if (Keybinds.B_LEFT.isDown()) B_INx += 1;
        if (Keybinds.B_RIGHT.isDown()) B_INx -= 1;

        // Y-axis: Up (+1) and Down (-1)
        B_INy = 0;
        if (Keybinds.B_UP.isDown()) B_INy += 1;
        if (Keybinds.B_DOWN.isDown()) B_INy -= 1;

        // Z-axis: Forward (+1) and Back (-1)
        B_INz = 0;
        if (Keybinds.B_FORWARD.isDown()) B_INz += 1;
        if (Keybinds.B_BACK.isDown()) B_INz -= 1;

        // Roll-axis: Left Roll (+1) and Right Roll (-1)
        B_INroll = 0;
        if (Keybinds.B_ROLL_LEFT.isDown()) B_INroll += 1;
        if (Keybinds.B_ROLL_RIGHT.isDown()) B_INroll -= 1;

        // Bag shortcut detection
        // Note: Using consumeClick() instead of isDown() to prevent UI spamming every tick
        B_bag_pressed = Keybinds.B_BAG.consumeClick();
    }
}