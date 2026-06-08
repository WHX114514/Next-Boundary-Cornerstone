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

// 确保这里的 modid 和你项目 resources/META-INF/mods.toml 里的绝对一致！
@Mod.EventBusSubscriber(modid = "next_boundary_cornerstone", value = Dist.CLIENT)
public class SpaceShellManager {

    public static final List<SpaceShellProxy> SHELLS = new CopyOnWriteArrayList<>();
    private static int tickCounter = 0; // 用来控制日志频率，防止刷屏卡死

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        // 换一个更不容易被跳过的渲染阶段
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (SHELLS.isEmpty() || !GlobalVariables.B_LowGravity) return;

        tickCounter++;
        // 每 60 帧（大约 1 秒）打印一次，证明渲染器真的在干活
        if (tickCounter % 60 == 0) {
            System.out.println("[NextBoundary Render] 渲染引擎正在工作！当前绘制弹壳数: " + SHELLS.size());
        }

        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        long currentTime = System.currentTimeMillis();

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        Iterator<SpaceShellProxy> iterator = SHELLS.iterator();
        while (iterator.hasNext()) {
            SpaceShellProxy proxy = iterator.next();

            double timeLived = (currentTime - proxy.spawnTime) / 1000.0;
            if (timeLived > 30.0) {
                SHELLS.remove(proxy);
                continue;
            }

            double currentX = proxy.startPosition.x + proxy.velocity.x * timeLived;
            double currentY = proxy.startPosition.y + proxy.velocity.y * timeLived;
            double currentZ = proxy.startPosition.z + proxy.velocity.z * timeLived;

            Quaternionf currentRot = new Quaternionf(proxy.startRotation);
            currentRot.rotateLocalX((float) (proxy.angularVelocity.x * timeLived));
            currentRot.rotateLocalY((float) (proxy.angularVelocity.y * timeLived));
            currentRot.rotateLocalZ((float) (proxy.angularVelocity.z * timeLived));

            poseStack.pushPose();
            poseStack.translate(currentX - camPos.x, currentY - camPos.y, currentZ - camPos.z);
            poseStack.mulPose(currentRot);

            // 恢复原版缩放比例
            poseStack.scale(1.0f, 1.0f, 1.0f);
            poseStack.translate(0, -1.5, 0);

            try {
                int fullBright = 15728880;
                RenderType renderType = RenderType.entityCutout(proxy.texture);
                proxy.model.render(poseStack, ItemDisplayContext.NONE, renderType, fullBright, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
                bufferSource.endBatch(renderType);
            } catch (Exception e) {
                // 如果是模型渲染抛出了异常，立刻抓捕归案！
                if (tickCounter % 60 == 0) {
                    System.out.println("[NextBoundary Render 致命错误] 弹壳渲染失败！");
                    e.printStackTrace();
                }
            }

            poseStack.popPose();
        }
    }
}