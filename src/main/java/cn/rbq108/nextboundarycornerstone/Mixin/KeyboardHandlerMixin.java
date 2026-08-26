package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void doABarrelRoll$interceptKeys(long window, int key, int scancode, int action, int mods, CallbackInfo ci) {
        var mc = Minecraft.getInstance();

        // 只有无重力、没开菜单、且不是松开按键时拦截
        if (mc.player != null && mc.player.isNoGravity() && mc.screen == null && action != GLFW.GLFW_RELEASE) {

            boolean isShiftDown = (mods & GLFW.GLFW_MOD_SHIFT) != 0;

            // 拦截背包键 (E) 和 丢弃键 (Q)
            boolean isInventoryKey = mc.options.keyInventory.matches(key, scancode);
            boolean isDropKey = mc.options.keyDrop.matches(key, scancode);

            if ((isInventoryKey || isDropKey) && !isShiftDown) {
                // 核心：在 HEAD 处直接取消原版方法的执行！
                ci.cancel();
            }
        }
    }
}