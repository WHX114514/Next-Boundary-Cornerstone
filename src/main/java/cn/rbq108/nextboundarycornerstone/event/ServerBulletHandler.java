package cn.rbq108.nextboundarycornerstone.event;

import cn.rbq108.nextboundarycornerstone.attachment.PlayerRotationAttachment;
import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

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

            if (player.isNoGravity()) {
                // Launch already uses FlightAim. Preserve TaCZ's legacy no-inertia behavior only.
                Vec3 playerVel = player.getDeltaMovement();
                entity.setDeltaMovement(entity.getDeltaMovement().subtract(
                        playerVel.x, player.onGround() ? 0 : playerVel.y, playerVel.z));
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
            // System.out.println("[GravityDebug] 正在压制玩家Y速度 player=" + player.getName().getString());
            Vec3 vel = player.getDeltaMovement();
            // 清零 Y 速度
            player.setDeltaMovement(vel.x, 0, vel.z);
            // 重置摔落距离
            player.fallDistance = 0;
        }
    }
}
