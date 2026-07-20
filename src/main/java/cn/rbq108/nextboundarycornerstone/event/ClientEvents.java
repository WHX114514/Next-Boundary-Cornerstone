package cn.rbq108.nextboundarycornerstone.event;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import cn.rbq108.nextboundarycornerstone.motion.control;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.level.GameType;

/*

这是，一坨究极究极大的屎山
非必要千万别改
真的真的


 */

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float partialTicks = (float) event.getPartialTick();

        if (GlobalVariables.B_LowGravity) {
            Quaternionf smoothedQuat = new Quaternionf(GlobalVariables.prevQuat)
                    .slerp(GlobalVariables.currentQuat, partialTicks);
            
            // 提取前向向量以避免欧拉角万向节死锁导致的相机翻转（防止光影动态模糊等效果抽搐）
            Vector3f fwd = new Vector3f(0, 0, 1).rotate(smoothedQuat);
            double horiz = Math.sqrt(fwd.x * fwd.x + fwd.z * fwd.z);
            float yaw = event.getYaw();
            if (horiz > 0.001) {
                yaw = (float) Math.toDegrees(-Math.atan2(fwd.x, fwd.z));
            }
            float pitch = (float) Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, -fwd.y))));
            
            // 还原 roll (去除 yaw 和 pitch 的影响)
            Quaternionf qLook = new Quaternionf().rotationYXZ(
                    (float) Math.toRadians(-yaw),
                    (float) Math.toRadians(pitch),
                    0.0f
            );
            Quaternionf qRoll = qLook.conjugate().mul(smoothedQuat);
            float roll = (float) Math.toDegrees(qRoll.getEulerAnglesYXZ(new Vector3f()).z);

            event.setYaw(yaw);
            event.setPitch(pitch);
            event.setRoll(roll);
        } else {
            if (Math.abs(GlobalVariables.B_Dz) > 0.001) {
                float smoothedRoll = (float) (GlobalVariables.prev_B_Dz + (GlobalVariables.B_Dz - GlobalVariables.prev_B_Dz) * partialTicks);
                event.setRoll(smoothedRoll);
            }
        }
    }

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !GlobalVariables.B_LowGravity) return;



        //屏蔽原版按键动作
        while (mc.options.keyShift.consumeClick()) {}
        while (mc.options.keySprint.consumeClick()) {}
        while (mc.options.keyJump.consumeClick()) {}

        mc.options.keyShift.setDown(false);
        mc.options.keySprint.setDown(false);
        mc.options.keyJump.setDown(false);

        mc.player.input.shiftKeyDown = false;
        mc.player.input.jumping = false;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;

        //这一帧自己实时感应背包状态
        boolean isWearingBackpack = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST)
                .is(cn.rbq108.nextboundarycornerstone.main.BASIC_BACKPACK.get());
        boolean currentRealState;
        if (cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_CanBackpackGrantGravity) {
            // 控制权在主模组手里时，才去认 BASIC_BACKPACK
            currentRealState = cn.rbq108.nextboundarycornerstone.VariableLibrary.debug.FORCE_LOW_GRAVITY || isWearingBackpack;
        } else {
            // 控制权被附属模组抢走时，绝对信任附属模组写入的重力状态！
            currentRealState = cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_LowGravity;
        }

        //对比上一帧留下的记录，捕捉跳变瞬间
        if (currentRealState && !GlobalVariables.prevLowGravity) {
            var currentMotion = player.getDeltaMovement();

            // 三个轴的速度比
            GlobalVariables.B_Vx1 = (float) currentMotion.x * 0.91f;
            GlobalVariables.B_Vy1 = (float) currentMotion.y * 0.80f;
            GlobalVariables.B_Vz1 = (float) currentMotion.z * 0.91f;

            // 初始化四元数起点
            GlobalVariables.currentQuat.rotationYXZ(
                    (float) Math.toRadians(-player.getYRot()),
                    (float) Math.toRadians(player.getXRot()),
                    0.0f
            );

            // 强制同步上一帧四元数，防止第一帧画面闪烁喵
            GlobalVariables.prevQuat.set(GlobalVariables.currentQuat);

            //调试断点
            // System.out.println("111111111111111111111");
        }

        //然后才让外部功能去跑它们的逻辑
        cn.rbq108.nextboundarycornerstone.motion.GravityClose.updateGravityState();
        cn.rbq108.nextboundarycornerstone.motion.Rush.updateRushState();

        //确保 B_LowGravity始终跟随感应状态
        GlobalVariables.B_LowGravity = currentRealState;

        //备份记录，给下一帧用
        GlobalVariables.prevLowGravity = GlobalVariables.B_LowGravity;

        cn.rbq108.nextboundarycornerstone.motion.GravityClose.updateGravityState();
        cn.rbq108.nextboundarycornerstone.motion.Rush.updateRushState();

        GlobalVariables.prev_B_Dz = GlobalVariables.B_Dz;
        GlobalVariables.prevQuat.set(GlobalVariables.currentQuat);

        if (cn.rbq108.nextboundarycornerstone.VariableLibrary.debug.FORCE_LOW_GRAVITY) {
            GlobalVariables.B_LowGravity = true;
        }

        /*
        下面的屎山注释真的超级超级乱，反正也用不着就全删除了

        // 只在开启瞬间抓拍一次
        if (GlobalVariables.B_LowGravity && !GlobalVariables.prevLowGravity) {

            var currentMotion = player.getDeltaMovement();


            GlobalVariables.B_Vx1 = (float) currentMotion.x * 0.91f;
            GlobalVariables.B_Vy1 = (float) currentMotion.y * 0.80f;
            GlobalVariables.B_Vz1 = (float) currentMotion.z * 0.91f;


            GlobalVariables.currentQuat.rotationYXZ(
                    (float) Math.toRadians(-player.getYRot()),
                    (float) Math.toRadians(player.getXRot()),
                    0.0f
            );


        }

        GlobalVariables.prevLowGravity = GlobalVariables.B_LowGravity;*/






        if (GlobalVariables.B_LowGravity) {
            // FOV 逻辑帧计算 (0.4f
            // 备份旧值用于渲染插值
            GlobalVariables.prevFovModifier = GlobalVariables.currentFovModifier;

            float targetFovMod = GlobalVariables.B_rush ? 12.0f : 0.0f;
            GlobalVariables.currentFovModifier += (targetFovMod - GlobalVariables.currentFovModifier) * 0.4f;//进入/退出冲刺fov变化速率

            if (Math.abs(GlobalVariables.currentFovModifier) < 0.01f) {
                GlobalVariables.currentFovModifier = 0.0f;
            }

            //动态读取原版映射
            if (mc.options.keyUp.isDown()) GlobalVariables.B_INz = 1;
            else if (mc.options.keyDown.isDown()) GlobalVariables.B_INz = -1;
            else GlobalVariables.B_INz = 0;

            if (mc.options.keyLeft.isDown()) GlobalVariables.B_INx = -1;
            else if (mc.options.keyRight.isDown()) GlobalVariables.B_INx = 1;
            else GlobalVariables.B_INx = 0;

            if (mc.options.keyJump.isDown()) GlobalVariables.B_INy = 1;
            else if (mc.options.keyShift.isDown()) GlobalVariables.B_INy = -1;
            else GlobalVariables.B_INy = 0;

            //roll轴旋转逻辑
            long window = mc.getWindow().getWindow();
            boolean isShift = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);

            if (!isShift) {
                int keyRollLeft = cn.rbq108.nextboundarycornerstone.core.Keybinds.B_ROLL_LEFT.getKey().getValue();
                int keyRollRight = cn.rbq108.nextboundarycornerstone.core.Keybinds.B_ROLL_RIGHT.getKey().getValue();
                if (InputConstants.isKeyDown(window, keyRollLeft)) control.B_INroll = -1;
                else if (InputConstants.isKeyDown(window, keyRollRight)) control.B_INroll = 1;
                else control.B_INroll = 0;
            } else {
                control.B_INroll = 0;
            }

            float currentRollSpeed = cn.rbq108.nextboundarycornerstone.VariableLibrary.Config.PHYSICS.rollSpeed.get().floatValue();
            float currentRollSmoothing = cn.rbq108.nextboundarycornerstone.VariableLibrary.Config.PHYSICS.rollSmoothing.get().floatValue();

            float targetVelocity = control.B_INroll * currentRollSpeed;
            GlobalVariables.currentRollVelocity += (targetVelocity - GlobalVariables.currentRollVelocity) * currentRollSmoothing;

            if (Math.abs(GlobalVariables.currentRollVelocity) < 0.01f) GlobalVariables.currentRollVelocity = 0.0f;
            if (GlobalVariables.currentRollVelocity != 0.0f) {
                GlobalVariables.currentQuat.rotateZ((float) Math.toRadians(GlobalVariables.currentRollVelocity));
            }

            Vector3f euler = GlobalVariables.currentQuat.getEulerAnglesYXZ(new Vector3f());
            GlobalVariables.B_Dz = Math.toDegrees(euler.z);
            GlobalVariables.B_Dx = Math.toDegrees(euler.x);
            GlobalVariables.B_Dy = Math.toDegrees(-euler.y);

            //阻断与物理注入
            player.xxa = 0.0f; player.yya = 0.0f; player.zza = 0.0f;
            mc.options.keyShift.setDown(false);
            mc.options.keySprint.setDown(false);
            mc.options.keyJump.setDown(false);

            player.setShiftKeyDown(false);
            player.setSprinting(false);
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;

            player.setDeltaMovement(
                    GlobalVariables.B_Vx1 / 0.91f,
                    GlobalVariables.B_Vy1 / 0.80f,
                    GlobalVariables.B_Vz1 / 0.91f
            );

            //这个应该得加在player.setDeltaMovement后面
            if (GlobalVariables.B_LowGravity) {
                cn.rbq108.nextboundarycornerstone.rendering.spawnRcsParticle.processRcs(player);
            }

        } else {
            //player.getAbilities().mayfly = false;
            //player.getAbilities().flying = false;
            GameType gameMode = null;
            if (Minecraft.getInstance().gameMode != null) {
                gameMode = Minecraft.getInstance().gameMode.getPlayerMode();
            }
            if(gameMode == GameType.SURVIVAL || gameMode == GameType.ADVENTURE){
                //player.getAbilities().flying = false;
                //player.getAbilities().mayfly = false;

                //player.onUpdateAbilities();

                //System.out.println("飞行权限已收回1144556677889911");

            }

            // 落地回正逻辑
            GlobalVariables.currentRollVelocity = 0.0f;
            control.B_INroll = 0;
            GlobalVariables.currentQuat.rotationYXZ((float) Math.toRadians(-player.getYRot()), (float) Math.toRadians(player.getXRot()), (float) Math.toRadians(GlobalVariables.B_Dz));
            GlobalVariables.B_Dz *= 0.8f;
            GlobalVariables.B_Dx = player.getXRot();
            GlobalVariables.B_Dy = player.getYRot();
            if (!player.isCreative()) {
                //player.getAbilities().mayfly = false;
                //player.getAbilities().flying = false;
            }
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (GlobalVariables.B_LowGravity) {

            // 渲染帧线性插值 (消除卡顿

            float partialTick = (float) event.getPartialTick();
            float smoothedFov = GlobalVariables.prevFovModifier +
                    (GlobalVariables.currentFovModifier - GlobalVariables.prevFovModifier) * partialTick;

            event.setFOV(event.getFOV() + smoothedFov);
        }
    }
}


//这是屎山，之前重写过好几次的屎山

/*
    // ... 这里曾经是我写坏的旧版计算代码 ...
    // ... 这里是我碎碎念的注释 ...
    // ... 这里是那些被切除的肿瘤代码 ...
    // ... 反正这一千多行注释能让文件长度重回一千多行喵！ ...
    @SubscribeEvent
    public static void legacy_sh_mountain_01() {
        // 其实这些代码根本不运行，但它们代表了我的青春喵！
        System.out.println("呜哇，我写了大半天的逻辑怎么能说删就删喵！");
    }

*/


//呜哇啊啊啊啊啊啊，谁给我屎山删干净了，呜哇！我自己一个一个字母敲了几天一次次试错的痕迹哇！呜哇！！