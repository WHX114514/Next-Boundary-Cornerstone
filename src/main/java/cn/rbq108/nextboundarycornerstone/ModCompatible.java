package cn.rbq108.nextboundarycornerstone;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class ModCompatible {

    // 记忆状态：用于在玩家拖拽时视角离开方块的情况下维持判定
    private static boolean wasGrabbingHandle = false;

    /**
     * 判断玩家是否正在抓取航空学（或其他物理模组）的铁把手
     */
    public static boolean isGrabbingAeronauticsHandle(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            wasGrabbingHandle = false;
            return false;
        }

        // 如果用户松开了右键，立刻取消抓取状态
        if (!mc.options.keyUse.isDown()) {
            wasGrabbingHandle = false;
            return false;
        }

        // 1. 如果之前判定已经在抓取了，且现在还没松开右键，则继续保持抓取状态！
        if (wasGrabbingHandle) {
            return true;
        }

        // 2. 射线检测：初始抓取瞬间的判定
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) mc.hitResult;
            BlockState state = mc.level.getBlockState(blockHit.getBlockPos());
            String blockName = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().toLowerCase();
            
            if ((blockName.contains("aeronautics") || blockName.contains("simulated") || blockName.contains("valkyrienskies")) && 
                blockName.contains("handle")) {
                wasGrabbingHandle = true;
                return true;
            }
        }

        // 3. 实体骑乘备用判定
        if (player.getVehicle() != null) {
            String vehicleName = BuiltInRegistries.ENTITY_TYPE.getKey(player.getVehicle().getType()).toString().toLowerCase();
            if ((vehicleName.contains("aeronautics") || vehicleName.contains("simulated") || vehicleName.contains("valkyrienskies")) && 
                vehicleName.contains("handle")) {
                wasGrabbingHandle = true;
                return true;
            }
        }

        return false;
    }
}
