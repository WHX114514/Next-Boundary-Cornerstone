package cn.rbq108.nextboundarycornerstone.body;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 1.20.1 正确的事件导包路径
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.TickEvent;

// 重点！绝对不能加 value = Dist.CLIENT！必须让服务端（负责算物理）也能跑这段代码！
@Mod.EventBusSubscriber(modid = main.MODID)
public class hitbox {

    // 尺寸定义
    @SubscribeEvent
    public static void onPlayerSize(EntityEvent.Size event) {
        if (event.getEntity() instanceof Player && GlobalVariables.B_LowGravity) {
            float size = GlobalVariables.B_HitboxSize;
            float offsetY = 0.5f;

            // 1.20.1 修复：分别设置碰撞箱尺寸和眼高，不能使用 withEyeHeight() 链式调用
            event.setNewSize(EntityDimensions.scalable(size, size));
            event.setNewEyeHeight(size * 0.85f + offsetY);

            // 注意：我删除了你原本写在下面重复的 setNewSize 代码，保留上面的逻辑
        }
    }

    //双端物理同步
    // 这个事件会在客户端和服务端双端触发
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 1.20.1 修复：不再区分 PlayerTickEvent.Post 类，而是判断事件的 phase 阶段
        if (event.phase == TickEvent.Phase.END) {

            // 1.20.1 修复：获取玩家直接用 event.player
            Player player = event.player;

            if (GlobalVariables.B_LowGravity) {
                // qiangzhizhanli!!!!
                player.setPose(Pose.STANDING);
                //player.setPose(Pose.SWIMMING);

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
}