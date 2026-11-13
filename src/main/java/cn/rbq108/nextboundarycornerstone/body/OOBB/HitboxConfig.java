package cn.rbq108.nextboundarycornerstone.body.OOBB;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Registered as SERVER config; NeoForge supplies the connected server's file to clients. */
public final class HitboxConfig {
    public static final String FILE_NAME = "next-boundary-hitbox-server.toml";
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<HitboxMode> MODE;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("Zero_Gravity_Hitboxes");
        MODE = builder.comment("失重受击盒模式：AABB 为分段轴对齐包络；OBB 为随骨架旋转的精确盒。",
                        "联机以服务器此文件为准，客户端自动接收；修改后重启服务器并重新连接。",
                        "仅影响受击和选取，不改变方块移动碰撞。")
                .defineEnum("Mode", HitboxMode.AABB);
        builder.pop();
        SPEC = builder.build();
    }

    private HitboxConfig() {}

    public static HitboxMode mode() {
        return SPEC.isLoaded() ? MODE.get() : HitboxMode.AABB;
    }
}
