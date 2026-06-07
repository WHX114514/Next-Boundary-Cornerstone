package cn.rbq108.nextboundarycornerstone.event;

import cn.rbq108.nextboundarycornerstone.capability.PlayerRotationProvider;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = main.MODID)
public class ServerBulletHandler {

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;

        // 订阅实体的加入事件，在这里对TACZ的子弹拦截修正参数
        Entity entity = event.getEntity();
        // 匹配TACZ的子弹实体类
        if (entity.getClass().getName().equals("com.tacz.guns.entity.EntityKineticBullet")) {

            try {
                // 拿到 projectile 的 owner
                Entity owner = null;
                if (entity instanceof net.minecraft.world.entity.projectile.Projectile projectile) {
                    owner = projectile.getOwner();
                }

                if (owner instanceof Player player) {
                    if (GlobalVariables.B_LowGravity) {
                        final Quaternionf bulletQuat = new Quaternionf();

                        // 提取服务端附加在 Player 身上的 3D 四元数
                        player.getCapability(PlayerRotationProvider.PLAYER_ROTATION).ifPresent(cap -> {
                            bulletQuat.set(cap.getQuaternion());
                        });

                        // 提前计算原版的错误旋转及其逆矩阵 (Undo 矩阵)
                        float vanillaPitch = player.getXRot();
                        float vanillaYaw = player.getYRot();
                        Quaternionf wrongRotation = new Quaternionf().rotationYXZ(
                                (float) Math.toRadians(-vanillaYaw),
                                (float) Math.toRadians(vanillaPitch),
                                0f
                        );
                        wrongRotation.invert();

                        // ==========================================
                        // 修正 1：方向与散布重构
                        // ==========================================
// ==========================================
                        // 修正 1：方向与散布重构
                        // ==========================================
                        Vec3 currentMovement = entity.getDeltaMovement();

                        // 【核心修复】：剔除 TACZ 强加的玩家惯性速度！
                        // 这样提取出来的 velocityVec 就只剩下：纯净的发射方向 + 枪械散布
                        Vec3 playerVel = player.getDeltaMovement();
                        double pVx = playerVel.x;
                        double pVy = player.onGround() ? 0.0D : playerVel.y;
                        double pVz = playerVel.z;

                        Vector3f velocityVec = new Vector3f(
                                (float)(currentMovement.x - pVx),
                                (float)(currentMovement.y - pVy),
                                (float)(currentMovement.z - pVz)
                        );

                        // 撤销错误欧拉角，把向量打回“局部空间”（此时仅保留枪械散布的偏移）
                        wrongRotation.transform(velocityVec);

                        // 套用真实的 6DoF 旋转矩阵
                        bulletQuat.transform(velocityVec);

                        // (可选探讨：如果你想让子弹在太空中真实继承飞船/玩家的速度，
                        // 可以把 playerVel 用 bulletQuat 旋转后加回来，但通常在射击游戏里不加手感最好)

                        // 赋予实体最终完美的真理弹道
                        entity.setDeltaMovement(velocityVec.x, velocityVec.y, velocityVec.z);
                        // ==========================================
                        // 修正 2：生成位置重构 (解决头顶射子弹问题)
                        // ==========================================
                        Vec3 eyePos = player.getEyePosition();
                        Vec3 currentPos = entity.position();

                        // 获取当前子弹相对于眼睛的物理偏移量 (包含世界Y轴写死的 -0.1 以及可能的前向延伸)
                        Vector3f posOffset = new Vector3f(
                                (float)(currentPos.x - eyePos.x),
                                (float)(currentPos.y - eyePos.y),
                                (float)(currentPos.z - eyePos.z)
                        );

                        // 将写死的偏移量打回“局部相机空间”
                        wrongRotation.transform(posOffset);
                        // 用 6DoF 带着 Roll 一起旋转到正确的 3D 空间位置
                        bulletQuat.transform(posOffset);

                        // 重新设定子弹的出发点
                        entity.setPos(eyePos.x + posOffset.x, eyePos.y + posOffset.y, eyePos.z + posOffset.z);

                        // ==========================================
                        // 修正 3：实体视觉朝向 (防止子弹模型歪斜)
                        // ==========================================
                        double d0 = Math.sqrt(velocityVec.x * velocityVec.x + velocityVec.z * velocityVec.z);
                        entity.setYRot((float)(net.minecraft.util.Mth.atan2(velocityVec.x, velocityVec.z) * (180F / Math.PI)));
                        entity.setXRot((float)(net.minecraft.util.Mth.atan2(velocityVec.y, d0) * (180F / Math.PI)));
                        entity.yRotO = entity.getYRot();
                        entity.xRotO = entity.getXRot();
                    }
//                    if (GlobalVariables.B_LowGravity) {
//                        final Quaternionf bulletQuat = new Quaternionf();
//
//                        // 提取服务端附加在 Player 身上的 3D 四元数
//                        player.getCapability(PlayerRotationProvider.PLAYER_ROTATION).ifPresent(cap -> {
//                            bulletQuat.set(cap.getQuaternion());
//                        });
//
//                        // 拿子弹实体当前的运动向量作为基准（此时实体已经自带了 TACZ 计算的初速度和散布）
//                        Vec3 currentMovement = entity.getDeltaMovement();
//                        float speed = (float) currentMovement.length();
//
//                        // 子弹实体重新用四元数 3D 空间变换
//                        Vector3f forwardVec = new Vector3f(0f, 0f, speed);
//                        // 应用包含 Roll 的完整 6DoF 四元数变换
//                        bulletQuat.transform(forwardVec);
//
//                        // 修正数据重新丢给子弹实体
//                        entity.setDeltaMovement(forwardVec.x, forwardVec.y, forwardVec.z);
//                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}