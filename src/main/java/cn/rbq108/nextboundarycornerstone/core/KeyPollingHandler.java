package cn.rbq108.nextboundarycornerstone.core;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import com.mojang.blaze3d.platform.InputConstants;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class KeyPollingHandler {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
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

        if (GlobalVariables.blockMovementUntilRelease) {
            inX = 0;
            inY = 0;
            inZ = 0;
        }

        // 真实物理硬核限制：失重无背包，且身体四周1格没有方块可供借力时，清空推力！
        if (GlobalVariables.B_LowGravity && GlobalVariables.B_CanBackpackGrantGravity) {
            net.minecraft.world.item.ItemStack chest = mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
            if (chest.isEmpty() || !chest.is(cn.rbq108.nextboundarycornerstone.main.BASIC_BACKPACK.get())) {
                net.minecraft.world.phys.AABB reachBox = mc.player.getBoundingBox().inflate(1.0D);
                boolean canPushOff = false;
                for (net.minecraft.core.BlockPos bp : net.minecraft.core.BlockPos.betweenClosed(
                        net.minecraft.util.Mth.floor(reachBox.minX), net.minecraft.util.Mth.floor(reachBox.minY), net.minecraft.util.Mth.floor(reachBox.minZ),
                        net.minecraft.util.Mth.floor(reachBox.maxX), net.minecraft.util.Mth.floor(reachBox.maxY), net.minecraft.util.Mth.floor(reachBox.maxZ))) {
                    net.minecraft.world.level.block.state.BlockState bs = mc.player.level().getBlockState(bp);
                    if (!bs.isAir() && !bs.getCollisionShape(mc.player.level(), bp).isEmpty()) {
                        canPushOff = true;
                        break;
                    }
                }
                if (!canPushOff) {
                    inX = 0;
                    inY = 0;
                    inZ = 0;
                }
            }
        }

        // 乖乖存进你的记事本里
        GlobalVariables.B_INx = inX;
        GlobalVariables.B_INy = inY;
        GlobalVariables.B_INz = inZ;

        // 把负责计算的类扒拉过来
        cn.rbq108.nextboundarycornerstone.motion.calculate.calculateTargetVelocity();
    }
}