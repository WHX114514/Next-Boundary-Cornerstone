package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.core.Keybinds;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public abstract class KeyMappingOrderMixin {
    @Inject(method = "compareTo(Lnet/minecraft/client/KeyMapping;)I", at = @At("HEAD"), cancellable = true)
    private void cornerstone$flightKeyOrder(KeyMapping other, CallbackInfoReturnable<Integer> result) {
        var self = (KeyMapping) (Object) this;
        if (Keybinds.KEY_CATEGORY.equals(self.getCategory()) && Keybinds.KEY_CATEGORY.equals(other.getCategory())) {
            int order = Integer.compare(Keybinds.displayOrder(self), Keybinds.displayOrder(other));
            if (order != 0) result.setReturnValue(order);
        }
    }
}
