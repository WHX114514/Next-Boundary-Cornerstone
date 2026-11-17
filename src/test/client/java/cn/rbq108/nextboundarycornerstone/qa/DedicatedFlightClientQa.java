package cn.rbq108.nextboundarycornerstone.qa;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.EventPriority;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;

/** Background TCP client: hover with zero gravity, then exercise the native kick. */
final class DedicatedFlightClientQa {
    private final long started=System.currentTimeMillis();
    private int phase,ticks, heartbeat;
    DedicatedFlightClientQa(){NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,this::tick);}
    private void tick(ClientTickEvent.Post event) {
        if(phase==99)return;
        var mc=Minecraft.getInstance();
        try {
            if(System.currentTimeMillis()-started>120000)throw new AssertionError("TCP client timeout");
            long window=mc.getWindow().getWindow();
            if(GLFW.glfwGetWindowAttrib(window,GLFW.GLFW_VISIBLE)!=0 || GLFW.glfwGetWindowAttrib(window,GLFW.GLFW_FOCUSED)!=0)
                throw new AssertionError("window visible or focused");
            mc.options.pauseOnLostFocus=false;
            if(++heartbeat%100==0)com.mojang.logging.LogUtils.getLogger().info("TCP_QA_WAIT phase={} ticks={} screen={}",phase,ticks,mc.screen==null?"null":mc.screen.getClass().getName());
            if(mc.screen!=null && mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen"))mc.setScreen(new TitleScreen());
            if(phase==0 && mc.screen instanceof TitleScreen && mc.getOverlay()==null) {
                ConnectScreen.startConnecting(mc.screen,mc,ServerAddress.parseString("127.0.0.1:25579"),
                        new ServerData("QA","127.0.0.1:25579",ServerData.Type.OTHER),false,null);
                phase=1;
            } else if(mc.player!=null && mc.screen==null) {
                phase=2;ticks++;
                // Deliberately hover after the backpack is removed, to verify native anti-flight still rejects it.
                mc.player.setDeltaMovement(Vec3.ZERO);
            } else if(phase==2 && mc.player==null) {
                var report=Path.of("../../dedicated-server-report.txt");
                if(!Files.exists(report))return;
                var result=Files.readString(report);
                if(!result.startsWith("PASS ") || ticks<300)throw new AssertionError(result+" ticks="+ticks);
                Files.writeString(Path.of("../client-report.txt"),"PASS dedicated TCP "+result.trim()+" hidden=true focused=false\n");
                phase=99;mc.stop();
            }
        } catch(Throwable failure) {
            try {Files.writeString(Path.of("../client-report.txt"),"FAIL "+failure);}catch(Exception ignored){}
            com.mojang.logging.LogUtils.getLogger().error("DEDICATED_QA_FAILED",failure);phase=99;mc.stop();
        }
    }
}
