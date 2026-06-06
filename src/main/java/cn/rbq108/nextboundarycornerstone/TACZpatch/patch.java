//package cn.rbq108.nextboundarycornerstone.TACZpatch;
//
//import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
//import net.minecraft.world.entity.player.Player;
//
//public class patch {
//
//    private static float lastSyncedXRot = 0f;
//    private static float lastSyncedYRot = 0f;
//    private static boolean wasInZeroG = false;
//
//    // 缓存 TACZ 施加的、但还没被鼠标消费掉的后座力等效输入（角度度数）
//    public static float virtualRecoilDx = 0f;
//    public static float virtualRecoilDy = 0f;
//
//    /**
//     * 捕捉 TACZ 的后座力平滑曲线修改
//     */
//    public static void handleTaczRecoil(Player player) {
//        if (player == null || !player.level().isClientSide()) return;
//
//        if (GlobalVariables.B_LowGravity && !wasInZeroG) {
//            lastSyncedXRot = player.getXRot();
//            lastSyncedYRot = player.getYRot();
//            wasInZeroG = true;
//            return;
//        }
//
//        if (!GlobalVariables.B_LowGravity) {
//            wasInZeroG = false;
//            virtualRecoilDx = 0f;
//            virtualRecoilDy = 0f;
//            return;
//        }
//
//        float currentXRot = player.getXRot();
//        float currentYRot = player.getYRot();
//
//        float deltaXRot = currentXRot - lastSyncedXRot;
//        float deltaYRot = currentYRot - lastSyncedYRot;
//
//        // 如果捕捉到了 TACZ 带来的平滑视角变动
//        if (Math.abs(deltaXRot) > 0.001f || Math.abs(deltaYRot) > 0.001f) {
//            // 直接记录下偏离的角度值增量
//            virtualRecoilDx += deltaXRot;
//            virtualRecoilDy += deltaYRot;
//        }
//
//        lastSyncedXRot = currentXRot;
//        lastSyncedYRot = currentYRot;
//    }
//}