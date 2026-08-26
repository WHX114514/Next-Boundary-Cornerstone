/*package cn.rbq108.test.body;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;

/**
 * 专门负责 6DOF 状态下的玩家姿态绝对控制喵！
 *
public class motion {

    @SuppressWarnings("rawtypes")
    public static void applyFlightPosture(HumanoidModel model) {
        // --- 1. 左手绝对锁定 (-25度) ---
        float armAngle = (float) Math.toRadians(-25.0f);
        model.leftArm.xRot = armAngle;
        model.leftArm.yRot = 0.0f;
        model.leftArm.zRot = 0.0f;

        // --- 2. 右手垂直焊死 ---
        model.rightArm.xRot = 0.0f;
        model.rightArm.yRot = 0.0f;
        model.rightArm.zRot = 0.0f;

        // --- 3. 双腿三轴全部焊死 (根治乱动) ---
        model.leftLeg.xRot = 0.0f;
        model.leftLeg.yRot = 0.0f;
        model.leftLeg.zRot = 0.0f;

        model.rightLeg.xRot = 0.0f;
        model.rightLeg.yRot = 0.0f;
        model.rightLeg.zRot = 0.0f;

        // --- 4. 身体和头部摆正 ---
        model.body.xRot = 0.0f;
        model.body.yRot = 0.0f;
        model.body.zRot = 0.0f;
        model.head.xRot = 0.0f;

        // --- 5. 外套/装甲层完美同步喵！ ---
        if (model instanceof PlayerModel playerModel) {
            playerModel.leftSleeve.copyFrom(model.leftArm);
            playerModel.rightSleeve.copyFrom(model.rightArm);
            playerModel.leftPants.copyFrom(model.leftLeg);
            playerModel.rightPants.copyFrom(model.rightLeg);
            playerModel.jacket.copyFrom(model.body);
        }
    }
}*/
/*package cn.rbq108.test.body;

import cn.rbq108.test.main;
import cn.rbq108.test.VariableLibrary.GlobalVariables;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;


 * 专门负责 6DOF 状态下的玩家姿态绝对控制喵！

// --- 🩺 重点修正：把这里的 @EventBusSubscriber 全部删掉喵呜！ ---
public class motion {

    /*
     * 🩺 被 Mixin 调用的终极控制逻辑
     * 因为是被 Mixin 直接调用的，所以不需要 @SubscribeEvent 喵！
     *
    @SuppressWarnings("rawtypes")
    public static void applyFlightPosture(HumanoidModel model) {
        // --- 1. 左手绝对锁定 ---
        float armAngle = (float) Math.toRadians(-25.0f);

        model.leftArm.xRot = armAngle;
        model.leftArm.yRot = 0.0f;
        model.leftArm.zRot = 0.0f;

        // --- 2. 皮肤外层处理 ---
        if (model instanceof PlayerModel playerModel) {
            playerModel.leftSleeve.xRot = armAngle;
            playerModel.leftSleeve.yRot = 0.0f;
            playerModel.leftSleeve.zRot = 0.0f;

            playerModel.rightSleeve.copyFrom(model.rightArm);
            playerModel.leftPants.copyFrom(model.leftLeg);
            playerModel.rightPants.copyFrom(model.rightLeg);
        }

        // --- 3. 锁定其他部位防止呼吸干扰 ---
        model.rightArm.xRot = 0.0f;
        model.rightArm.yRot = 0.0f;
        model.rightArm.zRot = 0.0f;
        model.leftLeg.xRot = 0.0f;
        model.rightLeg.xRot = 0.0f;
        model.body.xRot = 0.0f;
        model.head.xRot = 0.0f;
    }
}*/

/*package cn.rbq108.test.body;

import cn.rbq108.test.main;
import cn.rbq108.test.VariableLibrary.GlobalVariables;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;


 //* 专门负责 6DOF 状态下的玩家姿态整形喵！

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class motion {

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        // 只要开启了 6DOF 飞行喵
        if (GlobalVariables.B_LowGravity) {
            var player = event.getEntity();
            var model = event.getRenderer().getModel();

            // --- 🩺 1. 压制空中踏步（只关掉动画速度，不锁骨架喵） ---
            // 这样原版就不会去算摆腿的角度了，你就不会在空中乱蹬了喵呜！
            player.walkAnimation.setSpeed(0.0f);

            // --- 🩺 2. 强制介入左手角度喵！ ---
            // 弧度制：-25度大约是 -0.436。
            // 建议测试时可以先改成 Math.toRadians(-90.0f) 看看手有没有平举喵呜！
            float armAngle = (float) Math.toRadians(-25.0f);

            model.leftArm.xRot = armAngle;
            model.leftSleeve.xRot = armAngle; // 别忘了皮肤外层也要改喵！

            // --- 🩺 3. 彻底封死左手的其他摆动喵 ---
            // 这一步是为了防止原版“呼吸”或“手臂晃动”动画把左手拽走喵呜！
            model.leftArm.yRot = 0.0f;
            model.leftArm.zRot = 0.0f;
            model.leftSleeve.yRot = 0.0f;
            model.leftSleeve.zRot = 0.0f;

            // 除了左手和走路动画，其他部位（右手、双腿、身体）本神医一个字母都没碰喵！
        }
    }
}
/*package cn.rbq108.test.body;

import cn.rbq108.test.main;
import cn.rbq108.test.VariableLibrary.GlobalVariables;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;


 // 专门负责 6DOF 状态下的玩家姿态整形喵！

// 🩺 重点：必须是 GAME 总线，且监听 Pre 事件喵！
@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class motion {

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        // 只要 B_LowGravity 开启，就接管渲染逻辑喵呜！
        if (GlobalVariables.B_LowGravity) {
            var player = event.getEntity();
            var model = event.getRenderer().getModel();

            // --- 🩺 1. 彻底掐死“走路动作”的源头喵！ ---
            // 只要这一帧速度是 0，腿就绝对不会动喵呜！
            player.walkAnimation.setSpeed(0.0f);

            // --- 🩺 2. 强行锁定骨架旋转喵！ ---
            // 既然你怀疑 -90.0f 有问题，咱们先试一个明显的 25 度前倾（-0.43 弧度）喵~
            float armAngle = (float) Math.toRadians(-25.0f);

            // 左手（包括外套层）统一前倾喵！
            model.leftArm.xRot = armAngle;
            model.leftSleeve.xRot = armAngle;

            // 右手、双腿全部垂直锁死，不准摆动喵呜！
            model.rightArm.xRot = 0.0f;
            model.rightSleeve.xRot = 0.0f;

            model.leftLeg.xRot = 0.0f;
            model.leftPants.xRot = 0.0f;
            model.rightLeg.xRot = 0.0f;
            model.rightPants.xRot = 0.0f;

            // 身体和头也保持正位，把旋转权交给相机四元数喵！
            model.body.xRot = 0.0f;
            model.head.xRot = 0.0f;

            // 🧪 如果你在游戏里还是看不到变化，请取消下面这行的注释喵！
             //System.out.println(">>> 姿态系统运行中：左手角度 = " + armAngle);
        }
    }
}*/

package cn.rbq108.nextboundarycornerstone.body;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;


 // 专门负责 6DOF 状态下的玩家姿态整形喵！

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class motion {

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        // 只有在开启 6DOF 低重力时才接管姿态
        if (GlobalVariables.B_LowGravity) {
            //修正：通过 getRenderer() 拿模型喵
            var model = event.getRenderer().getModel();
            var player = event.getEntity();

            // 如果当前玩家处于游泳/横躺悬浮姿态，跳过单手平举的骨架控制，避免污染姿态
            if (player.getPose() == net.minecraft.world.entity.Pose.SWIMMING) {
                return;
            }

            // 左手前倾 25 度喵
            // -25度转换成弧度，负值是向前平举喵呜~
            model.leftArm.xRot = (float) Math.toRadians(-90.0f);

            //彻底定住双腿和右手（不过似乎没变化？）
            model.rightArm.xRot = 0.0f;
            model.leftLeg.xRot = 0.0f;
            model.rightLeg.xRot = 0.0f;


            model.body.xRot = 0.0f;
            model.head.xRot = 0.0f;


            player.walkAnimation.setSpeed(0.0f);
        }
    }
}