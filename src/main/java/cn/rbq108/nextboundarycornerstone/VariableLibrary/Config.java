//用来读取游戏根目录config文件夹下的 next-boundary.toml
package cn.rbq108.nextboundarycornerstone.VariableLibrary;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class Config {

    public static final ModConfigSpec SPEC;
    public static final PhysicsSettings PHYSICS;

    static {
        final Pair<PhysicsSettings, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(PhysicsSettings::new);
        SPEC = specPair.getRight();
        PHYSICS = specPair.getLeft();
    }


    // 字面意思啊config读取部分

    public static class PhysicsSettings {

        public final ModConfigSpec.DoubleValue rollSpeed;
        public final ModConfigSpec.DoubleValue rollSmoothing;
        public final ModConfigSpec.DoubleValue vMax;
        public final ModConfigSpec.DoubleValue rushXy;
        public final ModConfigSpec.DoubleValue rushZ;
        public final ModConfigSpec.DoubleValue afterburnerRatio;
        public final ModConfigSpec.DoubleValue aMax;
        public final ModConfigSpec.DoubleValue brakeRatio;
        public final ModConfigSpec.DoubleValue crosshairScaleFactor;

        public PhysicsSettings(ModConfigSpec.Builder builder) {
            builder.push("Flight_Dynamics");

            rollSpeed = builder
                    .comment("Roll轴旋转倍率 - 默认: 10.0")
                    .defineInRange("RollSpeed", 10.0, 0.0, 100.0);

            rollSmoothing = builder
                    .comment("Roll轴平滑因子 (越小越有惯性) - 默认: 0.75")
                    .defineInRange("RollSmoothing", 0.75, 0.0, 1.0);

            vMax = builder
                    .comment("基础最大非冲刺速度 (Vmax) - 默认: 0.15")
                    .defineInRange("Vmax", 0.15, 0.0, 10.0);

            rushXy = builder
                    .comment("冲刺时，上下左右的辅助喷射倍率 - 默认: 1.5")
                    .defineInRange("RushXY", 1.5, 0.0, 10.0);

            rushZ = builder
                    .comment("冲刺时，主推进器（前进）的喷射倍率 - 默认: 2.0")
                    .defineInRange("RushZ", 2.0, 0.0, 10.0);

            afterburnerRatio = builder
                    .comment("加力燃烧室推力倍率 - 默认: 2.0")
                    .defineInRange("AfterburnerRatio", 2.0, 0.0, 10.0);

            aMax = builder
                    .comment("基础最大加速度 (Amax) - 默认: 0.028")
                    .defineInRange("Amax", 0.028, 0.0, 1.0);

            brakeRatio = builder
                    .comment("自动制动/减速倍率 - 默认: 1.2")
                    .defineInRange("BrakeRatio", 1.2, 0.0, 20.0);

            crosshairScaleFactor = builder
                    .comment("第一人称自由视角准星物理投影微调系数 - 默认: 1.0 (如果觉得转头时准星有点过头或没到位，可微调此值，如 0.95 或 1.05)")
                    .defineInRange("CrosshairScaleFactor", 1.0, 0.1, 10.0);

            builder.pop();
        }
    }
}