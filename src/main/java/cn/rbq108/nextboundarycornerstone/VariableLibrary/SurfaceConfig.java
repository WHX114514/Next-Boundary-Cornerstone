package cn.rbq108.nextboundarycornerstone.VariableLibrary;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/** SERVER config: NeoForge synchronizes these values to clients when they join. */
public final class SurfaceConfig {
    public static final ModConfigSpec SPEC;
    public static final Settings SETTINGS;

    static {
        Pair<Settings, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(Settings::new);
        SETTINGS = pair.getLeft();
        SPEC = pair.getRight();
    }

    private SurfaceConfig() {}

    public static final class Settings {
        public final ModConfigSpec.BooleanValue enabled;
        public final ModConfigSpec.DoubleValue strengthRatio;
        public final ModConfigSpec.DoubleValue normalDamping;
        public final ModConfigSpec.IntValue stopTicks;

        private Settings(ModConfigSpec.Builder builder) {
            builder.push("Surface_Assistance");
            enabled = builder.comment("失重时接触静态方块后自动启用表面辅助。")
                    .define("Enabled", true);
            strengthRatio = builder.comment("普通吸附加速度占当前基础推进加速度上限的比例。")
                    .defineInRange("StrengthRatio", 0.4, 0.0, 1.0);
            normalDamping = builder.comment("每 tick 对离壁速度的阻尼系数；总阻力仍受推进能力限幅。")
                    .defineInRange("NormalDamping", 0.3, 0.0, 1.0);
            stopTicks = builder.comment("普通贴壁松键停车时间（tick，标准速度下的近似值）。")
                    .defineInRange("StopTicks", 6, 1, 100);
            builder.pop();
        }
    }
}
