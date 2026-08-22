package cn.rbq108.nextboundarycornerstone.body;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

// 重点！绝对不能加 value = Dist.CLIENT！必须让服务端（负责算物理）也能跑这段代码！
@EventBusSubscriber(modid = main.MODID)
public class hitbox {

    // 尺寸定义
    @SubscribeEvent
    public static void onPlayerSize(EntityEvent.Size event) {
        if (event.getEntity() instanceof Player && GlobalVariables.B_LowGravity) {
            float size = GlobalVariables.B_HitboxSize;
            // 锁定眼高，防止视角卡进方块窒息

            //定义“上移偏移量” (单位：格)
            // 可以随时调这个数字，越大碰撞箱“看起来”就越高
            float offsetY = 0.5f;

            //眼高加上偏移量，抬高摄像头
            event.setNewSize(EntityDimensions.scalable(size, size)
                    .withEyeHeight(size * 0.85f + offsetY));

            event.setNewSize(EntityDimensions.scalable(size, size).withEyeHeight(size * 0.6f));
        }
    }

    //双端物理同步
    // 这个事件会在客户端和服务端双端触发
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        if (GlobalVariables.B_LowGravity) {
            net.minecraft.world.item.ItemStack chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
            boolean isWearingBaseBackpack = chest.is(cn.rbq108.nextboundarycornerstone.main.BASIC_BACKPACK.get());
            boolean isWearingIndustryBackpack = !GlobalVariables.B_CanBackpackGrantGravity;
            
            if (!isWearingBaseBackpack && !isWearingIndustryBackpack) {
                // 无背包推进器时，展示水平悬浮/游泳姿态
                player.setPose(Pose.SWIMMING);
            } else {
                // qiangzhizhanli!!!! (穿戴背包时保持直立)
                player.setPose(Pose.STANDING);
            }

            // 如果服务端碰撞箱还没改，强制刷新
            if (Math.abs(player.getBbHeight() - GlobalVariables.B_HitboxSize) > 0.01f) {
                player.refreshDimensions();
            }
        } else {
            // 如果关闭失重操作，但盒子还是小的，强制变回原状
            if (Math.abs(player.getBbHeight() - GlobalVariables.B_HitboxSize) < 0.01f) {
                player.refreshDimensions();
            }
        }
    }
}