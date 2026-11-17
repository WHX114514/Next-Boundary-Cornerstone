package cn.rbq108.nextboundarycornerstone.qa;

import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.nio.file.*;

/** Real dedicated-server floating protection, with an ordinary-gravity negative control. */
@EventBusSubscriber(modid=main.MODID)
public final class DedicatedFlightServerQa {
    private static final boolean ENABLED=Boolean.getBoolean("cornerstone.qa.dedicated");
    private static ServerPlayer player;
    private static int ticks, floatingTicks;
    private static boolean hovered;
    private static long started;

    @SubscribeEvent public static void started(ServerStartedEvent event) {
        if(ENABLED) started=System.currentTimeMillis();
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if(!ENABLED || !(event.getEntity() instanceof ServerPlayer p)) return;
        player=p; p.setGameMode(GameType.SURVIVAL);p.teleportTo(0,150,0);
        p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(main.BASIC_BACKPACK.get()));
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) throws Exception {
        if(!ENABLED) return;
        if(System.currentTimeMillis()-started>150000) finish(false,"timeout");
        if(player==null || player.hasDisconnected()) return;
        ticks++;
        var field=player.connection.getClass().getDeclaredField("clientIsFloating");field.setAccessible(true);
        boolean floating=field.getBoolean(player.connection);
        if(ticks==260) {
            hovered=!floating && !player.server.isFlightAllowed() && !player.mayFly()
                    && !player.getAbilities().flying && player.isNoGravity();
            player.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);
        }
        if(ticks>280 && floating) floatingTicks++;
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) throws Exception {
        if(ENABLED && event.getEntity()==player) finish(hovered && floatingTicks>60,
                "hover260="+hovered+" ordinaryFloatingTicks="+floatingTicks+" allowFlight="+player.server.isFlightAllowed());
    }
    private static void finish(boolean pass,String detail) throws Exception {
        Files.writeString(Path.of("../dedicated-server-report.txt"),(pass?"PASS ":"FAIL ")+detail+"\n");
        var server=net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        server.halt(false);
    }
}
