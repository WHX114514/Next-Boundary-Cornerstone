//显然，这个已经被废弃，当个坟墓做纪念叭
//欧拉角做的无限制视角旋转真诡异啊

package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;



@Mixin(MouseHandler.class)
public class MouseHandlerMixinEliminated {

    @ModifyArgs(
            method = "turnPlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V")
    )
    private void doABarrelRoll$rotateMouseInputs(Args args) {
        // 当启用无重力操作时，拦截并旋转鼠标输入
        var player = Minecraft.getInstance().player;
        if (player != null && player.isNoGravity()) {
            double originalYRot = args.get(0);
            double originalXRot = args.get(1);

                float pitch = player.getXRot() % 360.0f;
                if (pitch > 180.0f) pitch -= 360.0f;
                if (pitch < -180.0f) pitch += 360.0f;


                if (Math.abs(pitch) > 90.0f) {
                    originalYRot = -originalYRot;
                }


            double rollRad = Math.toRadians(GlobalVariables.B_Dz);
            double cos = Math.cos(rollRad);
            double sin = Math.sin(rollRad);


            double newYRot = originalYRot * cos - originalXRot * sin;
            double newXRot = originalXRot * cos + originalYRot * sin;

            args.set(0, newYRot);
            args.set(1, newXRot);
        }
    }
}