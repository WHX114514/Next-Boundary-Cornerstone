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

                        // 拿子弹实体当前的运动向量作为基准（此时实体已经自带了 TACZ 计算的初速度和散布）
                        Vec3 currentMovement = entity.getDeltaMovement();
                        float speed = (float) currentMovement.length();

                        // 子弹实体重新用四元数 3D 空间变换
                        Vector3f forwardVec = new Vector3f(0f, 0f, speed);
                        // 应用包含 Roll 的完整 6DoF 四元数变换
                        bulletQuat.transform(forwardVec);

                        // 修正数据重新丢给子弹实体
                        entity.setDeltaMovement(forwardVec.x, forwardVec.y, forwardVec.z);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}