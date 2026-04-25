package cn.rbq108.nextboundarycornerstone.core;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.common.EventBusSubscriber;
//import net.minecraftforge.common.client.event.ClientTickEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class KeyPollingHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        // 仅开启无重力操作才运行
        if (mc.player == null || !GlobalVariables.B_LowGravity) return;

        long window = mc.getWindow().getWindow();

        int inX = 0, inY = 0, inZ = 0;


        int keyForward = Keybinds.B_FORWARD.getKey().getValue();
        int keyBack = Keybinds.B_BACK.getKey().getValue();
        int keyLeft = Keybinds.B_LEFT.getKey().getValue();
        int keyRight = Keybinds.B_RIGHT.getKey().getValue();
        int keyUp = Keybinds.B_UP.getKey().getValue();
        int keyDown = Keybinds.B_DOWN.getKey().getValue();

        // 这是？
        if (InputConstants.isKeyDown(window, keyForward)) inZ += 1;
        if (InputConstants.isKeyDown(window, keyBack)) inZ -= 1;
        if (InputConstants.isKeyDown(window, keyLeft)) inX -= 1;
        if (InputConstants.isKeyDown(window, keyRight)) inX += 1;

        // 无论是空格、Shift还是Ctrl，只要绑了就认
        if (InputConstants.isKeyDown(window, keyUp)) inY += 1;
        if (InputConstants.isKeyDown(window, keyDown)) inY -= 1;

        // 乖乖存进你的记事本里
        GlobalVariables.B_INx = inX;
        GlobalVariables.B_INy = inY;
        GlobalVariables.B_INz = inZ;

        // 把负责计算的类扒拉过来
        cn.rbq108.nextboundarycornerstone.motion.calculate.calculateTargetVelocity();
    }
}