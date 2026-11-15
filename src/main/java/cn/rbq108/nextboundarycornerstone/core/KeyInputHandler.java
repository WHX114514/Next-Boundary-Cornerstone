package cn.rbq108.nextboundarycornerstone.core;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class KeyInputHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        var mc = Minecraft.getInstance();
        // 只有按下按键、没开菜单、且在无重力状态下才生效
        if (mc.player == null || mc.screen != null || !GlobalVariables.B_LowGravity) return;

        if (event.getAction() == GLFW.GLFW_PRESS) {
            // 按住组合键（默认 Tab）时才触发；背包/丢弃键沿用玩家的原版绑定。
            if (KeyPollingHandler.isPressed(Keybinds.B_ACTION_MODIFIER)) {
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
