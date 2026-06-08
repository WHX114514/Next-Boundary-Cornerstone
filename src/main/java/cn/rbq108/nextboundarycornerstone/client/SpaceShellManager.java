package cn.rbq108.nextboundarycornerstone.client;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource; // 新增导入
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Mod.EventBusSubscriber(modid = "nextboundarycornerstone", value = Dist.CLIENT)
public class SpaceShellManager {

    public static final List<SpaceShellProxy> SHELLS = new CopyOnWriteArrayList<>();

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        // 确保只在实体渲染阶段之后渲染
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (SHELLS.isEmpty() || !GlobalVariables.B_LowGravity) return;

        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        long currentTime = System.currentTimeMillis();

        // 获取 Minecraft 的全局渲染缓冲区
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        Iterator<SpaceShellProxy> iterator = SHELLS.iterator();
        while (iterator.hasNext()) {
            SpaceShellProxy proxy = iterator.next();

            double timeLived = (currentTime - proxy.spawnTime) / 1000.0;
            if (timeLived > 30.0) {
                SHELLS.remove(proxy);
                continue;
            }

            // 计算世界绝对坐标
            double currentX = proxy.startPosition.x + proxy.velocity.x * timeLived;
            double currentY = proxy.startPosition.y + proxy.velocity.y * timeLived;
            double currentZ = proxy.startPosition.z + proxy.velocity.z * timeLived;

            // 计算当前绝对旋转姿态
            Quaternionf currentRot = new Quaternionf(proxy.startRotation);
            currentRot.rotateLocalX((float) (proxy.angularVelocity.x * timeLived));
            currentRot.rotateLocalY((float) (proxy.angularVelocity.y * timeLived));
            currentRot.rotateLocalZ((float) (proxy.angularVelocity.z * timeLived));

            poseStack.pushPose();

            // 核心世界坐标投影
            poseStack.translate(currentX - camPos.x, currentY - camPos.y, currentZ - camPos.z);
            poseStack.mulPose(currentRot);

            // 因为脱离了枪械的相对比例，这里加一个基础缩放防止太小看不见 (TACZ枪模通常自带缩放)
            // 如果觉得太大/太小可以调整这里的值
            poseStack.scale(0.6f, 0.6f, 0.6f);
            poseStack.translate(0, -1.5, 0);

            try {
                // 强制设置为全亮，确保在任何环境下都能刺眼地看到它
                int fullBright = 15728880;
                proxy.model.render(poseStack, ItemDisplayContext.NONE, RenderType.entityCutout(proxy.texture), fullBright, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            } catch (Exception ignored) {}

            poseStack.popPose();
        }

        // 终极咒语：强制把我们刚刚画的所有弹壳，立刻推送到显卡显示！
        bufferSource.endBatch();
    }
}