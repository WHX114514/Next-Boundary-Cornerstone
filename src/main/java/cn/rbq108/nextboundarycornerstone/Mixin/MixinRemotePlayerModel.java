package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.rendering.OtherPlayer;
import cn.rbq108.nextboundarycornerstone.ServeMiao.communication.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(HumanoidModel.class)
public abstract class MixinRemotePlayerModel<T extends LivingEntity> {

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void onSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof Player player) {
            // 依旧放过自己模组
            if (player == Minecraft.getInstance().player) return;

            if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(player.getUUID(), false)) {
                HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;

                //安全获取数据（都写在mixin类里了真的安全吗）
                UUID uuid = player.getUUID();
                // 使用 getOrDefault 确保即使没东西也只会返回 null
                OtherPlayer.RotationData data = OtherPlayer.ROTATION_SYNC_MAP.getOrDefault(uuid, null);

                // 只有数据完全齐活了（Data、身体四元数、脑袋四元数都不为空）才跑
                if (data != null && data.bodyQuatRender != null && data.headQuatRender != null) {
                    //纯四元数计算相对差值
                    Quaternionf headTwist = new Quaternionf(data.bodyQuatRender).conjugate().mul(data.headQuatRender);
                    headTwist.normalize();

                    // 走最短路径，防止出现大于 180 度的诡异翻折
                    if (headTwist.w < 0.0f) {
                        headTwist.x = -headTwist.x;
                        headTwist.y = -headTwist.y;
                        headTwist.z = -headTwist.z;
                        headTwist.w = -headTwist.w;
                    }

                    //纯四元数物理限制（防止脑袋转360度的诡异东西出现）
                    float maxW = 0.737277f;
                    if (headTwist.w < maxW) {
                        float currentSin = (float) Math.sqrt(1.0 - headTwist.w * headTwist.w);
                        float targetSin = (float) Math.sqrt(1.0 - maxW * maxW);
                        float scale = targetSin / currentSin;
                        headTwist.x *= scale;
                        headTwist.y *= scale;
                        headTwist.z *= scale;
                        headTwist.w = maxW; // 锁死最大旋转角
                    }

                    // 提取欧拉角
                    Vector3f euler = headTwist.getEulerAnglesZYX(new Vector3f());

                    // 赋值！！！
                    model.head.xRot = euler.x;
                    model.head.yRot = -euler.y;
                    model.head.zRot = -euler.z;

                    model.hat.xRot = model.head.xRot;
                    model.hat.yRot = model.head.yRot;
                    model.hat.zRot = model.head.zRot;

                    // 躯干和四肢依然保持 0（因为外面 PoseStack 已经完美平滑转过去了）
                    model.body.yRot = 0;
                    model.leftArm.yRot = 0; model.rightArm.yRot = 0;
                    model.leftLeg.yRot = 0; model.rightLeg.yRot = 0;
                }

                /*这坨角度过大时还会有问题
                if (data != null) {

                    Quaternionf headTwist = new Quaternionf(data.bodyQuatRender).conjugate().mul(data.headQuatRender);


                    Vector3f nose = new Vector3f(0, 0, 1);


                    nose.rotate(headTwist);


                    // Math.atan2 支持完美的 360 度圆周，绝对、绝对、绝对不会出现 180 度断脖子现象！
                    float yaw = (float) Math.atan2(nose.x, nose.z);
                    float pitch = (float) Math.asin(-nose.y);

                    //
                    // 这里继续用刚才自己摸索出来的正负号！(如果上下左右反了，加个负号就行)
                    model.head.xRot = pitch;   // 比如您之前如果用了 -pitch 就继续用
                    model.head.yRot = yaw;     // 同上

                    //
                    model.head.zRot = 0;

                    model.hat.xRot = model.head.xRot;
                    model.hat.yRot = model.head.yRot;
                    model.hat.zRot = model.head.zRot;

                    //
                    model.body.yRot = 0;
                    model.leftArm.yRot = 0; model.rightArm.yRot = 0;
                    model.leftLeg.yRot = 0; model.rightLeg.yRot = 0;
                }

                /*这坨角度过大时会有问题
                if (data != null) {
                    //
                    // 注意变量名：headTwist
                    Quaternionf headTwist = new Quaternionf(data.bodyQuatRender).conjugate().mul(data.headQuatRender);

                    //
                    // 刚才就是这里的变量名和上面对不上导致报错喵！
                    Vector3f euler = headTwist.getEulerAnglesZYX(new Vector3f());

                    //
                    // 在 YXZ 顺序下，受限制不超过 90 度的变成了 X (上下低头)，
                    // 而 Y (左右扭头) 获得了完整的 +-180 度解放
                    //Vector3f euler = headTwist.getEulerAnglesYXZ(new Vector3f());

                    //
                    model.head.xRot = (euler.x);
                    model.head.yRot = -(euler.y);
                    model.head.zRot = -(euler.z);
                    // 这里的正负号很重要
                    // 如果 Pitch（上下）反了，就去掉 x 前面的负号
                    // 如果 Roll（翻滚）反了，就给 z 加上负号

                    model.hat.xRot = euler.x;
                    model.hat.yRot = euler.y;
                    model.hat.zRot = euler.z;

                    // 保持躯干和四肢为 0，因为外面 PoseStack 已经整体平滑旋转过了喵
                    model.body.yRot = 0;
                    model.leftArm.yRot = 0; model.rightArm.yRot = 0;
                    model.leftLeg.yRot = 0; model.rightLeg.yRot = 0;
                }*/
            }
        }
    }
}

/*头反了！
package cn.rbq108.test.Mixin;

import cn.rbq108.test.rendering.OtherPlayer;
import cn.rbq108.test.ServeMiao.communication.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class MixinRemotePlayerModel<T extends LivingEntity> {

    private static final Logger log = LoggerFactory.getLogger(MixinRemotePlayerModel.class);

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void onSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof Player player) {
            if (player == Minecraft.getInstance().player) return;

            if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(player.getUUID(), false)) {
                HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;

                // 1. 获取黑板上的平滑姿态数据
                OtherPlayer.RotationData data = OtherPlayer.ROTATION_SYNC_MAP.get(player.getUUID());
                if (data != null) {

                    //
                    // 计算公式：inverse(smoothBodyQuat) * headQuatNet
                    Quaternionf headTwistQuat = new Quaternionf(data.bodyQuatRender).conjugate().mul(data.headQuatRender);

                    Vector3f euler = headTwistQuat.getEulerAnglesXYZ(new Vector3f());


                    // 如果 Pitch（上下）反了，就去掉 x 前面的负号
                    // 如果 Roll（翻滚）反了，就给 z 加上负号
                    model.head.xRot = euler.x;
                    model.head.yRot = -euler.y;
                    model.head.zRot = -euler.z;


                    model.hat.xRot = model.head.xRot;
                    model.hat.yRot = model.head.yRot;
                    model.hat.zRot = model.head.zRot;


                    // 保持躯干和四肢为 0，因为外面 PoseStack 已经转过了
                    model.body.yRot = 0;
                    model.leftArm.yRot = 0; model.rightArm.yRot = 0;
                    model.leftLeg.yRot = 0; model.rightLeg.yRot = 0;



                    //
                    // 乘以 -1 是因为 Minecraft 渲染模型的 X 和 Y 轴是反着的
                    Vector3f eulerAngles = headTwistQuat.getEulerAnglesXYZ(new Vector3f());


                    model.head.xRot = -eulerAngles.x; // pitch (低头抬头)
                    model.head.yRot = -eulerAngles.y; // yaw (扭头左看右看)
                    model.head.zRot = eulerAngles.z; // roll (虽然人形模型不支持，但是我们尽力了喵！)
                    model.hat.xRot = -eulerAngles.x; model.hat.yRot = -eulerAngles.y; model.hat.zRot = eulerAngles.z;


                    model.body.yRot = 0;
                    model.leftArm.yRot = 0; model.rightArm.yRot = 0;
                    model.leftLeg.yRot = 0; model.rightLeg.yRot = 0;
                }
            }
        }
    }
}*/

/*角度正确但是四肢脱臼
package cn.rbq108.test.Mixin;

import cn.rbq108.test.rendering.OtherPlayer;
import cn.rbq108.test.ServeMiao.communication.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class MixinRemotePlayerModel<T extends LivingEntity> {

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void onSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof Player player) {
            if (player == Minecraft.getInstance().player) return;

            if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(player.getUUID(), false)) {
                HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;

                // 1. 脑袋瞬间跟手
                model.head.yRot = 0;
                model.head.xRot = 0;
                model.hat.yRot = 0;
                model.hat.xRot = 0;

                Float twist = OtherPlayer.BODY_TWIST_MAP.get(player.getUUID());
                if (twist != null) {
                    float radTwist = twist * 0.017453292F;

                    // 给躯干加上延迟扭曲
                    model.body.yRot = radTwist;

                    // 给四肢也加上相同的扭曲！
                    // 注意是 += ，这样他们原本走路前后摆动的动画就不会被覆盖
                    model.leftArm.yRot += radTwist;
                    model.rightArm.yRot += radTwist;
                    model.leftLeg.yRot += radTwist;
                    model.rightLeg.yRot += radTwist;
                }
            }
        }
    }
}
*/


/*
这个版本四肢是抽搐的
package cn.rbq108.test.Mixin;

import cn.rbq108.test.rendering.OtherPlayer;
import cn.rbq108.test.ServeMiao.communication.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class MixinRemotePlayerModel<T extends LivingEntity> {

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void onSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof Player player) {
            if (player == Minecraft.getInstance().player) return;

            if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(player.getUUID(), false)) {
                HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;


                model.head.yRot = 0;
                model.head.xRot = 0;
                model.hat.yRot = 0;
                model.hat.xRot = 0;


                Float twist = OtherPlayer.BODY_TWIST_MAP.get(player.getUUID());

                    model.body.yRot = twist * 0.017453292F;
                }
            }
        }
    }
}*/


/*以下是没适配平滑版本otherplayer的版本，为了适配平滑的otherplayer弃用
package cn.rbq108.test.Mixin;


import cn.rbq108.test.ServeMiao.communication.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 直接劫持所有类人模型（包括玩家和盔甲）
@Mixin(HumanoidModel.class)
public abstract class MixinRemotePlayerModel<T extends LivingEntity> {

    /**
     * 在原版骨骼动画设置完毕后（TAIL），咱们来做最后的骨骼矫正

    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void onSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {

        if (entity instanceof Player player) {

            if (player == Minecraft.getInstance().player) {
                return;
            }


            if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(player.getUUID(), false)) {

                // 拿到当前正在渲染的模型骨架
                HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;


                // 因为外面 PoseStack 已经被 remoteQuat 整体旋转过了，
                // remoteQuat 代表了鼠标/脑袋的瞬间指向，所以整个模型此时已经对齐了脑袋

                float bodyYaw = player.yBodyRot;
                float headYaw = player.yHeadRot;


                // remoteQuat 已经代表了绝对的正确姿态（正确牛魔呢）

                // 脑袋保持 0 偏移
                model.head.yRot = 0;
                model.head.xRot = 0;
                model.hat.yRot = 0;
                model.hat.xRot = 0;




                model.body.yRot = 0;


                // model.leftArm.yRot = 0;
                // model.rightArm.yRot = 0;
                //调试用



                // if (model instanceof PlayerModel pm) { pm.jacket.yRot = model.body.yRot; }
            }
        }
    }
}*/