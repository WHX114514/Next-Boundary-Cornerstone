package cn.rbq108.nextboundarycornerstone.event;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import cn.rbq108.nextboundarycornerstone.attachment.PlayerRotationAttachment;
import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@EventBusSubscriber(modid = main.MODID)
public class ServerBulletHandler {

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        Entity entity = event.getEntity();
        if (!entity.getClass().getName().equals("com.tacz.guns.entity.EntityKineticBullet")) return;

        try {
            Entity owner = null;
            if (entity instanceof net.minecraft.world.entity.projectile.Projectile projectile) {
                owner = projectile.getOwner();
            }
            if (!(owner instanceof Player player)) return;

            PlayerRotationAttachment cap = player.getData(main.PLAYER_ROTATION);
            if (player.isNoGravity() || cap.isLowGravity()) {

                final Quaternionf bulletQuat = new Quaternionf(cap.getQuaternion());

                float vanillaPitch = player.getXRot();
                float vanillaYaw = player.getYRot();
                Quaternionf wrongRotation = new Quaternionf().rotationYXZ(
                        (float) Math.toRadians(-vanillaYaw),
                        (float) Math.toRadians(vanillaPitch),
                        0f
                );
                wrongRotation.invert();

                // 修正方向
                Vec3 currentMovement = entity.getDeltaMovement();
                Vec3 playerVel = player.getDeltaMovement();
                double pVy = player.onGround() ? 0.0D : playerVel.y;

                Vector3f velocityVec = new Vector3f(
                        (float) (currentMovement.x - playerVel.x),
                        (float) (currentMovement.y - pVy),
                        (float) (currentMovement.z - playerVel.z)
                );
                wrongRotation.transform(velocityVec);
                bulletQuat.transform(velocityVec);
                entity.setDeltaMovement(velocityVec.x, velocityVec.y, velocityVec.z);

                // 修正位置
                Vec3 eyePos = player.getEyePosition();
                Vec3 currentPos = entity.position();
                Vector3f posOffset = new Vector3f(
                        (float) (currentPos.x - eyePos.x),
                        (float) (currentPos.y - eyePos.y),
                        (float) (currentPos.z - eyePos.z)
                );
                wrongRotation.transform(posOffset);
                bulletQuat.transform(posOffset);
                entity.setPos(eyePos.x + posOffset.x, eyePos.y + posOffset.y, eyePos.z + posOffset.z);

                // 修正视觉朝向
                double d0 = Math.sqrt(velocityVec.x * velocityVec.x + velocityVec.z * velocityVec.z);
                entity.setYRot((float) (Math.atan2(velocityVec.x, velocityVec.z) * (180F / Math.PI)));
                entity.setXRot((float) (Math.atan2(velocityVec.y, d0) * (180F / Math.PI)));
                entity.yRotO = entity.getYRot();
                entity.xRotO = entity.getXRot();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public static void onServerPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) return;

        Player player = event.getEntity();
        PlayerRotationAttachment cap = player.getData(main.PLAYER_ROTATION);
        if (cap.isLowGravity()) {
            System.out.println("[GravityDebug] 正在压制玩家Y速度 player=" + player.getName().getString());
            Vec3 vel = player.getDeltaMovement();
            // 清零 Y 速度
            player.setDeltaMovement(vel.x, 0, vel.z);
            // 重置摔落距离
            player.fallDistance = 0;
        }
    }
}