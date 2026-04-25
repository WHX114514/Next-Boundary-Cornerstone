package cn.rbq108.nextboundarycornerstone.body;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//import net.minecraftforge.fml.common.EventBusSubscriber;
//import net.minecraftforge.common.client.event.ClientTickEvent;

@Mod.EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class trunk {

    public static float smoothX, smoothY, smoothZ;
    public static float prevSmoothX, prevSmoothY, prevSmoothZ;

    private static float lastRawX, lastRawY, lastRawZ;
    private static float cumulativeX, cumulativeY, cumulativeZ;
    private static boolean wasLowGravity = false;

    @SubscribeEvent
    //public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
    public static void onClientTick(TickEvent.ClientTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;

        if (GlobalVariables.B_LowGravity) {
            // 初始化视觉坐标，只在开启瞬间执行一次，防止身子闪现
            if (!wasLowGravity) {
                float rx = (float) GlobalVariables.B_Dx;
                float ry = (float) GlobalVariables.B_Dy;
                float rz = (float) GlobalVariables.B_Dz;
                cumulativeX = smoothX = prevSmoothX = rx;
                cumulativeY = smoothY = prevSmoothY = ry;
                cumulativeZ = smoothZ = prevSmoothZ = rz;
                lastRawX = rx; lastRawY = ry; lastRawZ = rz;
                wasLowGravity = true;
            }

            // 纯净的平滑计算
            float lerpSpeed = 0.3f;
            prevSmoothX = smoothX; prevSmoothY = smoothY; prevSmoothZ = smoothZ;

            float rawX = (float) GlobalVariables.B_Dx;
            float rawY = (float) GlobalVariables.B_Dy;
            float rawZ = (float) GlobalVariables.B_Dz;

            cumulativeX += Mth.wrapDegrees(rawX - lastRawX);
            cumulativeY += Mth.wrapDegrees(rawY - lastRawY);
            cumulativeZ += Mth.wrapDegrees(rawZ - lastRawZ);
            lastRawX = rawX; lastRawY = rawY; lastRawZ = rawZ;

            smoothX += (cumulativeX - smoothX) * lerpSpeed;
            smoothY += (cumulativeY - smoothY) * lerpSpeed;
            smoothZ += (cumulativeZ - smoothZ) * lerpSpeed;

        } else {
            wasLowGravity = false;
        }
    }
}