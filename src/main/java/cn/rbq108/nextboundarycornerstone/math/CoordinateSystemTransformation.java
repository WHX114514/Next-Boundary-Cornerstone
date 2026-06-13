package cn.rbq108.nextboundarycornerstone.math;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import org.joml.Vector3f;

/*
该类负责将各种向量在玩家坐标系与世界坐标系之间互相转换
*/
public class CoordinateSystemTransformation {

    public static void transformVelocityToWorld() {
        // 获取吃满所有倍率的“本地期望推力”
        float localX = GlobalVariables.B_Vx3_1; // 右
        float localY = GlobalVariables.B_Vy3_1; // 上
        float localZ = GlobalVariables.B_Vz3_1; // 前


        // 降维打击般的四元数引擎！
        //四元数！四元数是3d游戏中最伟大的发明！

        // 修正 在麦块坐标系中，当面向正南(+Z)时，右边是正西(-X)。
        // 所以本地的“向右(localX为正)”实际上需要反转为负号，才能匹配
        Vector3f localVel = new Vector3f(-localX, localY, localZ);


        GlobalVariables.currentQuat.transform(localVel);

        // 把转换好的“地面世界速度”解包，存进这堆数据里
        GlobalVariables.B_Vx4 = localVel.x;
        GlobalVariables.B_Vy4 = localVel.y;
        GlobalVariables.B_Vz4 = localVel.z;
    }
}