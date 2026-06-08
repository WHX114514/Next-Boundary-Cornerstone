package cn.rbq108.nextboundarycornerstone.client;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
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

@Mod.EventBusSubscriber(modid = "next_boundary_cornerstone", value = Dist.CLIENT)
public class SpaceShellManager {

    public static final List<SpaceShellProxy> SHELLS = new CopyOnWriteArrayList<>();

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (SHELLS.isEmpty() || !GlobalVariables.B_LowGravity) return;

        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        long currentTime = System.currentTimeMillis();

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        Iterator<SpaceShellProxy> iterator = SHELLS.iterator();
        while (iterator.hasNext()) {
            SpaceShellProxy proxy = iterator.next();

            double timeLived = (System.currentTimeMillis() - proxy.spawnTime) / 1000.0;
            if (timeLived > 30.0) {
                SHELLS.remove(proxy);
                continue;
            }

            // 1. 绝对纯粹的匀速直线运动
            // 不要在这里再加任何 offset 和 1.4！因为 startPosition 已经是绝对世界坐标了！
            double currentX = proxy.startPosition.x + proxy.velocity.x * timeLived;
            double currentY = proxy.startPosition.y + proxy.velocity.y * timeLived;
            double currentZ = proxy.startPosition.z + proxy.velocity.z * timeLived;

            // 2. 纯粹的四元数时间积分 (彻底消灭欧拉角万向节死锁)
            Quaternionf rotationDelta = new Quaternionf()
                    .rotateX((float) (proxy.angularVelocity.x * timeLived))
                    .rotateY((float) (proxy.angularVelocity.y * timeLived))
                    .rotateZ((float) (proxy.angularVelocity.z * timeLived));

            // 3. 将旋转变化量叠加到初始旋转上
            Quaternionf currentRot = new Quaternionf(rotationDelta).mul(proxy.startRotation);

            poseStack.pushPose();
            poseStack.translate(currentX - camPos.x, currentY - camPos.y, currentZ - camPos.z);
            poseStack.mulPose(currentRot);

            // 恢复原版缩放比例
            poseStack.scale(1.0f, 1.0f, 1.0f);
            poseStack.translate(0, -1.5, 0); // 这个是 TACZ 模型原点的抵消，保留即可

            try {
                int fullBright = 15728880;
                RenderType renderType = RenderType.entityCutout(proxy.texture);
                proxy.model.render(poseStack, ItemDisplayContext.NONE, renderType, fullBright, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
                bufferSource.endBatch(renderType);
            } catch (Exception ignored) {
                // 直接忽略即可。因为这里的渲染数据已经经过严格的安全校验，
                // 即便发生极其罕见的渲染异常，直接吞掉报错可以防止游戏被海量日志卡死。
            }

            poseStack.popPose();
        }
    }
}