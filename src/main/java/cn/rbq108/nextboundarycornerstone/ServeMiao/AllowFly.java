package cn.rbq108.nextboundarycornerstone.ServeMiao;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.common.EventBusSubscriber;
//import net.minecraftforge.common.event.tick.PlayerTickEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = main.MODID)
public class AllowFly {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;

        /*不是这个if在这有任何卵用吗？
        if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }*//*else{
            player.getAbilities().mayfly = false;
            player.onUpdateAbilities();
        }*/
        /*GameType gameMode = null;
        if (Minecraft.getInstance().gameMode != null) {
            gameMode = Minecraft.getInstance().gameMode.getPlayerMode();
        }

        Caused by: java.lang.RuntimeException: Attempted to load class net/minecraft/client/Minecraft for invalid dist DEDICATED_SERVER
        显然，这坨东西在服务端会崩溃
        */
        //我写的这是啥？


        GameType gameMode = GameType.SURVIVAL;
        // 1. 确定最终权限：开启低重力 OR 创造模式 OR 旁观模式
        boolean canFly = GlobalVariables.B_LowGravity || player.isCreative() || player.isSpectator();

// 2. 【关键】状态差异检查 (Dirty Check)
// 只有当当前权限和目标不一致时才执行，解决时序问题和网络包冲突
        if (player.getAbilities().mayfly != canFly) {
            player.getAbilities().mayfly = canFly;

            // 如果目标是关闭飞行权限（生存模式且关了重力），强制停止当前的飞行状态
            if (!canFly) {
                player.getAbilities().flying = false;
            }

            // 3. 同步能力修改
            player.onUpdateAbilities();
        }/*
        if (player instanceof ServerPlayer serverPlayer) {
            gameMode = serverPlayer.gameMode.getGameModeForPlayer();
        } else if (player.level().isClientSide) {

        }
        if (GlobalVariables.B_LowGravity) {
            player.getAbilities().flying = true;
            player.getAbilities().mayfly = true;//zheli!

            player.onUpdateAbilities();
        } else if (GlobalVariables.B_LowGravity == false && (gameMode == GameType.SURVIVAL || gameMode == GameType.ADVENTURE)) {
            player.getAbilities().flying = false;
            player.getAbilities().mayfly = false;

            //System.out.println("设置后(调用前)1: " + player.getAbilities().mayfly);
            //可能是时序问题？注释掉这两个println后就没用了，必须加一个sleep才能用，或者重新把这个println弄回来？
            //try { Thread.sleep(1); } catch (InterruptedException e) {}
            player.onUpdateAbilities();
            //System.out.println("调用后2: " + player.getAbilities().mayfly);

        }


        /*
        if (GlobalVariables.B_LowGravity) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
        } else {
            if (!player.isCreative() && !player.isSpectator() && player.getAbilities().mayfly) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }*/
    }
}


/*public class AllowFly {

    /*
     *
     *
     *
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        Player player = event.getEntity();


        if (GlobalVariables.B_LowGravity) {

            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;


                player.onUpdateAbilities();
            }
        }

        else {
            // 创造模式和旁观模式不归咱们管喵
            if (!player.isCreative() && !player.isSpectator()) {
                if (player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false; // 关掉开关后强制落地姿态
                    player.onUpdateAbilities();
                }
            }
        }
    }
}*/

//下面这一坨tm是啥啊
/*

 *
package cn.rbq108.test.ServeMiao;

import cn.rbq108.test.main;
import cn.rbq108.test.VariableLibrary.GlobalVariables;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.EventBusSubscriber;
import net.minecraftforge.common.event.tick.PlayerTickEvent;


@EventBusSubscriber(modid = main.MODID)
public class AllowFly {

    /**

     * 只要玩家的 mayfly 属性为 true，服务器的anti-fly就不执行
     *
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        Player player = event.getEntity();


        if (GlobalVariables.B_LowGravity) {


            if (!player.getAbilities().mayfly) {

                player.getAbilities().mayfly = true;


                player.onUpdateAbilities();
            }

        } else {


            if (!player.isCreative() && !player.isSpectator() && player.getAbilities().mayfly) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false; // 落地时停止飞行姿态
                player.onUpdateAbilities();
            }
        }
    }
}*/