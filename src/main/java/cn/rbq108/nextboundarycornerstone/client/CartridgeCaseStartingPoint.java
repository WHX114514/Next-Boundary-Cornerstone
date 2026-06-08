package cn.rbq108.nextboundarycornerstone.client;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class CartridgeCaseStartingPoint {

    // ================= 核心常量配置区 =================

    // 1. 旋转中心位置 (地面系，相对于玩家脚底)
    // 这是你在太空中翻滚时的“核心支点”。
    // 比如：如果你的角色是绕着肚子翻滚的，这里就设为 (0, 0.7f, 0)
    public static final Vector3f PIVOT_CENTER = new Vector3f(0.0f, 0.6f, 0.0f);

    // 2. 抛壳口微调偏移 (玩家系，相对于旋转中心！)
    // 当角色没有任何旋转时，抛壳口【距离旋转中心】的前后左右上下位置。
    // X=右, Y=上, Z=前
    // 比如：旋转中心在肚子(0.7)，抛壳窗在胸口(1.4)，那这里的 Y 就是 0.7 (因为 0.7 + 0.7 = 1.4)
    public static final Vector3f GUN_OFFSET = new Vector3f(0f, 0.7f, 0f);

    // =================================================

    public static Vector3f getThirdPersonOffset(Player player) {

        // ================= 阶段 1：6DoF 纯净物理运算 =================

        // 1. 把真正的本地偏移全加起来
        // 肚脐 (PIVOT) 和枪口 (GUN) 都在玩家身上，必须在旋转前先合并成一根“骨头”
        Vector3f totalLocalOffset = new Vector3f(PIVOT_CENTER).add(GUN_OFFSET);

        // 2. 获取绝对 6DoF 四元数
        Quaternionf spaceRot = new Quaternionf(GlobalVariables.currentQuat);

        // 3. 将这根“骨头”整体丢进 6DoF 空间里旋转
        // 这样一来，平躺时，这 0.6 的高度也会乖乖转到前面去，跟着身体走！
        spaceRot.transform(totalLocalOffset);

        // 这就是绝对完美的 6DoF 空间坐标
        Vector3f true6DoFOffset = totalLocalOffset;

        // ================= 阶段 2：对冲 Mixin 遗留算式 =================

        // 提前减去 Mixin 里硬加的 1.4
        Vector3f compensatedOffset = new Vector3f(
                true6DoFOffset.x,
                true6DoFOffset.y - 1.4f,
                true6DoFOffset.z
        );

        // 抵消 Vanilla yBodyRot 在平躺时由“万向节死锁”引发的疯狂乱转
        float vanillaYaw = player.yBodyRot * (float) (Math.PI / 180.0);
        Quaternionf vanillaRot = new Quaternionf().rotateY(-vanillaYaw);
        Quaternionf inverseVanillaRot = vanillaRot.invert();

        inverseVanillaRot.transform(compensatedOffset);

        return compensatedOffset;
    }
}