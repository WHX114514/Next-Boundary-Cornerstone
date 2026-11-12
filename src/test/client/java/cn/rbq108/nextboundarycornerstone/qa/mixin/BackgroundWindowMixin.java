package cn.rbq108.nextboundarycornerstone.qa.mixin;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The QA run disables the early splash first, so these hints precede the first real window. */
@Mixin(Window.class)
public abstract class BackgroundWindowMixin {
    @Inject(method = "<init>", at = @At(value = "INVOKE", target =
            "Lnet/neoforged/fml/loading/ImmediateWindowHandler;setupMinecraftWindow(Ljava/util/function/IntSupplier;Ljava/util/function/IntSupplier;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)J",
            remap = false))
    private void surfaceqa$backgroundWindow(CallbackInfo ci) {
        if (Boolean.getBoolean("cornerstone.hitbox.visual")
                || Boolean.getBoolean("cornerstone.surface.qa") && !Boolean.getBoolean("cornerstone.surface.physical")) {
            GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        }
    }
}
