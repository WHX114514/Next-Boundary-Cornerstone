package cn.rbq108.nextboundarycornerstone.ServeMiao.communication;

import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncConfigPayload(boolean restoreVanillaSpectator) implements CustomPacketPayload {

    public static final Type<SyncConfigPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(main.MODID, "config_sync"));

    // 序列化与反序列化
    public static final StreamCodec<FriendlyByteBuf, SyncConfigPayload> STREAM_CODEC = StreamCodec.ofMember(
            SyncConfigPayload::write, SyncConfigPayload::read
    );

    public static SyncConfigPayload read(FriendlyByteBuf buf) {
        return new SyncConfigPayload(
                buf.readBoolean()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(restoreVanillaSpectator);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
