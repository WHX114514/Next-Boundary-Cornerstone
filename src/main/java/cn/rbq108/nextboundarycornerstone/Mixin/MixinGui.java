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
        if (GlobalVariables.B_LowGravity) {
            var mc = Minecraft.getInstance();
            double guiWidth = mc.getWindow().getGuiScaledWidth();
            double guiHeight = mc.getWindow().getGuiScaledHeight();

            int centerX = (int) (guiWidth / 2);
            int centerY = (int) (guiHeight / 2);

            // 刚进入游戏或复活时的失重初始化提示 (3秒完全显示，第4秒起渐变淡出，5秒彻底消失) - 适用于所有失重模式
            if (mc.player != null) {
                int tickCount = mc.player.tickCount;
                if (tickCount < 100) {
                    float alpha = 1.0f;
                    if (tickCount >= 60) {
                        alpha = 1.0f - (float) (tickCount - 60) / 40.0f;
                    }
                    int alphaInt = Math.max(0, Math.min(255, (int) (alpha * 255)));
                    if (alphaInt > 0) {
                        int color = (alphaInt << 24) | 0x00999999; // 灰色文字，带淡出 Alpha
                        guiGraphics.drawCenteredString(mc.font, "失重模式四元数初始化中，视角旋转可能有稍微卡顿", centerX, centerY + 24, color);
                    }
                }
            }

            // 自由视角专属逻辑：渲染视角数字指示器和偏移准星
            if (GlobalVariables.B_FreeCameraActive && mc.options.getCameraType().isFirstPerson()) {
                double fovDegrees = GlobalVariables.lastComputedFov;

                // 获取渲染帧部分 tick (0.0 到 1.0 之间的当前 tick 内插值位置)
                float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);

                // 1. 获取机身和相机的绝对旋转四元数
                Quaternionf bodyQuat = GlobalVariables.getBodyQuat();
                Quaternionf cameraQuat = new Quaternionf(GlobalVariables.prevQuat)
                        .slerp(GlobalVariables.currentQuat, partialTick);
                // 预计算相机四元数的逆 (共轭)，创建独立副本
                // 重要：JOML 的 conjugate() 是原地修改操作，不能直接在 cameraQuat 上调用！
                Quaternionf cameraQuatInv = new Quaternionf(cameraQuat).conjugate();

                // 2. 计算身体前向向量在世界空间的方向 (当前模组正 Z 是前向)
                // 重要：JOML 的 transform() 也是原地修改操作，必须用副本避免污染 bodyQuat
                Vector3f bodyFwdWorld = new Quaternionf(bodyQuat).transform(new Vector3f(0.0f, 0.0f, 1.0f));

                // 3. 将世界空间前向向量转换到相机空间 (使用副本避免污染 cameraQuatInv)
                Vector3f bodyFwdCam = new Quaternionf(cameraQuatInv).transform(new Vector3f(bodyFwdWorld));

                double fovRad = Math.toRadians(fovDegrees);
                double scale = (guiHeight / 2.0) / Math.tan(fovRad / 2.0);
                double scaleFactor = Config.PHYSICS.crosshairScaleFactor.get().doubleValue() * GlobalVariables.B_CrosshairScaleFactor;

                // 4. 计算准星在 GUI 视口下的偏移量 (如果身体在镜头前半球，否则将准星平移到视野外隐藏)
                double offsetX = 10000.0;
                double offsetY = 10000.0;
                if (bodyFwdCam.z > 0.01f) {
                    double depth = bodyFwdCam.z;
                    offsetX = -(bodyFwdCam.x / depth) * scale * scaleFactor;
                    offsetY = -(bodyFwdCam.y / depth) * scale * scaleFactor;
                }

                // 6. 计算头部指示 UI (圆圈/角度/警告) 的偏移量
                // 普通自由视角 (未锁定)：头部 = 相机前向，UI 固定在屏幕中心，偏移量为 0
                // 固定头部自由视角 (B_HeadRotationLocked)：头部锁死在 lockedHeadRelQuat，
                //   相机自由旋转用的是四元数，必须用纯四元数乘法做独立 3D 投影
                double headOffsetX = 0.0;
                double headOffsetY = 0.0;
                if (GlobalVariables.B_HeadRotationLocked) {
                    // 头部绝对朝向 = bodyQuat * lockedHeadRelQuat (纯四元数乘法)
                    Quaternionf headAbsQuat = new Quaternionf(bodyQuat).mul(GlobalVariables.lockedHeadRelQuat);
                    Vector3f headFwdWorld = headAbsQuat.transform(new Vector3f(0.0f, 0.0f, 1.0f));
                    Vector3f headFwdCam = new Quaternionf(cameraQuatInv).transform(new Vector3f(headFwdWorld));
                    if (headFwdCam.z > 0.01f) {
                        headOffsetX = -(headFwdCam.x / headFwdCam.z) * scale * scaleFactor;
                        headOffsetY = -(headFwdCam.y / headFwdCam.z) * scale * scaleFactor;
                    } else {
                        headOffsetX = 10000.0;
                        headOffsetY = 10000.0;
                    }
                }

                // 绘制头部的 UI 组件 (圆圈/角度/警告)，应用头部的偏移量
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate((float) headOffsetX, (float) headOffsetY, 0.0f);

                // 渲染指示圆圈 (左移1像素，上移1像素微调对齐)
                drawHollowCircle(guiGraphics, centerX - 1, centerY - 1, 5, 0x80FFFFFF);

                // 绘制相对偏航角 (Yaw) 与俯仰角 (Pitch) 数字指示
                int yawVal = (int) Math.round(GlobalVariables.B_freeLookYaw);
                int pitchVal = (int) Math.round(-GlobalVariables.B_freeLookPitch);
                String yawText = yawVal < 0 ? "L " + Math.abs(yawVal) + "°" : (yawVal > 0 ? "R " + yawVal + "°" : "0°");
                String pitchText = pitchVal < 0 ? "D " + Math.abs(pitchVal) + "°" : (pitchVal > 0 ? "U " + pitchVal + "°" : "0°");

                guiGraphics.drawCenteredString(mc.font, yawText, centerX, centerY - 16, 0x80FFFFFF);
                guiGraphics.drawString(mc.font, pitchText, centerX + 10, centerY - 4, 0x80FFFFFF);

                // 当扭头角度达到 110° 极限时，在圆圈下方显示警告文本
                if (Math.abs(yawVal) >= 110) {
                    guiGraphics.drawCenteredString(mc.font, "1277227", centerX, centerY + 10, 0xFFFF5555);
                }

                guiGraphics.pose().popPose();

                // 移动准星矩阵，使准星绝对对齐身体前向的 3D 空间朝向 (由 renderCrosshair 绘制，在 RETURN 注入中由 popPose 清理)
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate((float) offsetX, (float) offsetY, 0.0f);
            }
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
