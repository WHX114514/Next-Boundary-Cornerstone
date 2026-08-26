package cn.rbq108.nextboundarycornerstone.ServeMiao;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;




@EventBusSubscriber(modid = main.MODID)
public class AllowFly {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

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

        // 核心同步：如果当前是在服务端，且重力控制权在核心前置手中，主动通过 setNoGravity 维持玩家重力状态
        if (!player.level().isClientSide) {
            if (GlobalVariables.B_CanBackpackGrantGravity) {
                boolean isWearingBackpack = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST)
                        .is(cn.rbq108.nextboundarycornerstone.main.BASIC_BACKPACK.get());
                boolean isInSpace = player.level().dimension().location().getPath().equals("space");
                boolean shouldBeZeroG = cn.rbq108.nextboundarycornerstone.VariableLibrary.debug.FORCE_LOW_GRAVITY || isWearingBackpack || isInSpace;
                if (player.isNoGravity() != shouldBeZeroG) {
                    player.setNoGravity(shouldBeZeroG);
                }
            }
        }

        // 默认游戏模式
        GameType gameMode = GameType.SURVIVAL;
        boolean modDeterminedMayFly;

        if (player.isCreative() || player.isSpectator()) {
            modDeterminedMayFly = true;
        } else {
            modDeterminedMayFly = player.isNoGravity();
        }
        if (player.getAbilities().mayfly != modDeterminedMayFly) {
            player.getAbilities().mayfly = modDeterminedMayFly;

            if (!modDeterminedMayFly && !player.isCreative() && !player.isSpectator()) {
                player.getAbilities().flying = false;
            }

            // 同步给客户端
            player.onUpdateAbilities();
        }
        /*GameType gameMode = GameType.SURVIVAL;
        // 逻辑：(开启了低重力) 或者 (是创造模式) 或者 (是旁观者模式) → 这三种情况都必须能飞
        boolean targetMayFly = GlobalVariables.B_LowGravity || player.isCreative() || player.isSpectator();

        if (player.getAbilities().mayfly != targetMayFly) {
            player.getAbilities().mayfly = targetMayFly;
            if (!targetMayFly) {
                player.getAbilities().flying = false;
            }

            player.onUpdateAbilities();
        }*/
        /*
        boolean canFly = GlobalVariables.B_LowGravity || player.isCreative() || player.isSpectator();

        if (player.getAbilities().mayfly != canFly) {
            player.getAbilities().mayfly = canFly;
            // 如果失去了飞行权限，强制停止飞行状态，防止生存模式卡在半空
            if (!canFly) player.getAbilities().flying = false;

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
            //player.getAbilities().flying = false;//false;刚注释掉的，我发现这行启用创造就会飞不了
            player.getAbilities().mayfly = true;//false;

            //System.out.println("设置后(调用前)1: " + player.getAbilities().mayfly);
            //可能是时序问题？注释掉这两个println后就没用了，必须加一个sleep才能用，或者重新把这个println弄回来？
            //try { Thread.sleep(1); } catch (InterruptedException e) {}
            player.onUpdateAbilities();
            //System.out.println("调用后2: " + player.getAbilities().mayfly);

        }*/


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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;


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