package cn.rbq108.nextboundarycornerstone.ServeMiao.communication;

import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record SyncConfigPayload(boolean restoreVanillaSpectator) {


    // 序列化与反序列化

    public static SyncConfigPayload read(FriendlyByteBuf buf) {
        return new SyncConfigPayload(
                buf.readBoolean()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(restoreVanillaSpectator);
    }


}
