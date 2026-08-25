package cn.rbq108.nextboundarycornerstone.client;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class CartridgeCaseStartingPoint {

    // 抛壳口相对于玩家【本地坐标系】的偏移
    // X = 向左, Y = 向上, Z = 向前
    public static final float LOCAL_RIGHT   =  -0.3f;  // 枪在玩家右侧
    public static final float LOCAL_UP      =  0.4f;  // 略高于玩家中心
    public static final float LOCAL_FORWARD =  0.8f;  // 与玩家持枪深度对齐

    /**
     * 将本地偏移量通过 currentQuat 旋转到世界空间，返回世界空间的偏移向量。
     * 调用者再加上 player.position() 即可得到最终世界坐标。
     *
     * @param pivotHeightAboveFeet 旋转支点距玩家脚底的高度（就可以确定身体中心啦~）
     */
    public static Vector3f getWorldSpaceOffset(float pivotHeightAboveFeet) {
        // 本地偏移向量
        Vector3f localOffset = new Vector3f(LOCAL_RIGHT, LOCAL_UP, LOCAL_FORWARD);

        // 用 currentQuat 把本地向量旋转到世界空间
        new Quaternionf(GlobalVariables.currentQuat).transform(localOffset);

        // 旋转完的向量就是世界空间偏移，加上旋转中心的高度偏置
        localOffset.y += pivotHeightAboveFeet;

        return localOffset; // 此时是相对于 player.position() 的世界空间偏移
    }
}