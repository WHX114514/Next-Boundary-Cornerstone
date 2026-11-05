package cn.rbq108.nextboundarycornerstone.core;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;

public class KeyPollingHandler {

    public static void pollInput() {
        var mc = Minecraft.getInstance();
        int inX = 0, inY = 0, inZ = 0;
        if (mc.player != null && GlobalVariables.B_LowGravity) {
            if (isPressed(Keybinds.B_FORWARD)) inZ += 1;
            if (isPressed(Keybinds.B_BACK)) inZ -= 1;
            if (isPressed(Keybinds.B_LEFT)) inX -= 1;
            if (isPressed(Keybinds.B_RIGHT)) inX += 1;
            if (isPressed(Keybinds.B_UP)) inY += 1;
            if (isPressed(Keybinds.B_DOWN)) inY -= 1;
        }

        if (GlobalVariables.blockMovementUntilRelease) {
            inX = 0;
            inY = 0;
            inZ = 0;
        }

        // 真实物理硬核限制：失重无背包，且身体四周1格没有方块可供借力时，清空推力！
        if (mc.player != null && GlobalVariables.B_LowGravity && GlobalVariables.B_CanBackpackGrantGravity) {
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

    }
    /** Physical input survives the suppression of vanilla jump/sneak/sprint key states. */
    public static boolean isPressed(net.minecraft.client.KeyMapping mapping) {
        var mc = Minecraft.getInstance();
        if (mc.screen != null || !mc.isWindowActive()) return false;
        if (!mapping.isConflictContextAndModifierActive()) return false;
        var key = mapping.getKey();
        if (key.equals(InputConstants.UNKNOWN)) return false;
        long window = mc.getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return org.lwjgl.glfw.GLFW.glfwGetMouseButton(window, key.getValue()) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        }
        if (key.getType() == InputConstants.Type.KEYSYM) return InputConstants.isKeyDown(window, key.getValue());
        return mapping.isDown();
    }
}
