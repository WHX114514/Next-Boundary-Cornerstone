package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import org.joml.Vector3f;
import com.tacz.guns.entity.EntityKineticBullet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityKineticBullet.class)
public class FixTACZ {

    @Inject(method = "getFirstPersonRenderOffset", at = @At("RETURN"), cancellable = true, remap = false)
    private void nextboundary$fixFirstPersonRenderOffset(CallbackInfoReturnable<Vector3f> cir) {
        Vector3f originalOffset = cir.getReturnValue();

        if (originalOffset == null || !GlobalVariables.B_LowGravity) {
            return;
        }

        net.minecraft.world.entity.projectile.Projectile projectile = (net.minecraft.world.entity.projectile.Projectile) (Object) this;
        if (!projectile.level().isClientSide()) {
            return;
        }

        // 提取当前的 Roll (翻滚角)
        Vector3f euler = GlobalVariables.currentQuat.getEulerAnglesYXZ(new Vector3f());

        //转反了记得在地下euler.z加上负号
        float rollAngle =- euler.z;

        Vector3f offset = new Vector3f(originalOffset);

        // 核心数学：只在局部平面（屏幕所在的 X-Y 平面）进行 2D 旋转
        float cos = (float) Math.cos(rollAngle);
        float sin = (float) Math.sin(rollAngle);

        // 绕 Z 轴旋转 X (左右) 和 Y (上下) 偏移量
        float newX = (offset.x * cos - offset.y * sin)*3;
        float newY = (offset.x * sin + offset.y * cos)*3;

        // 更新偏移量并返回 (Z轴前后距离保持不变)
        offset.set(newX, newY, offset.z);
        cir.setReturnValue(offset);
    }
}
//package cn.rbq108.nextboundarycornerstone.Mixin;
//
//import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
//import net.minecraft.world.entity.Entity;
//import net.minecraft.world.entity.player.Player;
//import org.joml.Quaternionf;
//import org.joml.Vector3f;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(targets = "com.tacz.guns.entity.EntityKineticBullet", remap = false)
//public class FixTACZ {
//
//    // 拦截 TACZ 获取第一人称渲染偏移量的方法
//    @Inject(method = "getFirstPersonRenderOffset", at = @At("RETURN"), cancellable = true)
//    private void nextboundary$fixFirstPersonRenderOffset(CallbackInfoReturnable<Vector3f> cir) {
//        Vector3f originalOffset = cir.getReturnValue();
//
//        // 如果没有开启低重力模式，或者原本偏移量就是空的，直接放行，节省性能
//        if (originalOffset == null || !GlobalVariables.B_LowGravity) {
//            return;
//        }
//
//        // 将自己强转为 Projectile，以获取所处的 Level 和 Owner
//        net.minecraft.world.entity.projectile.Projectile projectile = (net.minecraft.world.entity.projectile.Projectile) (Object) this;
//
//        // 渲染只发生在客户端，如果是在服务端端跑到了这里，直接忽略
//        if (!projectile.level().isClientSide()) {
//            return;
//        }
//
//        Entity owner = projectile.getOwner();
//
//        // 第一人称渲染的 owner 必然是玩家
//        if (owner instanceof Player player) {
//
//            // 克隆一个向量，因为原返回值可能是个被 TACZ 缓存的固定对象，直接改会导致全图错乱
//            Vector3f offset = new Vector3f(originalOffset);
//
//            float vanillaPitch = player.getXRot();
//            float vanillaYaw = player.getYRot();
//
//            // 1. 构建 TACZ 在算这个渲染偏移时用的“错误旋转矩阵”
//            Quaternionf wrongRotation = new Quaternionf().rotationYXZ(
//                    (float) Math.toRadians(-vanillaYaw),
//                    (float) Math.toRadians(vanillaPitch),
//                    0f
//            );
//
//            // 求逆（Undo 操作）
//            wrongRotation.invert();
//
//            // 2. 把世界坐标系下算错的偏移量，打回原始的“相机局部空间”
//            // 这一步之后，offset 就变成了纯粹的相对于摄像机模型的前后左右偏移
//            wrongRotation.transform(offset);
//
//            // 3. 套用咱们本地维护的、带有真实 Roll 轴的 6DoF 四元数
//            // 注意：这是纯客户端第一人称渲染拦截，所以直接用 GlobalVariables.currentQuat 是最精准的
//            GlobalVariables.currentQuat.transform(offset);
//
//            // 4. 把彻底修正好的真 3D 偏移量塞回去，让 TACZ 拿着它去画光束
//            cir.setReturnValue(offset);
//        }
//    }
//}