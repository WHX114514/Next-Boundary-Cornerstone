package cn.rbq108.nextboundarycornerstone.api;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import org.joml.Quaternionf;

/**
 * Next Boundary Cornerstone - 自由视角外部 API 接口
 * 
 * 供其他模组或渲染辅助插件调用，用以获取自由视角与头部固定的运行状态及相对空间角度。
 */
public class FreeLookAPI {

    /**
     * 获取当前是否处于自由视角模式（包含长按C与Shift锁定状态）
     * 
     * @return true 代表视角与身体已经解耦，物理推进方向对齐机体而非视角
     */
    public static boolean isFreeLookActive() {
        return GlobalVariables.B_FreeCameraActive;
    }

    /**
     * 获取当前是否处于固定头部视角锁定状态（即释放了按键仍保持自由视角）
     * 
     * @return true 代表头部朝向已锁死，鼠标滚轮可控制相机 Roll 轴自由倾斜
     */
    public static boolean isFixedCameraLocked() {
        return GlobalVariables.B_FreeCameraToggle && GlobalVariables.B_HeadRotationLocked;
    }

    /**
     * 获取相机相对于身体物理朝向的相对偏航角（Yaw，单位：度）
     * 
     * @return 相对 Yaw 角度值，通常在自由视角激活时有效
     */
    public static float getCameraRelativeYaw() {
        return GlobalVariables.B_freeLookYaw;
    }

    /**
     * 获取相机相对于身体物理朝向的相对俯仰角（Pitch，单位：度）
     * 
     * @return 相对 Pitch 角度值，通常在自由视角激活时有效
     */
    public static float getCameraRelativePitch() {
        return GlobalVariables.B_freeLookPitch;
    }

    /**
     * 获取相机相对于身体物理朝向的相对桶滚角（Roll，单位：度）
     * 
     * @return 相对 Roll 角度值，通常在自由视角激活时有效
     */
    public static float getCameraRelativeRoll() {
        return GlobalVariables.B_freeLookRoll;
    }

    /**
     * 获取当前渲染帧头部模型相对于身体的球面平滑插值旋转四元数
     * 
     * @return 相对头部旋转的 Quaternionf 拷贝（只读副本）
     */
    public static Quaternionf getHeadRelativeRotation() {
        return new Quaternionf(GlobalVariables.headRelQuat);
    }

    /**
     * 获取锁定状态下头部固定那一瞬间的相对旋转四元数
     * 
     * @return 锁定时刻的 Quaternionf 拷贝（只读副本）
     */
    public static Quaternionf getLockedHeadRelativeRotation() {
        return new Quaternionf(GlobalVariables.lockedHeadRelQuat);
    }
}
