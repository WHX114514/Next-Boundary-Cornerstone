package cn.rbq108.nextboundarycornerstone.client;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
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

// 注册到 Forge 的事件总线，确保能监听到渲染事件
@Mod.EventBusSubscriber(modid = "nextboundarycornerstone", value = Dist.CLIENT)
public class SpaceShellManager {

    // 线程安全的列表，用来存放所有正在飞行的弹壳
    public static final List<SpaceShellProxy> SHELLS = new CopyOnWriteArrayList<>();

    // 每一帧世界渲染完毕后，我们把弹壳画上去
    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        // 确保只在实体渲染之后画，避免被方块遮挡逻辑出错
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        if (SHELLS.isEmpty() || !GlobalVariables.B_LowGravity) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();

        long currentTime = System.currentTimeMillis();

        Iterator<SpaceShellProxy> iterator = SHELLS.iterator();
        while (iterator.hasNext()) {
            SpaceShellProxy proxy = iterator.next();

            // 1. 生命周期检查 (30秒)
            float livedSeconds = (currentTime - proxy.spawnTime) / 1000.0f;
            if (livedSeconds > proxy.livingTime) {
                SHELLS.remove(proxy);
                continue;
            }

            // 2. 物理更新 (匀速直线运动)
            // 你后续想加碰撞变速，就在这里写 level.clip 逻辑！
            double deltaSeconds = mc.getFrameTime() / 20.0; // 粗略的每帧时间
            proxy.worldPosition = proxy.worldPosition.add(
                    proxy.velocity.x * deltaSeconds,
                    proxy.velocity.y * deltaSeconds,
                    proxy.velocity.z * deltaSeconds
            );

            // 更新翻滚姿态
            proxy.rotation.rotateX((float) (proxy.angularVelocity.x * deltaSeconds));
            proxy.rotation.rotateY((float) (proxy.angularVelocity.y * deltaSeconds));
            proxy.rotation.rotateZ((float) (proxy.angularVelocity.z * deltaSeconds));

            // 3. 渲染绘制
            poseStack.pushPose();

            // 核心：把世界绝对坐标，转换为相对于相机的渲染坐标
            poseStack.translate(
                    proxy.worldPosition.x - camPos.x,
                    proxy.worldPosition.y - camPos.y,
                    proxy.worldPosition.z - camPos.z
            );

            // 应用它自己的旋转姿态
            poseStack.mulPose(proxy.rotation);

            // 微调 TACZ 模型自带的下沉偏移
            poseStack.translate(0, -1.5, 0);

            // 白嫖 TACZ 的渲染逻辑，直接把图画出来！
            try {
                int light = mc.level.getLightEngine().getRawBrightness(
                        net.minecraft.core.BlockPos.containing(proxy.worldPosition), 0
                );
                proxy.model.render(poseStack, ItemDisplayContext.NONE, RenderType.entityCutout(proxy.texture), light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            } catch (Exception ignored) { }

            poseStack.popPose();
        }
    }
}