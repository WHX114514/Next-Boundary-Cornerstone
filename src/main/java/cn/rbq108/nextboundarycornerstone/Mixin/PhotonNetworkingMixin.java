package cn.rbq108.nextboundarycornerstone.Mixin;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.lowdragmc.photon.PhotonNetworking", remap = false)
public abstract class PhotonNetworkingMixin {
    // Photon 2.2.8 marks its command packets as mandatory. Local playback uses none of them.
    @Redirect(method = "registerPayloads", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/network/event/RegisterPayloadHandlersEvent;registrar(Ljava/lang/String;)Lnet/neoforged/neoforge/network/registration/PayloadRegistrar;"))
    private static PayloadRegistrar cornerstone$allowClientOnlyPhoton(RegisterPayloadHandlersEvent event, String version) {
        return event.registrar(version).optional();
    }
}
