package cn.rbq108.nextboundarycornerstone.body;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;

// 重点！绝对不能加 value = Dist.CLIENT！必须让服务端（负责算物理）也能跑这段代码！
@EventBusSubscriber(modid = main.MODID)
public class hitbox {

    // 尺寸定义
    @SubscribeEvent
    public static void onPlayerSize(EntityEvent.Size event) {
        if (event.getEntity() instanceof Player player && player.isNoGravity()) {
            float size = GlobalVariables.B_HitboxSize;
            // 除了眼睛跟随头部移动，其他的骨骼结构全部保留
            event.setNewSize(EntityDimensions.scalable(size, size));
            event.setNewEyeHeight(BodyEyes.neutralHeight(size, 1.0f));
        }
    }

    //双端物理同步
    // 这个事件会在客户端和服务端双端触发
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        Player player = event.player;

        if (player.isNoGravity()) {
            net.minecraft.world.item.ItemStack chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
            boolean isWearingBaseBackpack = chest.is(cn.rbq108.nextboundarycornerstone.main.BASIC_BACKPACK.get());
            boolean isWearingIndustryBackpack = player.getPersistentData().getBoolean("next_boundary_industry.is_wearing_eva");

            if (!isWearingBaseBackpack && !isWearingIndustryBackpack) {
                // 无背包推进器时，展示水平悬浮/游泳姿态
                player.setPose(Pose.SWIMMING);
                player.setSwimming(true);
            } else {
                // qiangzhizhanli!!!! (穿戴背包时保持直立)
                player.setPose(Pose.STANDING);
                player.setSwimming(false);
            }

            // 如果服务端碰撞箱还没改，强制刷新
            if (Math.abs(player.getBbHeight() - GlobalVariables.B_HitboxSize) > 0.01f) {
                player.refreshDimensions();
            }
        } else {
            if (cn.rbq108.nextboundarycornerstone.attachment.PlayerRotationAttachment.get(player).getHitboxPose() != null) {
                cn.rbq108.nextboundarycornerstone.attachment.PlayerRotationAttachment.get(player).setHitboxPose(null);
                cn.rbq108.nextboundarycornerstone.attachment.PlayerRotationAttachment.get(player).setBodyBlocked(false);
            }
            // 如果关闭失重操作，但盒子还是小的，强制变回原状
            if (Math.abs(player.getBbHeight() - GlobalVariables.B_HitboxSize) < 0.01f) {
                player.refreshDimensions();
            }
        }
    }
}
