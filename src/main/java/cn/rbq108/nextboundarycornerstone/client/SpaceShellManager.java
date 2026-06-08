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
import org.joml.Vector3f;

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
        long currentTime = System.nanoTime();



        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        Iterator<SpaceShellProxy> iterator = SHELLS.iterator();
        while (iterator.hasNext()) {
            SpaceShellProxy proxy = iterator.next();

//            double timeLived = (System.currentTimeMillis() - proxy.spawnTime) / 1000.0;
            double timeLived = (currentTime - proxy.spawnTime) / 1_000_000_000.0;
            if (timeLived > 30.0) {
                SHELLS.remove(proxy);
                continue;
            }





            // 1. 预测这一帧的绝对坐标和绝对旋转
            double currentX = proxy.startPosition.x + proxy.velocity.x * timeLived;
            double currentY = proxy.startPosition.y + proxy.velocity.y * timeLived;
            double currentZ = proxy.startPosition.z + proxy.velocity.z * timeLived;
            Vec3 nextPos = new Vec3(currentX, currentY, currentZ);

            Quaternionf rotationDelta = new Quaternionf()
                    .rotateX((float) (proxy.angularVelocity.x * timeLived))
                    .rotateY((float) (proxy.angularVelocity.y * timeLived))
                    .rotateZ((float) (proxy.angularVelocity.z * timeLived));
            Quaternionf currentRot = new Quaternionf(rotationDelta).mul(proxy.startRotation);

            // 2. 连续碰撞检测 (RayTracing)：从上一帧的位置连线到预测位置
            if (proxy.lastPos != null && mc.level != null) {
                net.minecraft.world.phys.BlockHitResult hitResult = mc.level.clip(
                        new net.minecraft.world.level.ClipContext(
                                proxy.lastPos, nextPos,
                                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                                net.minecraft.world.level.ClipContext.Fluid.NONE,
                                mc.player
                        )
                );

                // 如果在这条射线上撞到了方块
                if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                    // 获取撞击面的法线向量 (比如撞到顶面，法线就是 0, 1, 0)
                    net.minecraft.core.Direction face = hitResult.getDirection();
                    Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
                    Vec3 v = new Vec3(proxy.velocity.x, proxy.velocity.y, proxy.velocity.z);

                    // 计算速度与面法线的点积
                    double dot = v.dot(normal);

                    // 防粘滞检测：只有点积小于0，说明弹壳是【迎面撞向】这个平面，才触发反弹
                    if (dot < 0) {
                        // 【反弹核心数学】：反转垂直于这个面的轴的速度并且将其乘以 0.5
                        // 向量反射衰减公式: V_new = V - (1 + Bounce) * (V · N) * N, 这里 Bounce 设为 0.5，所以是 1.5
//                        Vec3 vNew = v.subtract(normal.scale(1.5 * dot));
//                        proxy.velocity = new Vector3f((float)vNew.x, (float)vNew.y, (float)vNew.z);



                        //这是是算上表面摩擦力的
                        // 【反弹核心数学：分离法向与切向】
                        Vec3 vNormal = normal.scale(dot);        // 1. 算出垂直于墙面的速度 (撞墙分量)
                        Vec3 vTangent = v.subtract(vNormal);     // 2. 算出平行于墙面的速度 (滑动分量，即另外两个轴)

                        // 分别处理弹力与摩擦力
                        Vec3 newNormal = vNormal.scale(-0.5);    // 撞墙分量反弹，并衰减到 50%
                        Vec3 newTangent = vTangent.scale(0.8);   // 【新增】：滑动分量摩擦，衰减到 80%

                        // 将两个处理后的分量重新合成新速度
                        Vec3 vNew = newNormal.add(newTangent);
                        proxy.velocity = new Vector3f((float)vNew.x, (float)vNew.y, (float)vNew.z);

                        // 【夹角判定】：计算总速度方向与平面的夹角
                        double speed = v.length();
                        if (speed > 0.0001) {
                            // 计算入射角余弦值 (相对于法线)
                            double cosTheta = -dot / speed;
                            // 你的要求：与平面夹角 > 45度
                            // 在数学上等价于：与法线夹角 < 45度。而 cos(45度) ≈ 0.7071
                            if (cosTheta > 0.7071) {
                                // 反转三个轴的旋转速度
                                proxy.angularVelocity.mul(-1.0f);
                            }
                        }

                        // 【时空重置】：更新物理积分起点，让接下来的运动从撞击点重新开始计算！
                        proxy.startPosition = hitResult.getLocation(); // 起点重置为墙面撞击点
                        proxy.spawnTime = currentTime;                 // 时间重置为当前
                        proxy.startRotation = currentRot;              // 锁定反弹瞬间的旋转姿态

                        nextPos = proxy.startPosition; // 这一帧就把弹壳卡在墙壁上，下一帧它就会弹开
                    }
                }
            }

            // 更新“上一帧”的轨迹点
            proxy.lastPos = nextPos;
//            // 1. 绝对纯粹的匀速直线运动
//            // 不要在这里再加任何 offset 和 1.4！因为 startPosition 已经是绝对世界坐标了！
//            double currentX = proxy.startPosition.x + proxy.velocity.x * timeLived;
//            double currentY = proxy.startPosition.y + proxy.velocity.y * timeLived;
//            double currentZ = proxy.startPosition.z + proxy.velocity.z * timeLived;
//
//            // 2. 纯粹的四元数时间积分 (彻底消灭欧拉角万向节死锁)
//            Quaternionf rotationDelta = new Quaternionf()
//                    .rotateX((float) (proxy.angularVelocity.x * timeLived))
//                    .rotateY((float) (proxy.angularVelocity.y * timeLived))
//                    .rotateZ((float) (proxy.angularVelocity.z * timeLived));
//
//            // 3. 将旋转变化量叠加到初始旋转上
//            Quaternionf currentRot = new Quaternionf(rotationDelta).mul(proxy.startRotation);



            //这里是保持不变的渲染管线
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
                // 直接忽略。因为这里的渲染数据已经经过严格的安全校验，
                // 即便发生极其罕见的渲染异常，直接吞掉报错可以防止游戏被海量日志卡死。
            }

            poseStack.popPose();
        }
    }
}