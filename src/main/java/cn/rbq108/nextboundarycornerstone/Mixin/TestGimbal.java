package cn.rbq108.nextboundarycornerstone.Mixin;

/*
 * 当初为了解决万向节死锁和前向向量提取，搁这儿试错测了好久的纪念（
 * import org.joml.Quaternionf;
 * import org.joml.Vector3f;
 * 
 * public class TestGimbal {
 *     public static void main(String[] args) {
 *         Quaternionf q = new Quaternionf();
 *         // 模拟直接垂直向下看（89.99度，差点死锁的临界点喵）
 *         q.rotationYXZ((float)Math.toRadians(45), (float)Math.toRadians(89.99), 0);
 *         
 *         // 旋转前向向量，准备提取姿态
 *         Vector3f fwd = new Vector3f(0, 0, 1).rotate(q);
 *         
 *         // 算水平投影距离，用来防分母为0和万向节死锁
 *         double horiz = Math.sqrt(fwd.x * fwd.x + fwd.z * fwd.z);
 *         
 *         float yaw = 45;
 *         if (horiz > 0.001) {
 *             // 只有水平向量够大才重新算 Yaw，防止极点处 180 度大跳变
 *             yaw = (float) Math.toDegrees(-Math.atan2(fwd.x, fwd.z));
 *         }
 *         // 用 asin 安全提取 Pitch，值域限制在 [-90, 90]
 *         float pitch = (float) Math.toDegrees(Math.asin(-fwd.y));
 *         
 *         System.out.println("前向向量: " + fwd);
 *         System.out.println("解算结果 - 偏航角 Yaw: " + yaw + ", 俯仰角 Pitch: " + pitch);
 *     }
 * }
 */
