/*
这是曾经被寄予厚望的冲击伤害计算代码
但是调试了无数次依然bug满满
无奈放弃

现在只要进入无重力操作就没有摔落伤害

package cn.rbq108.test.event;


import cn.rbq108.test.main;
import cn.rbq108.test.VariableLibrary.GlobalVariables;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.EventBusSubscriber;
import net.minecraftforge.common.event.entity.living.LivingFallEvent;

@EventBusSubscriber(modid = main.MODID)
public class DamageEvents {

    @SubscribeEvent
    public static void onPlayerFall(LivingFallEvent event) {

        if (event.getEntity() instanceof Player player) {





            double realImpactSpeed = Math.abs(GlobalVariables.B_Vy1);

            // 重新设定安全速度阈值

            double safeSpeed = 0.6;

            if (realImpactSpeed <= safeSpeed) {

                event.setCanceled(true);
                event.setDistance(0.0f);
            } else {

                float crashSeverity = (float) (realImpactSpeed - safeSpeed);


                event.setDistance(crashSeverity * 20.0f + 3.0f);
            }
        }
    }
}//*/