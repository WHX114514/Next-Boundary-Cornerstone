package cn.rbq108.nextboundarycornerstone.body;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class model {

    // 🩺 动画冰冻锁：防止抽搐的核心
    private static float oldXRot, oldYBodyRot, oldYHeadRot;
    private static float oldXRotO, oldYBodyRotO, oldYHeadRotO;

    // 解决 PoseStack 崩溃的绝对安全哨兵集合，记录哪些玩家执行过 pushPose
    private static final java.util.Set<LocalPlayer> PUSHED_PLAYERS = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

    private static boolean isRenderingInInventory() {
        if (net.minecraft.client.Minecraft.getInstance().screen != null) {
            for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                if (element.getClassName().contains("InventoryScreen")) {
                    return true;
                }
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (event.getEntity() instanceof LocalPlayer player && player.getClass().getName().equals("net.minecraft.client.player.LocalPlayer") && GlobalVariables.B_LowGravity) {
            PUSHED_PLAYERS.add(player); // 记录推入标记

            if (isRenderingInInventory()) {
                PoseStack poseStack = event.getPoseStack();
                poseStack.pushPose();
                poseStack.translate(0, -0.3f, 0); // 补偿失重状态下碰撞箱缩短导致的人物偏高
                return;
            }

            float pt = event.getPartialTick();

            // 补帧插值获取平滑角度
            float renderX = Mth.lerp(pt, trunk.prevSmoothX, trunk.smoothX);
            float renderY = Mth.lerp(pt, trunk.prevSmoothY, trunk.smoothY);
            float renderZ = Mth.lerp(pt, trunk.prevSmoothZ, trunk.smoothZ);

            // 计算当前碰撞箱中心点（这破变量报错过无数次了！）
            float currentHitboxHeight = player.getBbHeight();
            float centerY = currentHitboxHeight * 0.5f;

            // 构造身体四元数 (YXZ 顺序)
            Quaternionf bodyQuat = new Quaternionf()
                    .rotateY((float) Math.toRadians(-renderY))
                    .rotateX(-(float) Math.toRadians(-renderX))
                    .rotateZ((float) Math.toRadians(renderZ));

            // 这是啥，冻结数据吗
            oldXRot = player.getXRot(); oldXRotO = player.xRotO;
            oldYBodyRot = player.yBodyRot; oldYBodyRotO = player.yBodyRotO;
            oldYHeadRot = player.yHeadRot; oldYHeadRotO = player.yHeadRotO;
            player.setXRot(0f); player.xRotO = 0f;
            player.yBodyRot = 0f; player.yBodyRotO = 0f;
            player.yHeadRot = 0f; player.yHeadRotO = 0f;

            //Mixin锁头
            GlobalVariables.isPlayerRendering = true;
            if (event.getRenderer() instanceof PlayerRenderer renderer) {
                GlobalVariables.playerHead = renderer.getModel().head;
                GlobalVariables.playerHat = renderer.getModel().hat;
            }

            // 计算头身隔离矩阵
            Quaternionf camQuat = new Quaternionf(GlobalVariables.prevQuat).slerp(GlobalVariables.currentQuat, pt);
            Quaternionf relQuat;

            // 检查头部相对旋转是否仍在平滑过渡/回正中
            boolean isHeadReturning = GlobalVariables.headRelQuat.angle() > 0.001f;

            if (GlobalVariables.B_FreeCameraActive || isHeadReturning) {
                // 自由视角或回弹归位期间，使用每 tick 进行 Slerp 平滑插值后的头部相对四元数（消除突变）
                relQuat = new Quaternionf(GlobalVariables.prevHeadRelQuat).slerp(GlobalVariables.headRelQuat, pt);
            } else {
                // 正常状态下，直接使用身体与镜头的实时旋转差，保留原版微弱的头部转向惯性延迟效果
                relQuat = new Quaternionf(bodyQuat).invert().mul(camQuat);
            }

            Quaternionf y180 = new Quaternionf().rotateY((float) Math.PI);
            Quaternionf z180 = new Quaternionf().rotateZ((float) Math.PI);
            GlobalVariables.headFixQuat = new Quaternionf(z180).invert()
                    .mul(new Quaternionf(y180).invert())
                    .mul(relQuat).mul(y180).mul(z180);

            // 应用渲染 (身箱合一逻辑)
            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();

            // 将模型中心对齐碰撞箱中心，并执行旋转
            poseStack.translate(0, centerY, 0);
            poseStack.mulPose(bodyQuat);
            // 补偿：让原版 1.8 高的模型中心（0.9）对齐支点
            poseStack.translate(0, -0.9f, 0);
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (event.getEntity() instanceof LocalPlayer player && PUSHED_PLAYERS.remove(player)) {
            if (isRenderingInInventory()) {
                event.getPoseStack().popPose();
                return;
            }

            // 弹出矩阵
            event.getPoseStack().popPose();
            GlobalVariables.isPlayerRendering = false;

            // 解冻数值
            player.setXRot(oldXRot); player.xRotO = oldXRotO;
            player.yBodyRot = oldYBodyRot; player.yBodyRotO = oldYBodyRotO;
            player.yHeadRot = oldYHeadRot; player.yHeadRotO = oldYHeadRotO;
        }
    }
}