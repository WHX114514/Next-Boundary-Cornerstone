package cn.rbq108.nextboundarycornerstone.qa.mixin;

import com.mojang.blaze3d.platform.GLX;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keep macOS GLFW initialization from promoting this hidden QA process to a UI app. */
@Mixin(GLX.class)
public abstract class BackgroundGlfwMixin {
    @Redirect(method="_initGlfw", at=@At(value="INVOKE", target="Lorg/lwjgl/glfw/GLFW;glfwInit()Z", remap=false))
    private static boolean backgroundInit() {
        GLFW.glfwInitHint(GLFW.GLFW_COCOA_MENUBAR, GLFW.GLFW_FALSE);
        return GLFW.glfwInit();
    }
}
