package cn.rbq108.nextboundarycornerstone.motion;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import cn.rbq108.nextboundarycornerstone.VariableLibrary.Config;
import cn.rbq108.nextboundarycornerstone.math.CoordinateSystemTransformation;
import org.joml.Vector3f;
import net.minecraft.client.Minecraft;

public class calculate {

    public static void calculateTargetVelocity() {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (!Config.SPEC.isLoaded()) return;

        //碰撞同步
        double ax = mc.player.getX() - mc.player.xo;
        double ay = mc.player.getY() - mc.player.yo;
        double az = mc.player.getZ() - mc.player.zo;
        if (mc.player.horizontalCollision) {
            if (Math.abs(ax) < 0.01) GlobalVariables.B_Vx1 = 0;
            if (Math.abs(az) < 0.01) GlobalVariables.B_Vz1 = 0;
        }
        if (mc.player.verticalCollision && Math.abs(ay) < 0.01) GlobalVariables.B_Vy1 = 0;

        //Config 变量提取
        float cfgVmax = Config.PHYSICS.vMax.get().floatValue();
        float cfgRushXy = Config.PHYSICS.rushXy.get().floatValue();
        float cfgRushZ = Config.PHYSICS.rushZ.get().floatValue();
        float cfgAfterburner = Config.PHYSICS.afterburnerRatio.get().floatValue();
        float cfgAmax = Config.PHYSICS.aMax.get().floatValue();
        float cfgBrake = Config.PHYSICS.brakeRatio.get().floatValue();

        //一：目标推力计算
        int inX = GlobalVariables.B_INx;
        int inY = GlobalVariables.B_INy;
        int inZ = GlobalVariables.B_INz;

        if (GlobalVariables.B_rush && inZ < 0) GlobalVariables.B_rush = false;

        Vector3f localInput = new Vector3f(inX, inY, inZ);
        if (localInput.lengthSquared() > 0) localInput.normalize();

        float curRushX = GlobalVariables.B_rush ? cfgRushXy : 1.0f;
        float curRushY = GlobalVariables.B_rush ? cfgRushXy : 1.0f;
        float curRushZ = GlobalVariables.B_rush ? ((inZ > 0) ? cfgRushZ : cfgRushXy) : 1.0f;

        GlobalVariables.B_Vx3_1 = localInput.x * cfgVmax * curRushX * cfgAfterburner;
        GlobalVariables.B_Vy3_1 = localInput.y * cfgVmax * curRushY * cfgAfterburner;
        GlobalVariables.B_Vz3_1 = localInput.z * cfgVmax * curRushZ * cfgAfterburner;

        CoordinateSystemTransformation.transformVelocityToWorld();

        // 三：平滑制动（我二呢？）
        boolean hasInput = (inX != 0 || inY != 0 || inZ != 0);
        boolean shouldBrake = cn.rbq108.nextboundarycornerstone.VariableLibrary.debug.DEBUG_B_HOLD ||
                cn.rbq108.nextboundarycornerstone.core.Keybinds.B_MANUAL_BRAKE.isDown();

        Vector3f currentVel = new Vector3f(GlobalVariables.B_Vx1, GlobalVariables.B_Vy1, GlobalVariables.B_Vz1);

        if (!hasInput && !shouldBrake) {
            return; // 自由滑行
        }

        if (!hasInput && shouldBrake) {

            float damping = Math.min(cfgBrake * 0.1f, 0.95f);
            currentVel.lerp(new Vector3f(0, 0, 0), damping);
            if (currentVel.length() < 0.005f) currentVel.set(0, 0, 0);
        } else {

            Vector3f targetVel = new Vector3f(GlobalVariables.B_Vx4, GlobalVariables.B_Vy4, GlobalVariables.B_Vz4);
            float maxA = cfgAmax * cfgAfterburner;
            Vector3f accel = new Vector3f();
            targetVel.sub(currentVel, accel);
            if (accel.length() > maxA) accel.normalize().mul(maxA);
            currentVel.add(accel);
        }

        GlobalVariables.B_Vx1 = currentVel.x;
        GlobalVariables.B_Vy1 = currentVel.y;
        GlobalVariables.B_Vz1 = currentVel.z;
    }

    private static void printDebugInfo(String status) {
        System.out.printf("[这里写啥都没用了，反正idea不显示中文 - %s] 输入[Z:%d] | 真实惯性[Vx:%.3f, Vy:%.3f, Vz:%.3f]\n",
                status, GlobalVariables.B_INz, GlobalVariables.B_Vx1, GlobalVariables.B_Vy1, GlobalVariables.B_Vz1);
    }
}



// 下面是……我塞的屎！
/*package cn.rbq108.test.motion;

import cn.rbq108.test.VariableLibrary.GlobalVariables;
import cn.rbq108.test.VariableLibrary.Config; // <-- 神医提醒：千万别漏了导入 Config 大小姐喵！
import cn.rbq108.test.math.CoordinateSystemTransformation;
import org.joml.Vector3f;
import net.minecraft.client.Minecraft;

public class calculate {

    public static void calculateTargetVelocity() {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;


        double actualVx = mc.player.getX() - mc.player.xo;
        double actualVy = mc.player.getY() - mc.player.yo;
        double actualVz = mc.player.getZ() - mc.player.zo;

        if (mc.player.horizontalCollision) {
            if (Math.abs(actualVx) < 0.01) GlobalVariables.B_Vx1 = 0.0f;
            if (Math.abs(actualVz) < 0.01) GlobalVariables.B_Vz1 = 0.0f;
        }

        if (mc.player.verticalCollision) {
            if (Math.abs(actualVy) < 0.01) GlobalVariables.B_Vy1 = 0.0f;
        }


        float cfgVmax = Config.PHYSICS.vMax.get().floatValue();
        float cfgRushXy = Config.PHYSICS.rushXy.get().floatValue();
        float cfgRushZ = Config.PHYSICS.rushZ.get().floatValue();
        float cfgAfterburner = Config.PHYSICS.afterburnerRatio.get().floatValue();
        float cfgAmax = Config.PHYSICS.aMax.get().floatValue();
        float cfgBrake = Config.PHYSICS.brakeRatio.get().floatValue();


        int inX = GlobalVariables.B_INx;
        int inY = GlobalVariables.B_INy;
        int inZ = GlobalVariables.B_INz;

        if (GlobalVariables.B_rush && inZ < 0) {
            GlobalVariables.B_rush = false;
        }

        float weightX = 1.0f;
        float weightY = 0.8f;
        float weightZ = (inZ > 0) ? 1.2f : 1.0f;

        Vector3f localInput = new Vector3f(inX, inY, inZ);
        if (localInput.lengthSquared() > 0) {
            localInput.normalize();
        }

        localInput.x *= weightX;
        localInput.y *= weightY;
        localInput.z *= weightZ;

        // 使用 Config 里的 cfgVmax
        float targetVx = localInput.x * cfgVmax;
        float targetVy = localInput.y * cfgVmax;
        float targetVz = localInput.z * cfgVmax;


        // 使用 Config 里的冲刺倍率
        float currentRushX = GlobalVariables.B_rush ? cfgRushXy : 1.0f;
        float currentRushY = GlobalVariables.B_rush ? cfgRushXy : 1.0f;

        float currentRushZ = 1.0f;
        if (GlobalVariables.B_rush) {
            currentRushZ = (inZ > 0) ? cfgRushZ : cfgRushXy;
        }


        GlobalVariables.B_Vx3_1 = targetVx * currentRushX * cfgAfterburner;
        GlobalVariables.B_Vy3_1 = targetVy * currentRushY * cfgAfterburner;
        GlobalVariables.B_Vz3_1 = targetVz * currentRushZ * cfgAfterburner;


        CoordinateSystemTransformation.transformVelocityToWorld();


        boolean hasInput = (GlobalVariables.B_INx != 0 || GlobalVariables.B_INy != 0 || GlobalVariables.B_INz != 0);


        boolean shouldBrake = cn.rbq108.test.VariableLibrary.debug.DEBUG_B_HOLD || cn.rbq108.test.core.Keybinds.B_MANUAL_BRAKE.isDown();

        if (!hasInput && !shouldBrake) {
            printDebugInfo("自由滑行中...");
            return;
        }

        Vector3f targetWorldVel = new Vector3f(GlobalVariables.B_Vx4, GlobalVariables.B_Vy4, GlobalVariables.B_Vz4);
        Vector3f currentWorldVel = new Vector3f(GlobalVariables.B_Vx1, GlobalVariables.B_Vy1, GlobalVariables.B_Vz1);

        Vector3f neededAccel = new Vector3f();
        targetWorldVel.sub(currentWorldVel, neededAccel);

        // 使用 Config 里的最大加速度
        float currentMaxAccel = cfgAmax * cfgAfterburner;
/*

        if (!hasInput && cn.rbq108.test.VariableLibrary.debug.DEBUG_B_HOLD) {
            currentMaxAccel *= cfgBrake;
        }




        if (!hasInput && shouldBrake) {

            currentMaxAccel *= cfgBrake;
        }



        if (neededAccel.length() > currentMaxAccel) {
            neededAccel.normalize().mul(currentMaxAccel);
        }

        currentWorldVel.add(neededAccel);

        GlobalVariables.B_Vx1 = currentWorldVel.x;
        GlobalVariables.B_Vy1 = currentWorldVel.y;
        GlobalVariables.B_Vz1 = currentWorldVel.z;


        printDebugInfo(hasInput ? "引擎狂暴喷射中" : "主动减速制动中");
    }


    private static void printDebugInfo(String status) {
        System.out.printf("[148455674564 - %s] 输入[Z:%d] | 视角[Pitch:%.1f, Yaw:%.1f] | 真实惯性[Vx:%.3f, Vy:%.3f, Vz:%.3f] | 世界目标[Vx4:%.3f, Vy4:%.3f, Vz4:%.3f]\n",
                status,
                GlobalVariables.B_INz,
                GlobalVariables.B_Dx, GlobalVariables.B_Dy,
                GlobalVariables.B_Vx1, GlobalVariables.B_Vy1, GlobalVariables.B_Vz1,
                GlobalVariables.B_Vx4, GlobalVariables.B_Vy4, GlobalVariables.B_Vz4
        );
    }
}*/


// 没想到吧屎有两坨！

/*package cn.rbq108.test.motion;

import cn.rbq108.test.VariableLibrary.GlobalVariables;
import cn.rbq108.test.math.CoordinateSystemTransformation;
import org.joml.Vector3f;
import net.minecraft.client.Minecraft;

public class calculate {

    public static void calculateTargetVelocity() {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        double actualVx = mc.player.getX() - mc.player.xo;
        double actualVy = mc.player.getY() - mc.player.yo;
        double actualVz = mc.player.getZ() - mc.player.zo;


        if (mc.player.horizontalCollision) {
            // 如果 X 轴实际位移趋近于 0，说明 X 轴方向被墙彻底挡死，清空 X 轴惯性
            if (Math.abs(actualVx) < 0.01) GlobalVariables.B_Vx1 = 0.0f;
            // 如果 Z 轴被挡死，清空 Z 轴惯性
            if (Math.abs(actualVz) < 0.01) GlobalVariables.B_Vz1 = 0.0f;
        }


        if (mc.player.verticalCollision) {

            if (Math.abs(actualVy) < 0.01) GlobalVariables.B_Vy1 = 0.0f;
        }

        int inX = GlobalVariables.B_INx;
        int inY = GlobalVariables.B_INy;
        int inZ = GlobalVariables.B_INz;

        if (GlobalVariables.B_rush && inZ < 0) {
            GlobalVariables.B_rush = false;
        }

        float weightX = 1.0f;
        float weightY = 0.8f;
        float weightZ = (inZ > 0) ? 1.2f : 1.0f;

        Vector3f localInput = new Vector3f(inX, inY, inZ);
        if (localInput.lengthSquared() > 0) {
            localInput.normalize();
        }

        localInput.x *= weightX;
        localInput.y *= weightY;
        localInput.z *= weightZ;

        float targetVx = localInput.x * GlobalVariables.B_Vmax;
        float targetVy = localInput.y * GlobalVariables.B_Vmax;
        float targetVz = localInput.z * GlobalVariables.B_Vmax;


        float currentRushX = GlobalVariables.B_rush ? GlobalVariables.B_rush_xy : 1.0f;
        float currentRushY = GlobalVariables.B_rush ? GlobalVariables.B_rush_xy : 1.0f;

        float currentRushZ = 1.0f;
        if (GlobalVariables.B_rush) {
            currentRushZ = (inZ > 0) ? GlobalVariables.B_rush_z : GlobalVariables.B_rush_xy;
        }

        GlobalVariables.B_Vx3_1 = targetVx * currentRushX * GlobalVariables.B_AfterburnerRatio;
        GlobalVariables.B_Vy3_1 = targetVy * currentRushY * GlobalVariables.B_AfterburnerRatio;
        GlobalVariables.B_Vz3_1 = targetVz * currentRushZ * GlobalVariables.B_AfterburnerRatio;


        CoordinateSystemTransformation.transformVelocityToWorld();



        if (!hasInput && !cn.rbq108.test.VariableLibrary.debug.DEBUG_B_HOLD) {
            printDebugInfo("自由滑行中...");
            return;
        }

        Vector3f targetWorldVel = new Vector3f(GlobalVariables.B_Vx4, GlobalVariables.B_Vy4, GlobalVariables.B_Vz4);
        Vector3f currentWorldVel = new Vector3f(GlobalVariables.B_Vx1, GlobalVariables.B_Vy1, GlobalVariables.B_Vz1);

        Vector3f neededAccel = new Vector3f();
        targetWorldVel.sub(currentWorldVel, neededAccel);

        float currentMaxAccel = GlobalVariables.B_Amax * GlobalVariables.B_AfterburnerRatio;

        if (!hasInput && cn.rbq108.test.VariableLibrary.debug.DEBUG_B_HOLD) {
            currentMaxAccel *= GlobalVariables.B_BrakeRatio;
        }

        if (neededAccel.length() > currentMaxAccel) {
            neededAccel.normalize().mul(currentMaxAccel);
        }

        currentWorldVel.add(neededAccel);

        GlobalVariables.B_Vx1 = currentWorldVel.x;
        GlobalVariables.B_Vy1 = currentWorldVel.y;
        GlobalVariables.B_Vz1 = currentWorldVel.z;


        printDebugInfo(hasInput ? "1" : "2");
    }


    private static void printDebugInfo(String status) {
        System.out.printf("[548584 - %s] 输入[Z:%d] | 视角[Pitch:%.1f, Yaw:%.1f] | 真实惯性[Vx:%.3f, Vy:%.3f, Vz:%.3f] | 世界目标[Vx4:%.3f, Vy4:%.3f, Vz4:%.3f]\n",
                status,
                GlobalVariables.B_INz,
                GlobalVariables.B_Dx, GlobalVariables.B_Dy,
                GlobalVariables.B_Vx1, GlobalVariables.B_Vy1, GlobalVariables.B_Vz1,
                GlobalVariables.B_Vx4, GlobalVariables.B_Vy4, GlobalVariables.B_Vz4
        );
    }
}
*/