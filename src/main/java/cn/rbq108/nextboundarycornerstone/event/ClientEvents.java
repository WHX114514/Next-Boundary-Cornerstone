package cn.rbq108.nextboundarycornerstone.event;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import cn.rbq108.nextboundarycornerstone.motion.control;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.level.GameType;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

/*

这是，一坨究极究极大的屎山
非必要千万别改
真的真的


 */

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class ClientEvents {

    private static boolean lastIsSpectator = false;

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

        // 自由视角与锁定逻辑按键状态更新 (在 Pre-tick 运行以完全消除竞态条件)
        boolean freeCameraKeyDown = cn.rbq108.nextboundarycornerstone.core.Keybinds.B_FREE_CAMERA.isDown();
        boolean fixedCameraKeyDown = cn.rbq108.nextboundarycornerstone.core.Keybinds.B_FIXED_CAMERA.isDown();

        boolean freeCameraKeyTapped = freeCameraKeyDown && !GlobalVariables.prevFreeCameraKeyDown;
        boolean fixedCameraKeyTapped = fixedCameraKeyDown && !GlobalVariables.prevFixedCameraKeyDown;

        GlobalVariables.prevFreeCameraKeyDown = freeCameraKeyDown;
        GlobalVariables.prevFixedCameraKeyDown = fixedCameraKeyDown;

        if (freeCameraKeyTapped && !GlobalVariables.B_FreeCameraToggle) {
            mc.player.displayClientMessage(Component.literal("自由视角").withStyle(ChatFormatting.GREEN), true);
        }

        if (GlobalVariables.B_FreeCameraToggle) {
            // 如果处于锁定自由视角状态，按下 C 键退出
            if (freeCameraKeyTapped) {
                GlobalVariables.B_FreeCameraToggle = false;
                GlobalVariables.B_HeadRotationLocked = false;
            }
        } else {
            // 正常持有模式下，如果处于自由视角并且按下了固定视角键 (Shift)
            if (GlobalVariables.B_FreeCameraActive && fixedCameraKeyTapped) {
                GlobalVariables.B_FreeCameraToggle = true;
                GlobalVariables.B_HeadRotationLocked = true;
                // 保存当前头部的相对位置作为固定角
                GlobalVariables.lockedHeadRelQuat.set(GlobalVariables.headRelQuat);

                // 开启持续拦截所有移动动作，直到本次 Shift 被松开
                GlobalVariables.blockMovementUntilRelease = true;
                GlobalVariables.B_INx = 0;
                GlobalVariables.B_INy = 0;
                GlobalVariables.B_INz = 0;

                mc.player.displayClientMessage(Component.literal("固定头部自由视角").withStyle(ChatFormatting.GOLD), true);
            }
        }

        // 更新 B_FreeCameraActive：按住 C 或者处于 Toggle 锁定状态
        GlobalVariables.B_FreeCameraActive = freeCameraKeyDown || GlobalVariables.B_FreeCameraToggle;

        if (GlobalVariables.blockMovementUntilRelease) {
            // 如果用户仍未松开固定视角键 (Shift)，则继续屏蔽所有移动指令
            if (!cn.rbq108.nextboundarycornerstone.core.Keybinds.B_FIXED_CAMERA.isDown()) {
                GlobalVariables.blockMovementUntilRelease = false;
            } else {
                GlobalVariables.B_INx = 0;
                GlobalVariables.B_INy = 0;
                GlobalVariables.B_INz = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;

        boolean isSpectator = player.isSpectator();
        if (isSpectator && !lastIsSpectator) {
            if (GlobalVariables.prevLowGravity) {
                player.displayClientMessage(
                        Component.literal("失重下观察者模式是否禁用自由度操作\n可在配置文件RestoreVanillaSpectator更改（服务器则请联系腐竹更改）")
                                .withStyle(net.minecraft.ChatFormatting.YELLOW),
                        false
                );
            }
        }
        lastIsSpectator = isSpectator;

        // 备份上一帧的相对旋转角，供第一人称准星渲染时做线性插值消除延迟
        GlobalVariables.prevFreeLookYaw = GlobalVariables.B_freeLookYaw;
        GlobalVariables.prevFreeLookPitch = GlobalVariables.B_freeLookPitch;

        // 感应宇航服头盔状态 (避免直接引用类以防止循环依赖)
        net.minecraft.world.item.ItemStack headStack = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        GlobalVariables.B_SpaceHelmet = !headStack.isEmpty() && 
            net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(headStack.getItem())
            .equals(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("next_boundary_industry", "extravehicular_spacesuit_helmet"));

        //这一帧自己实时感应背包状态
        boolean isWearingBackpack = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST)
                .is(cn.rbq108.nextboundarycornerstone.main.BASIC_BACKPACK.get());
        boolean isSpectatorAndRestored = cn.rbq108.nextboundarycornerstone.ServeMiao.communication.NetworkHandler.isSpectatorAndRestored(player);
        boolean currentRealState;
        if (isSpectatorAndRestored) {
            currentRealState = false;
        } else if (cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_CanBackpackGrantGravity) {
            // 控制权在主模组手里时，才去认 BASIC_BACKPACK 或者 身处太空维度
            boolean isInSpace = player.level().dimension().location().getPath().equals("space");
            currentRealState = cn.rbq108.nextboundarycornerstone.VariableLibrary.debug.FORCE_LOW_GRAVITY || isWearingBackpack || isInSpace;
        } else {
            // 控制权被附属模组抢走时，绝对信任附属模组写入的重力状态！
            currentRealState = player.isNoGravity();
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

            GlobalVariables.headRelQuat.identity();
            GlobalVariables.prevHeadRelQuat.identity();

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

            if (GlobalVariables.blockMovementUntilRelease) {
                // 如果用户仍未松开固定视角键 (Shift)，则继续屏蔽所有移动指令
                if (!cn.rbq108.nextboundarycornerstone.core.Keybinds.B_FIXED_CAMERA.isDown()) {
                    GlobalVariables.blockMovementUntilRelease = false;
                } else {
                    GlobalVariables.B_INx = 0;
                    GlobalVariables.B_INy = 0;
                    GlobalVariables.B_INz = 0;
                }
            }

            // 真实物理硬核限制：如果在失重下且没有穿推进背包，且身体四周1格没有方块可供借力，清空所有推力！
            if (GlobalVariables.B_LowGravity && GlobalVariables.B_CanBackpackGrantGravity) {
                net.minecraft.world.item.ItemStack chest = mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
                if (chest.isEmpty() || !chest.is(cn.rbq108.nextboundarycornerstone.main.BASIC_BACKPACK.get())) {
                    // 没有穿任何推进背包（如果穿了新工业模组背包，B_CanBackpackGrantGravity 就是 false）
                    // 开始对方块进行碰撞盒外扩探测
                    net.minecraft.world.phys.AABB reachBox = mc.player.getBoundingBox().inflate(1.0D);
                    boolean canPushOff = false;
                    for (net.minecraft.core.BlockPos bp : net.minecraft.core.BlockPos.betweenClosed(
                            net.minecraft.util.Mth.floor(reachBox.minX), net.minecraft.util.Mth.floor(reachBox.minY), net.minecraft.util.Mth.floor(reachBox.minZ),
                            net.minecraft.util.Mth.floor(reachBox.maxX), net.minecraft.util.Mth.floor(reachBox.maxY), net.minecraft.util.Mth.floor(reachBox.maxZ))) {
                        net.minecraft.world.level.block.state.BlockState bs = mc.player.level().getBlockState(bp);
                        if (!bs.isAir() && !bs.getCollisionShape(mc.player.level(), bp).isEmpty()) {
                            canPushOff = true;
                            break;
                        }
                    }
                    // 若周围无方块可供借力，则无法产生加速度
                    if (!canPushOff) {
                        GlobalVariables.B_INx = 0;
                        GlobalVariables.B_INy = 0;
                        GlobalVariables.B_INz = 0;
                    }
                }
            }

            //roll轴旋转逻辑
            long window = mc.getWindow().getWindow();
            boolean isShift = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);

            if (!isShift && !GlobalVariables.B_FreeCameraActive) {
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
                if (GlobalVariables.B_FreeCameraActive && !GlobalVariables.B_HeadRotationLocked) {
                    GlobalVariables.B_freeLookRoll += GlobalVariables.currentRollVelocity;
                    GlobalVariables.B_freeLookRoll = net.minecraft.util.Mth.wrapDegrees(GlobalVariables.B_freeLookRoll);
                } else {
                    GlobalVariables.currentQuat.rotateZ((float) Math.toRadians(GlobalVariables.currentRollVelocity));
                }
            }

            // 计算当前相对于身体的相对旋转量，用来推导头部朝向
            Quaternionf bodyQuat = new Quaternionf().rotationYXZ(
                    (float) Math.toRadians(-GlobalVariables.B_Dy),
                    (float) Math.toRadians(GlobalVariables.B_Dx),
                    (float) Math.toRadians(GlobalVariables.B_Dz)
            );
            Quaternionf relQuat = new Quaternionf(bodyQuat).invert().mul(GlobalVariables.currentQuat);

            Quaternionf targetRelQuat = new Quaternionf();

            if (GlobalVariables.B_FreeCameraActive) {
                // 自由视角激活时：身体朝向 (B_Dx, B_Dy, B_Dz) 保持冻结，不从 currentQuat 更新
                
                // 初始化自由视角变量
                if (!GlobalVariables.wasFreeCamera) {
                    GlobalVariables.B_freeLookYaw = 0.0f;
                    GlobalVariables.B_freeLookPitch = 0.0f;
                    GlobalVariables.B_freeLookRoll = 0.0f;
                }

                if (GlobalVariables.B_HeadRotationLocked) {
                    // 如果头部旋转被锁死，视角如同原版那样自由转动，头部固定为锁死瞬间的角度
                    targetRelQuat.set(GlobalVariables.lockedHeadRelQuat);
                } else {
                    // 限制 Yaw/Pitch 在左右/上下 90 度内，Roll 在 60 度内
                    float limit = 90.0f;
                    float clampedYaw = Math.max(-limit, Math.min(limit, GlobalVariables.B_freeLookYaw));
                    float clampedPitch = Math.max(-limit, Math.min(limit, GlobalVariables.B_freeLookPitch));

                    float rollLimit = 60.0f;
                    float clampedRoll = Math.max(-rollLimit, Math.min(rollLimit, GlobalVariables.B_freeLookRoll));

                    // 重建目标相对旋转四元数供头部渲染使用
                    targetRelQuat.rotationYXZ(
                            (float) Math.toRadians(-clampedYaw),
                            (float) Math.toRadians(clampedPitch),
                            (float) Math.toRadians(clampedRoll)
                    );

                    // 更新当前视角的绝对四元数 (不受限位)
                    Quaternionf freeLookRelQuat = new Quaternionf().rotationYXZ(
                            (float) Math.toRadians(-GlobalVariables.B_freeLookYaw),
                            (float) Math.toRadians(GlobalVariables.B_freeLookPitch),
                            (float) Math.toRadians(GlobalVariables.B_freeLookRoll)
                    );
                    GlobalVariables.currentQuat.set(bodyQuat).mul(freeLookRelQuat);
                }
            } else {
                // 如果刚刚从自由视角松开，将视角瞬间对准身体朝向
                if (GlobalVariables.wasFreeCamera) {
                    GlobalVariables.currentQuat.rotationYXZ(
                            (float) Math.toRadians(-GlobalVariables.B_Dy),
                            (float) Math.toRadians(GlobalVariables.B_Dx),
                            (float) Math.toRadians(GlobalVariables.B_Dz)
                    );
                    GlobalVariables.prevQuat.set(GlobalVariables.currentQuat);

                    // 瞬间同步玩家逻辑朝向，防止手部渲染或视角变化抽搐
                    player.setYRot((float) GlobalVariables.B_Dy);
                    player.setXRot((float) GlobalVariables.B_Dx);
                    player.yRotO = player.getYRot();
                    player.xRotO = player.getXRot();

                    mc.player.displayClientMessage(Component.literal("恢复视角").withStyle(ChatFormatting.GRAY), true);
                }

                // 正常状态下，身体角度同步跟随视角四元数
                Vector3f euler = GlobalVariables.currentQuat.getEulerAnglesYXZ(new Vector3f());
                GlobalVariables.B_Dz = Math.toDegrees(euler.z);
                GlobalVariables.B_Dx = Math.toDegrees(euler.x);
                GlobalVariables.B_Dy = Math.toDegrees(-euler.y);

                targetRelQuat.set(relQuat);
            }
            GlobalVariables.wasFreeCamera = GlobalVariables.B_FreeCameraActive;

            if (GlobalVariables.B_HeadRotationLocked) {
                float keyboardRollSpeed = 2.0f;
                if (cn.rbq108.nextboundarycornerstone.core.Keybinds.B_FIXED_CAMERA_LEFT.isDown()) {
                    GlobalVariables.currentQuat.rotateZ((float) Math.toRadians(-keyboardRollSpeed));
                }
                if (cn.rbq108.nextboundarycornerstone.core.Keybinds.B_FIXED_CAMERA_ROLL.isDown()) {
                    GlobalVariables.currentQuat.rotateZ((float) Math.toRadians(keyboardRollSpeed));
                }
            }

            // 备份上一帧头部相对旋转
            GlobalVariables.prevHeadRelQuat.set(GlobalVariables.headRelQuat);

            // 每 tick 线性/球面插值更新头部朝向
            // 如果自由视角开启或者头部仍在归位过程中，用 Slerp 做平滑变化
            if (GlobalVariables.B_FreeCameraActive || GlobalVariables.headRelQuat.angle() > 0.001f) {
                float interpolationSpeed = 0.25f; // 平滑度系数，可以按需调节
                GlobalVariables.headRelQuat.slerp(targetRelQuat, interpolationSpeed);
            } else {
                GlobalVariables.headRelQuat.set(targetRelQuat);
            }

            //阻断与物理注入
            player.xxa = 0.0f; player.yya = 0.0f; player.zza = 0.0f;
            mc.options.keyShift.setDown(false);
            mc.options.keySprint.setDown(false);
            mc.options.keyJump.setDown(false);

            player.setShiftKeyDown(false);
            player.setSprinting(false);
            
            // 接入兼容逻辑：如果玩家正在抓取航空学铁把手，临时撤销飞行状态以允许物理位移
            if (cn.rbq108.nextboundarycornerstone.ModCompatible.isGrabbingAeronauticsHandle(player)) {
                player.getAbilities().flying = false;
            } else {
                player.getAbilities().mayfly = true;
                player.getAbilities().flying = true;
                player.getAbilities().setFlyingSpeed(0.0f);
            }

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
            player.getAbilities().setFlyingSpeed(0.05f);
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
            
            // 落地时强制清除自由视角残留状态
            GlobalVariables.B_FreeCameraActive = false;
            GlobalVariables.wasFreeCamera = false;
            if (!player.isCreative()) {
                //player.getAbilities().mayfly = false;
                //player.getAbilities().flying = false;
            }
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        float partialTick = (float) event.getPartialTick();
        double finalFov = event.getFOV();
        if (GlobalVariables.B_LowGravity) {
            // 渲染帧线性插值 (消除卡顿)
            float smoothedFov = GlobalVariables.prevFovModifier +
                    (GlobalVariables.currentFovModifier - GlobalVariables.prevFovModifier) * partialTick;
            finalFov += smoothedFov;
            event.setFOV(finalFov);
        }
        GlobalVariables.lastComputedFov = (float) finalFov;
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (GlobalVariables.B_LowGravity && GlobalVariables.B_HeadRotationLocked) {
            double deltaY = event.getScrollDeltaY();
            if (deltaY != 0) {
                // 滚轮向上 (deltaY > 0) -> 左滚转 (B_FIXED_CAMERA_LEFT)
                // 滚轮向下 (deltaY < 0) -> 右滚转 (B_FIXED_CAMERA_ROLL)
                float rollSpeed = 5.0f; // 滚轮滚转灵敏度
                float rollAmount = (float) (-deltaY * rollSpeed);
                GlobalVariables.currentQuat.rotateZ((float) Math.toRadians(rollAmount));

                // 取消事件以阻止原版快捷栏切换槽位
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (GlobalVariables.B_LowGravity && GlobalVariables.B_HeadRotationLocked) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == null) {
                int button = event.getButton();
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    event.setCanceled(true);
                }
            }
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