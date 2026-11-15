package cn.rbq108.nextboundarycornerstone.ServeMiao.communication;

/** Loaded only for packets received on the client. */
final class ClientPackets {
    static net.minecraft.world.entity.player.Player player() { return net.minecraft.client.Minecraft.getInstance().player; }
}
