package cn.rbq108.nextboundarycornerstone.VariableLibrary;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class Config {

    public static final ForgeConfigSpec SPEC;
    public static final PhysicsSettings PHYSICS;

    static {
        // 使用 Forge 官方推荐的 Pair 写法，安全可靠
        final Pair<PhysicsSettings, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(PhysicsSettings::new);
        SPEC = specPair.getRight();
        PHYSICS = specPair.getLeft();
    }

    public static class PhysicsSettings {

        public final ForgeConfigSpec.DoubleValue rollSpeed;
        public final ForgeConfigSpec.DoubleValue rollSmoothing;
        public final ForgeConfigSpec.DoubleValue vMax;
        public final ForgeConfigSpec.DoubleValue rushXy;
        public final ForgeConfigSpec.DoubleValue rushZ;
        public final ForgeConfigSpec.DoubleValue afterburnerRatio;
        public final ForgeConfigSpec.DoubleValue aMax;
        public final ForgeConfigSpec.DoubleValue brakeRatio;

        public PhysicsSettings(ForgeConfigSpec.Builder builder) {
            builder.push("Flight_Dynamics");

            rollSpeed = builder.comment("Roll轴旋转倍率 - 默认: 10.0").defineInRange("RollSpeed", 10.0, 0.0, 100.0);
            rollSmoothing = builder.comment("Roll轴平滑因子 (越小越有惯性) - 默认: 0.75").defineInRange("RollSmoothing", 0.75, 0.0, 1.0);
            vMax = builder.comment("基础最大非冲刺速度 (Vmax) - 默认: 0.15").defineInRange("Vmax", 0.15, 0.0, 10.0);
            rushXy = builder.comment("冲刺时，上下左右的辅助喷射倍率 - 默认: 1.5").defineInRange("RushXY", 1.5, 0.0, 10.0);
            rushZ = builder.comment("冲刺时，主推进器（前进）的喷射倍率 - 默认: 2.0").defineInRange("RushZ", 2.0, 0.0, 10.0);
            afterburnerRatio = builder.comment("加力燃烧室推力倍率...").defineInRange("AfterburnerRatio", 2.0, 0.0, 10.0);
            aMax = builder.comment("基础加速度 (Amax) - 默认: 0.05").defineInRange("Amax", 0.05, 0.0, 10.0);
            brakeRatio = builder.comment("制动倍率 - 默认: 0.9").defineInRange("BrakeRatio", 0.9, 0.0, 1.0);

            builder.pop();
        }
    }
}