package cn.rbq108.nextboundarycornerstone.VariableLibrary;

import org.joml.Quaternionf;

public class GlobalVariables {
    //摘自 FullyVariableDocument.rbq
    // 三轴角度
    public static double B_Dx = 0.0; // Pitch 俯仰角
    public static double B_Dy = 0.0; // Yaw 偏航角
    public static double B_Dz = 0.0; // Roll 滚转角

    // 渲染平滑插值用的历史状态
    public static double prev_B_Dx = 0.0;
    public static double prev_B_Dy = 0.0;
    public static double prev_B_Dz = 0.0;

    // 操纵相关 (核心状态位，千万别移走喵！)
    public static boolean B_LowGravity = false; // 当前是否处于无重力操作状态
    public static float B_HitboxSize = 1.2f;//碰撞箱尺寸！

    // 矩阵转换代码
    public static Quaternionf currentQuat = new Quaternionf();
    public static Quaternionf prevQuat = new Quaternionf();

    // 当前的实时旋转速度（这个是由系统自动算的，不要手动改！）
    public static float currentRollVelocity = 0.0f;

    //飞行状态与输入
    public static boolean B_rush = false; // 玩家当前是否按住了冲刺键

    // 玩家的三轴按键输入状态 (-1, 0, 1)
    public static int B_INx = 0; // 左右 (A/D)
    public static int B_INy = 0; // 上下 (Ctrl/Space)
    public static int B_INz = 0; // 前后 (S/W)


    // 物理计算黑盒缓存区
    // 最终算出的三轴目标速度（玩家本地参考系）
    public static float B_Vx3 = 0.0f;
    public static float B_Vy3 = 0.0f;
    public static float B_Vz3 = 0.0f;

    //一：三轴真目标速度（玩家参考系，吃满所有倍率后的推力期望）
    public static float B_Vx3_1 = 0.0f;
    public static float B_Vy3_1 = 0.0f;
    public static float B_Vz3_1 = 0.0f;

    //二：三轴目标速度（地面参考系，经过四元数空间扭曲后的推力方向）
    public static float B_Vx4 = 0.0f;
    public static float B_Vy4 = 0.0f;
    public static float B_Vz4 = 0.0f;

    //三：三轴真实速度（地面参考系，玩家正在维持的真实物理惯性）
    public static float B_Vx1 = 0.0f;
    public static float B_Vy1 = 0.0f;
    public static float B_Vz1 = 0.0f;


    // 用于实现丝滑 FOV 切换

    public static float prevFovModifier = 0.0f;    // 上一逻辑帧的旧值
    public static float currentFovModifier = 0.0f; // 当前逻辑帧的新值


    // 🩺 飞船机身独立控制变量

    public static float B_bodyx = 0.0f; // 身体的 Pitch (俯仰)
    public static float B_bodyy = 0.0f; // 身体的 Yaw (偏航)
    public static float B_bodyz = 0.0f; // 身体的 Roll (滚转)


    // Mixin 底层通信变量

    public static boolean isPlayerRendering = false; // 是否正在渲染玩家
    public static Object playerHead = null;          // 抓取原版的头骨对象
    public static Object playerHat = null;           // 抓取原版的头盔对象
    public static org.joml.Quaternionf headFixQuat = new org.joml.Quaternionf(); // 抵消矩阵

    // 自由视角 (Free Look) 状态位
    public static boolean B_FreeCameraActive = false;
    public static boolean wasFreeCamera = false;

    // 自由视角下头部的平滑相对旋转四元数
    public static Quaternionf headRelQuat = new Quaternionf();
    public static Quaternionf prevHeadRelQuat = new Quaternionf();

    // 自由视角下累积的相对角度 (以机体为基准)
    public static float B_freeLookYaw = 0.0f;
    public static float B_freeLookPitch = 0.0f;
    public static float B_freeLookRoll = 0.0f;

    /**
     * 获取玩家机体物理朝向四元数。
     * 当处于自由视角时，机体朝向与相机四元数分离，由已冻结的 B_Dx, B_Dy, B_Dz 计算生成；
     * 正常状态下，直接使用视角的 currentQuat。
     */
    public static Quaternionf getBodyQuat() {
        if (B_LowGravity) {
            return new Quaternionf().rotationYXZ(
                    (float) Math.toRadians(-B_Dy),
                    (float) Math.toRadians(B_Dx),
                    (float) Math.toRadians(B_Dz)
            );
        }
        return currentQuat;
    }


    public static boolean B_CanBackpackGrantGravity = true;//负责控制“重力切换”这个功能是由本模组负责还是其他依依妖妖的附属模组负责，等加入附属模组的时候要把这个量设为false

    // 在
    //在（？）
    public static boolean prevLowGravity = false; // 用来记录上一帧是不是低重力喵

    // 鼠标原始输入量（每 tick 更新，由 EntityMixin 的 turn() 拦截写入）
    // 用于在失重模式下驱动手臂摆动，取代有万向节死锁隐患的欧拉角差值法
    public static float mouseDeltaX = 0.0f; // 鼠标纵向增量（控制 Pitch，正数=往下看）
    public static float mouseDeltaY = 0.0f; // 鼠标横向增量（控制 Yaw，正数=往右转）

}