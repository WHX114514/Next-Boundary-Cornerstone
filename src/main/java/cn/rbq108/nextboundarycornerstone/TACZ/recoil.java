package cn.rbq108.nextboundarycornerstone.TACZ;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.GunRecoilKeyFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Quaternionf;

public class recoil {

    /**
     * 【四元数版】在玩家开火的瞬间调用此方法。
     * 基于飞船/玩家当前的四元数姿态，将局部坐标系的后坐力精准投影至世界坐标系。
     */
    public static void applyWeaponBlowback() {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;


        //获取玩家主手持有的物品
        ItemStack mainHandItem = mc.player.getMainHandItem();

        //检查手上拿的是不是 TACZ 的枪
        if (mainHandItem.getItem() instanceof IGun iGun) {
            ResourceLocation gunId = iGun.getGunId(mainHandItem);

            //从 TACZ 抓取静态数据
            TimelessAPI.getClientGunIndex(gunId).ifPresent(gunIndex -> {
                GunData gunData = gunIndex.getGunData();
                if (gunData == null || gunData.getRecoil() == null) return;

                GunRecoilKeyFrame[] pitchFrames = gunData.getRecoil().getPitch();

                if (pitchFrames != null && pitchFrames.length > 0) {
                    //提取第一帧基础垂直后座力
                    float[] values = pitchFrames[0].getValue();
                    float baseRecoilForce = (values[0] + values[1]) / 2.0f;

                    //四元数！
                    // 设定核心缩放系数
                    float scaleFactor = 0.05f;
                    float finalForce = baseRecoilForce * scaleFactor;

                    // 定义局部坐标系下的后坐力向量。

                    Vector3f localRecoil = new Vector3f(0.0f, 0.0f, -finalForce);

                    // 获取当前飞船/玩家的四元数姿态
                    // 从全局变量读取你算好的、包含 Roll（滚转）在内的完美四元数
                    Quaternionf shipOrientation = new Quaternionf(GlobalVariables.currentQuat);

                    // 让四元数转转转~局部向量
                    Vector3f worldRecoil = shipOrientation.transform(localRecoil);

                    // 抛给最终账本
                    GlobalVariables.B_Vx5 = worldRecoil.x;
                    GlobalVariables.B_Vy5 = worldRecoil.y;
                    GlobalVariables.B_Vz5 = worldRecoil.z;
                    System.out.println(GlobalVariables.B_Vx5+ " " +GlobalVariables.B_Vy5+ " "+GlobalVariables.B_Vz5);
                }
            });
        }
    }
}

//package cn.rbq108.nextboundarycornerstone.TACZ;
//
//import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
//import com.tacz.guns.api.TimelessAPI;
//import com.tacz.guns.api.item.IGun;
//import com.tacz.guns.resource.pojo.data.gun.GunData;
//import com.tacz.guns.resource.pojo.data.gun.GunRecoilKeyFrame;
//import net.minecraft.client.Minecraft;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.phys.Vec3;
//
//public class recoil {
//
//    /*
//     * 在玩家开火的瞬间调用此方法，从 TACZ 中抓取枪械的基础后坐力，
//     * 并计算出反方向的世界坐标系动量，注入到 B_Vx5 / y5 / z5 中。
//     */
//    public static void applyWeaponBlowback() {
//        var mc = Minecraft.getInstance();
//        if (mc.player == null) return;
//
//        //获取玩家主手持有的物品
//        ItemStack mainHandItem = mc.player.getMainHandItem();
//
//        //检查手上拿的是不是 TACZ 的枪
//        if (mainHandItem.getItem() instanceof IGun iGun) {
//            //拿到这把枪的唯一注册 ID (例如 "tacz:m4a1")
//            ResourceLocation gunId = iGun.getGunId(mainHandItem);
//
//            // 从 TACZ 客户端注册表中抓取枪械静态属性索引并提取 GunData
//            TimelessAPI.getClientGunIndex(gunId).ifPresent(gunIndex -> {
//                GunData gunData = gunIndex.getGunData();
//                if (gunData == null || gunData.getRecoil() == null) return;
//
//                //抓取垂直后座的关键帧数组
//                GunRecoilKeyFrame[] pitchFrames = gunData.getRecoil().getPitch();
//
//                if (pitchFrames != null && pitchFrames.length > 0) {
//                    //提取第一帧（Index 0）后座力的 [最小值, 最大值]
//                    float[] values = pitchFrames[0].getValue();
//
//                    // 取个平均值作为这把枪的基础垂直后座力数值 (Base Recoil)
//                    float baseRecoilForce = (values[0] + values[1]) / 2.0f;
//
//                    // 后坐力是开火方向的反方向
//                    // 拿到玩家当前视角的正前方单位方向向量
//                    Vec3 lookVec = mc.player.getLookAngle();
//
//                    // 后座力数值通常是视角上跳度数，当成位移速度会过大
//                    // 暂定系数0.05f
//                    float scaleFactor = 0.05f;
//
//                    // 将反冲动量（朝脑后退）拆解到世界三轴上，塞~后坐力~
//                    GlobalVariables.B_Vx5 = (float) (-lookVec.x * baseRecoilForce * scaleFactor);
//                    GlobalVariables.B_Vy5 = (float) (-lookVec.y * baseRecoilForce * scaleFactor);
//                    GlobalVariables.B_Vz5 = (float) (-lookVec.z * baseRecoilForce * scaleFactor);
//                }
//            });
//        }
//    }
//}