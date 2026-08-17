package cn.rbq108.nextboundarycornerstone.rendering;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class spawnRcsParticle {

    // 记录上一帧的世界速度，用来算真实的 ΔV（加速度）喵！
    private static Vec3 prevVelocity = Vec3.ZERO;


    // 喷口相对坐标矩阵 (需要自己填喵！)
    // 注意：Blockbench里的像素值记得除以 16.0f 转换成游戏里的米喵！

    private static final Vector3f[] NOZZLES_TOP = {
            new Vector3f(-0.2625f, 1.65f, -0.815625f),
            new Vector3f(-1.13125f, 1.65f, -0.815625f),
            new Vector3f(-0.2625f, 1.65f, -0.190625f),
            new Vector3f(-1.13125f, 1.65f, -0.190625f),/*填入顶部的喷口(本来想换成双杠注释的，但是会报错吓哭了）*/
    };
    private static final Vector3f[] NOZZLES_BOTTOM = {
            new Vector3f(-0.28125f, 0.5125f, -0.190625f),
            new Vector3f(-0.28125f, 0.5125f, -0.815625f),
            new Vector3f(-1.1125f, 0.5125f, -0.190625f),
            new Vector3f(-1.1125f, 0.5125f, -0.815625f)/* 填入底部的喷口(哦草报错是因为双杠注释把后面的大括号一块弄没了） */ };
    private static final Vector3f[] NOZZLES_FRONT  = {
            new Vector3f(0.265625f, 1.6171875f, -0.8515625f),
            new Vector3f(1.128125f, 1.6171875f, -0.8515625f),
            new Vector3f(0.28125f, 0.5390625f, -0.8515625f),
            new Vector3f(-1.1125f, 0.5390625f, -0.1546875f)/* 填入前面的喷口 */ };
    private static final Vector3f[] NOZZLES_BACK   = {
            new Vector3f(-0.265625f, 1.6171875f, -0.1546875f),
            new Vector3f(-1.128125f, 1.6171875f, -0.1546875f),
            new Vector3f(-0.28125f, 0.5390625f, -0.1546875f),
            new Vector3f(-1.1125f, 0.5390625f, -0.1546875f)/* 填入后面的喷口 */ };
    private static final Vector3f[] NOZZLES_LEFT   = {
            new Vector3f(-1.14375f, 1.625f, -0.190625f),
            new Vector3f(-1.14375f, 1.625f, -0.8f),
            new Vector3f(-1.1125f, 0.546875f, -0.190625f),
            new Vector3f(-1.1125f, 0.546875f, -0.815625f), /* 填入左侧的喷口 */ };
    private static final Vector3f[] NOZZLES_RIGHT  = {
            new Vector3f(-0.25f, 1.625f, -0.8f),
            new Vector3f(-0.25f, 1.625f, -0.190625f),
            new Vector3f(-0.265625f, 0.546875f, -0.8f),
            new Vector3f(-0.265625f, 0.546875f, -0.190625f),/* 填入右侧的喷口 */ };

    /*
     * 核心飞控侦测逻辑：在 onClientTick 里每帧调用喵！
     */
    public static void processRcs(Player player) {
        // 【新增兼容】：如果玩家正在被外部物理设施（如铁把手）拉扯，说明加速度并非来自宇航服，跳过喷气渲染
        if (cn.rbq108.nextboundarycornerstone.ModCompatible.isGrabbingAeronauticsHandle(player)) {
            // 静默同步当前速度，防止松手瞬间由于积累的差值爆出一大团粒子
            prevVelocity = player.getDeltaMovement();
            return;
        }

        // 如果玩家穿着下一边界工业的宇航服，跳过前置模组的喷气效果渲染，转由工业模组自己渲染并支持热重载微调！
        net.minecraft.world.item.ItemStack chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (!chest.isEmpty() && chest.getItem().getClass().getName().contains("ExtravehicularSpacesuit")) {
            return;
        }

        Vec3 currentVelocity = player.getDeltaMovement();

        // 1. 算世界坐标系下的加速度 (A_world = V_current - V_prev)
        Vector3f worldAccel = new Vector3f(
                (float)(currentVelocity.x - prevVelocity.x),
                (float)(currentVelocity.y - prevVelocity.y),
                (float)(currentVelocity.z - prevVelocity.z)
        );

        // 存档给下一帧用
        prevVelocity = currentVelocity;

        if (worldAccel.lengthSquared() < 0.001f) return;


        // 把世界加速度“扭”回玩家的机体坐标系（Local Space）
        Quaternionf inverseQuat = new Quaternionf(GlobalVariables.getBodyQuat()).invert();
        Vector3f localAccel = worldAccel.rotate(inverseQuat);

        // 根据机体感受到的推力方向，点对应的喷口喵
        float threshold = 0.02f; //

        // Y 轴：机体上下（加速向上 -> 底部喷火往下推）
        if (localAccel.y > threshold) fireNozzles(player, NOZZLES_BOTTOM, new Vector3f(0, -1, 0));
        else if (localAccel.y < -threshold) fireNozzles(player, NOZZLES_TOP, new Vector3f(0, 1, 0));

        // Z 轴：机体前后（Minecraft里 Z 负方向是前。加速向前 -> 尾部喷火往后推）
        if (localAccel.z < -threshold) fireNozzles(player, NOZZLES_BACK, new Vector3f(0, 0, 1));
        else if (localAccel.z > threshold) fireNozzles(player, NOZZLES_FRONT, new Vector3f(0, 0, -1));

        // X 轴：机体左右（X 正方向是右。加速向右 -> 左侧喷火向左推）
        if (localAccel.x > threshold) fireNozzles(player, NOZZLES_LEFT, new Vector3f(-1, 0, 0));
        else if (localAccel.x < -threshold) fireNozzles(player, NOZZLES_RIGHT, new Vector3f(1, 0, 0));
    }

    /*
     * 粒子发射器：将局部喷口动态对齐到 3D 支架的当前旋转状态喵！
     */
    private static void fireNozzles(Player player, Vector3f[] nozzles, Vector3f localDirection) {
        Quaternionf quat = GlobalVariables.getBodyQuat();
        // 拿到随机数生成器，让粒子别排成死板的一条线
        net.minecraft.util.RandomSource random = player.getRandom();

        for (Vector3f localOffset : nozzles) {
            Vector3f rotatedOffset = new Vector3f(localOffset).rotate(quat);
            Vector3f rotatedDir = new Vector3f(localDirection).rotate(quat);

            double posX = player.getX() +0.8f+ rotatedOffset.x;
            double posY = player.getY() -0.62f+ rotatedOffset.y;
            double posZ = player.getZ() + rotatedOffset.z;



            for (int i = 0; i < 3; i++) {
                // 给位置加一点点细微的随机偏移，防止所有粒子重叠
                double jitterX = (random.nextFloat() - 0.5f) * 0.05;
                double jitterY = (random.nextFloat() - 0.5f) * 0.05;
                double jitterZ = (random.nextFloat() - 0.5f) * 0.05;

                // 重新设定粒子属性喵
                net.minecraft.core.particles.DustParticleOptions rcsSmoke =
                        new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.9f, 0.9f, 0.9f), 0.4f);

                player.level().addParticle(
                        rcsSmoke,
                        posX + jitterX, posY + jitterY, posZ + jitterZ,
                        // 给速度也加点随机性
                        rotatedDir.x * 0.15 + (random.nextFloat() - 0.5) * 0.02,
                        rotatedDir.y * 0.15 + (random.nextFloat() - 0.5) * 0.02,
                        rotatedDir.z * 0.15 + (random.nextFloat() - 0.5) * 0.02
                );
            }
        }
    }
    /*private static void fireNozzles(Player player, Vector3f[] nozzles, Vector3f localDirection) {
        Quaternionf quat = GlobalVariables.getBodyQuat();

        for (Vector3f localOffset : nozzles) {

            Vector3f rotatedOffset = new Vector3f(localOffset).rotate(quat);

            Vector3f rotatedDir = new Vector3f(localDirection).rotate(quat);


            double posX = player.getX() + rotatedOffset.x;
            double posY = player.getY() + 0.0 + rotatedOffset.y;
            double posZ = player.getZ() + rotatedOffset.z;



            net.minecraft.core.particles.DustParticleOptions smallGlow =
                    new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.9f, 0.9f, 0.9f), 0.3f);

            player.level().addParticle(
                    smallGlow,
                    posX, posY, posZ,
                    rotatedDir.x * 0.15, rotatedDir.y * 0.15, rotatedDir.z * 0.15
            );




            player.level().addParticle(
                    ParticleTypes.CLOUD,
                    posX, posY, posZ,
                    rotatedDir.x * 0.15, rotatedDir.y * 0.15, rotatedDir.z * 0.15 // 给点初始喷射速度喵
            );
        }
    }*/
}