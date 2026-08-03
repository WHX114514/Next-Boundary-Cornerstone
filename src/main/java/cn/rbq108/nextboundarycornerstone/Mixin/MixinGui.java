package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class MixinGui {

    @Inject(method = "renderCrosshair", at = @At("HEAD"))
    private void onRenderCrosshairHead(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (GlobalVariables.B_LowGravity && GlobalVariables.B_FreeCameraActive && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            var mc = Minecraft.getInstance();
            double fovDegrees = GlobalVariables.lastComputedFov;
            double guiWidth = mc.getWindow().getGuiScaledWidth();
            double guiHeight = mc.getWindow().getGuiScaledHeight();

            int centerX = (int) (guiWidth / 2);
            int centerY = (int) (guiHeight / 2);

            // 在屏幕中心渲染一个半透明白色指示圆圈 (HUD中心指示器)
            drawHollowCircle(guiGraphics, centerX, centerY, 5, 0x80FFFFFF);

            // 获取渲染帧部分 tick (0.0 到 1.0 之间的当前 tick 内插值位置)
            float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);

            // 1. 获取机身和相机的绝对旋转四元数
            Quaternionf bodyQuat = GlobalVariables.getBodyQuat();
            Quaternionf cameraQuat = new Quaternionf(GlobalVariables.prevQuat)
                    .slerp(GlobalVariables.currentQuat, partialTick);

            // 2. 计算身体前向向量在世界空间的方向 (当前模组正 Z 是前向)
            Vector3f bodyFwdWorld = bodyQuat.transform(new Vector3f(0.0f, 0.0f, 1.0f));

            // 3. 将世界空间前向向量转换到相机空间
            Vector3f bodyFwdCam = cameraQuat.conjugate().transform(bodyFwdWorld);

            // 4. 判断是否在镜头前半球 (z > 0.01f 表示在镜头前方，防止除零或反向渲染)
            if (bodyFwdCam.z <= 0.01f) {
                // 如果在镜头后方，则将准星平移到视野外隐藏
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(10000.0f, 10000.0f, 0.0f);
                return;
            }

            // 5. 使用 3D 透视投影公式精确计算准星在 GUI 视口下的偏移量 (并乘以配置文件的微调系数)
            double fovRad = Math.toRadians(fovDegrees);
            double scale = (guiHeight / 2.0) / Math.tan(fovRad / 2.0);
            double scaleFactor = Config.PHYSICS.crosshairScaleFactor.get().doubleValue() * GlobalVariables.B_CrosshairScaleFactor;

            double depth = bodyFwdCam.z;
            double offsetX = -(bodyFwdCam.x / depth) * scale * scaleFactor;
            double offsetY = -(bodyFwdCam.y / depth) * scale * scaleFactor;

            // 6. 移动准星矩阵，使准星绝对对齐身体前向的 3D 空间朝向
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate((float) offsetX, (float) offsetY, 0.0f);
        }
    }

    @Inject(method = "renderCrosshair", at = @At("RETURN"))
    private void onRenderCrosshairReturn(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (GlobalVariables.B_LowGravity && GlobalVariables.B_FreeCameraActive && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            guiGraphics.pose().popPose();
        }
    }

    /**
     * 中点画圆算法，用于在 GUI 界面绘制指定颜色和半径的空心圆
     */
    private void drawHollowCircle(GuiGraphics guiGraphics, int centerX, int centerY, int radius, int color) {
        int x = radius;
        int y = 0;
        int err = 0;

        while (x >= y) {
            drawPixel(guiGraphics, centerX + x, centerY + y, color);
            drawPixel(guiGraphics, centerX + y, centerY + x, color);
            drawPixel(guiGraphics, centerX - y, centerY + x, color);
            drawPixel(guiGraphics, centerX - x, centerY + y, color);
            drawPixel(guiGraphics, centerX - x, centerY - y, color);
            drawPixel(guiGraphics, centerX - y, centerY - x, color);
            drawPixel(guiGraphics, centerX + y, centerY - x, color);
            drawPixel(guiGraphics, centerX + x, centerY - y, color);

            if (err <= 0) {
                y += 1;
                err += 2 * y + 1;
            }
            if (err > 0) {
                x -= 1;
                err -= 2 * x + 1;
            }
        }
    }

    private void drawPixel(GuiGraphics guiGraphics, int x, int y, int color) {
        guiGraphics.fill(x, y, x + 1, y + 1, color);
    }
}
