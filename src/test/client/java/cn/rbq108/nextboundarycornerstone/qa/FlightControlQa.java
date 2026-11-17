package cn.rbq108.nextboundarycornerstone.qa;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import cn.rbq108.nextboundarycornerstone.core.Keybinds;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Deterministic standalone flight trace, compared with the unmodified checkout. */
final class FlightControlQa {
    private final List<List<Double>> trace = new ArrayList<>();
    private int frame;
    private Vec3 previous;

    boolean tick() throws Exception {
        var mc=Minecraft.getInstance();
        // Game-internal focus flag only; GLFW visibility/focus assertions remain active.
        mc.setWindowActive(true);
        if (frame==0) {
            GlobalVariables.currentQuat.identity();GlobalVariables.prevQuat.identity();
            GlobalVariables.B_Vx1=GlobalVariables.B_Vy1=GlobalVariables.B_Vz1=0;
            mc.player.setDeltaMovement(Vec3.ZERO);
            previous=mc.player.position();
        } else {
            var delta=mc.player.position().subtract(previous);previous=mc.player.position();
            trace.add(List.of((double)frame,delta.x,delta.y,delta.z,(double)GlobalVariables.B_Vx1,
                    (double)GlobalVariables.B_Vy1,(double)GlobalVariables.B_Vz1,(double)mc.player.getFieldOfViewModifier()));
        }
        Keybinds.B_FORWARD.setDown(frame<25);
        Keybinds.B_UP.setDown(frame>=25 && frame<50);
        Keybinds.B_RIGHT.setDown(frame>=50 && frame<75);
        Keybinds.B_MANUAL_BRAKE.setDown(frame>=100 && frame<125);
        Keybinds.B_ROLL_RIGHT.setDown(frame>=125 && frame<150);
        if (++frame<=175) return false;
        Keybinds.B_FORWARD.setDown(false);Keybinds.B_UP.setDown(false);Keybinds.B_RIGHT.setDown(false);
        Keybinds.B_MANUAL_BRAKE.setDown(false);Keybinds.B_ROLL_RIGHT.setDown(false);
        mc.setWindowActive(false);
        Files.writeString(Path.of("../standalone-flight-trace.json"),new com.google.gson.Gson().toJson(trace));
        return true;
    }
}
