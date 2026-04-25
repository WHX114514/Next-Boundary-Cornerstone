package cn.rbq108.nextboundarycornerstone.core;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.common.EventBusSubscriber;
//import net.minecraftforge.common.client.event.InputEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class KeyInputHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        var mc = Minecraft.getInstance();
        // 只有按下按键、没开菜单、且在无重力状态下才生效
        if (mc.player == null || mc.screen != null || !GlobalVariables.B_LowGravity) return;

        if (event.getAction() == GLFW.GLFW_PRESS) {
            boolean isShiftDown = (event.getModifiers() & GLFW.GLFW_MOD_SHIFT) != 0;

            //只有在 Shift 按下时，我们才手动触发原版功能
            if (isShiftDown) {
                // 如果按的是背包键 (E)
                if (mc.options.keyInventory.matches(event.getKey(), event.getScanCode())) {
                    mc.setScreen(new InventoryScreen(mc.player));
                }
                // 如果按的是丢弃键 (Q)
                if (mc.options.keyDrop.matches(event.getKey(), event.getScanCode())) {
                    mc.player.drop(false);
                }
            }
        }
    }
}