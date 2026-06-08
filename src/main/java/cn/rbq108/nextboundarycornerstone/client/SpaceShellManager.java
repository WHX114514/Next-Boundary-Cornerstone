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

            double timeLived = (currentTime - proxy.spawnTime) / 1000.0;
            if (timeLived > 30.0) {
                SHELLS.remove(proxy);
                continue;
            }

            // 纯粹的匀速直线运动
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

            poseStack.scale(1.0f, 1.0f, 1.0f);
            poseStack.translate(0, -1.5, 0); // TACZ模型的原点补偿

            try {
                int fullBright = 15728880;
                RenderType renderType = RenderType.entityCutout(proxy.texture);
                proxy.model.render(poseStack, ItemDisplayContext.NONE, renderType, fullBright, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
                bufferSource.endBatch(renderType);
            } catch (Exception ignored) {} // 模型既然100%拿到了，这里就不会再报错了

            poseStack.popPose();
        }
    }
}