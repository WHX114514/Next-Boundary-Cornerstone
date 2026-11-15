package cn.rbq108.nextboundarycornerstone.Mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(method = "keyPress", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/KeyMapping;click(Lcom/mojang/blaze3d/platform/InputConstants$Key;)V",
            shift = At.Shift.AFTER))
    private void doABarrelRoll$interceptKeys(long window, int key, int scancode, int action, int mods, CallbackInfo ci) {
        var mc = Minecraft.getInstance();

        // 保留正常的按下/松开事件，只吃掉原版动作的点击；Tab 组合键由 KeyInputHandler 处理。
        if (mc.player != null && mc.player.isNoGravity() && mc.screen == null) {
            if (mc.options.keyInventory.matches(key, scancode)) {
                while (mc.options.keyInventory.consumeClick()) {}
            }
            if (mc.options.keyDrop.matches(key, scancode)) {
                while (mc.options.keyDrop.consumeClick()) {}
            }
        }
    }
}
