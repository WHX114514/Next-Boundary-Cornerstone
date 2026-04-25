package cn.rbq108.nextboundarycornerstone.body;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.common.EventBusSubscriber;
import net.minecraftforge.client.event.RenderPlayerEvent;
//import net.minecraftforge.common.client.event.RenderPlayerEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;

@Mod.EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class model {

    // 🩺 动画冰冻锁：防止抽搐的核心
    private static float oldXRot, oldYBodyRot, oldYHeadRot;
    private static float oldXRotO, oldYBodyRotO, oldYHeadRotO;

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (event.getEntity() instanceof LocalPlayer player && GlobalVariables.B_LowGravity) {



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

            /*    GlobalVariables.playerHead = renderer.getModel().head;
                GlobalVariables.playerHat = renderer.getModel().hat;*/
            PlayerRenderer renderer = event.getRenderer();
            GlobalVariables.playerHead = renderer.getModel().head;
            GlobalVariables.playerHat = renderer.getModel().hat;


            // 计算头身隔离矩阵
            Quaternionf camQuat = new Quaternionf(GlobalVariables.prevQuat).slerp(GlobalVariables.currentQuat, pt);
            Quaternionf y180 = new Quaternionf().rotateY((float) Math.PI);
            Quaternionf z180 = new Quaternionf().rotateZ((float) Math.PI);
            GlobalVariables.headFixQuat = new Quaternionf(z180).invert()
                    .mul(new Quaternionf(y180).invert())
                    .mul(new Quaternionf(bodyQuat).invert())
                    .mul(camQuat).mul(y180).mul(z180);

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
        if (event.getEntity() instanceof LocalPlayer player && GlobalVariables.B_LowGravity) {

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