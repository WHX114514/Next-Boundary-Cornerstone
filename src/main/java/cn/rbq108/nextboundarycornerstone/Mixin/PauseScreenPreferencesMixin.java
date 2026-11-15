package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.client.preferences.ModSettingsScreen;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Add the settings row before vanilla arranges and positions the entire menu. */
@Mixin(PauseScreen.class)
public abstract class PauseScreenPreferencesMixin {
    @WrapOperation(method = "createPauseMenu", at = @At(value = "INVOKE", ordinal = 1,
            target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;I)Lnet/minecraft/client/gui/layouts/LayoutElement;"))
    private LayoutElement nextboundary$preferencesRow(GridLayout.RowHelper rows, LayoutElement quit, int columns,
                                                       Operation<LayoutElement> original) {
        var screen = (PauseScreen) (Object) this;
        rows.addChild(Button.builder(Component.translatable("ui.next_boundary_cornerstone.settings.button"),
                button -> Minecraft.getInstance().setScreen(new ModSettingsScreen(screen)))
                .width(quit.getWidth()).build(), columns);
        return original.call(rows, quit, columns);
    }
}
