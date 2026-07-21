package cn.rbq108.nextboundarycornerstone.Mixin;

/*
 * 调试留存文件：用于测试 JOML 四元数旋转逻辑
 * 
 * import org.joml.Quaternionf;
 * import org.joml.Vector3f;
 * 
 * public class TestJoml {
 *     public static void main(String[] args) {
 *         Quaternionf q = new Quaternionf();
 *         //向下90度喵
 *         q.rotateX((float)Math.toRadians(90));
 *         System.out.println("After rotateX(90): " + q.getEulerAnglesYXZ(new Vector3f()));
 *         
 *         //90度的偏航？
 *         q.rotateY((float)Math.toRadians(90));
 *         System.out.println("After rotateY(90): " + q.getEulerAnglesYXZ(new Vector3f()));
 *         
 *         Quaternionf q2 = new Quaternionf();
 *         q2.rotateLocalX((float)Math.toRadians(90));
 *         q2.rotateLocalY((float)Math.toRadians(90));
 *         System.out.println("After rotateLocal: " + q2.getEulerAnglesYXZ(new Vector3f()));
 *     }
 * }
 */
