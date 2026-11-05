/*
正如标题，这个是在失重条件下启停冲刺的

双击w进入冲刺状态，亦或者在按w平移的时候按住左Ctrl（物理级读取，不干扰原版）进入冲刺

撞到东西让速度降到一定阈值、或者松开w、或者按s均会退出冲刺
*/
package cn.rbq108.nextboundarycornerstone.motion;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import cn.rbq108.nextboundarycornerstone.core.Keybinds; // 🩺 导入你刚注册的按键类
import net.minecraft.client.Minecraft;

public class Rush {
    private static long lastWPressTime = 0;
    private static boolean wasWDown = false;
    private static final long DOUBLE_CLICK_INTERVAL = 300; // 300毫秒内双击有效

    public static void updateRushState() {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        //读取玩家自定义的“前进键”状态
        boolean isForwardDown = GlobalVariables.B_INz > 0;

        // 读取注册的专用冲刺键状态
        // 使用 isDown() 直接获取物理按下状态，不触发原版疾跑逻辑
        boolean isRushKeyDown = cn.rbq108.nextboundarycornerstone.core.KeyPollingHandler.isPressed(Keybinds.B_RUSH_BUTTON);



        // 前进时按下专用冲刺键 (W + B_RushButton)
        if (isForwardDown && isRushKeyDown && GlobalVariables.B_INz > 0) {
            GlobalVariables.B_rush = true;
        }

        //双击前进检测 (Double W)
        if (isForwardDown && !wasWDown) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastWPressTime < DOUBLE_CLICK_INTERVAL) {
                GlobalVariables.B_rush = true;
            }
            lastWPressTime = currentTime;
        }
        wasWDown = isForwardDown;


        if (GlobalVariables.B_INz <= 0) {
            GlobalVariables.B_rush = false;
        }


        if (mc.player.horizontalCollision || mc.player.verticalCollision) {
            float speedSq = GlobalVariables.B_Vx1 * GlobalVariables.B_Vx1 +
                    GlobalVariables.B_Vy1 * GlobalVariables.B_Vy1 +
                    GlobalVariables.B_Vz1 * GlobalVariables.B_Vz1;
            if (speedSq < 0.001f) {
                GlobalVariables.B_rush = false;
            }
        }
    }
    public static void reset() {
        lastWPressTime = 0;
        wasWDown = false;
        GlobalVariables.B_rush = false;
    }
}
