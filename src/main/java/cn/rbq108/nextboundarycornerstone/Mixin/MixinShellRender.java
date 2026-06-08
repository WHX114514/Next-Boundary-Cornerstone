package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import cn.rbq108.nextboundarycornerstone.client.SpaceShellManager;
import cn.rbq108.nextboundarycornerstone.client.SpaceShellProxy;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.model.BedrockAmmoModel;
import com.tacz.guns.client.model.functional.ShellRender;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f; // 【极其重要】必须是 joml 下的 Vector3f！
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ShellRender.class)
public class MixinShellRender {

    @Inject(method = "addShell", at = @At("HEAD"), cancellable = true, remap = false)
    private void nextboundary$interceptShell(Vector3f randomVelocity, CallbackInfo ci) {
        // === 侦测器开始 ===
        System.out.println("[NextBoundary Debug] 开火！TACZ 正在尝试抛壳...");
        System.out.println("[NextBoundary Debug] 当前低重力状态为: " + GlobalVariables.B_LowGravity);

        if (!GlobalVariables.B_LowGravity) {
            return; // 没开失重，放行！
        }

        ci.cancel();
        System.out.println("[NextBoundary Debug] 成功拦截！已将弹壳转移至空间站管理器！");
        // === 侦测器结束 ===

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack mainHandItem = mc.player.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(mainHandItem);
        if (iGun == null) return;

        ResourceLocation gunId = iGun.getGunId(mainHandItem);
        TimelessAPI.getClientGunIndex(gunId).ifPresent(gunIndex -> {
            GunData gunData = gunIndex.getGunData();
            TimelessAPI.getClientAmmoIndex(gunData.getAmmoId()).ifPresent(ammoIndex -> {
                BedrockAmmoModel model = ammoIndex.getShellModel();
                ResourceLocation texture = ammoIndex.getShellTextureLocation();

                if (model == null || texture == null) return;

                SpaceShellProxy proxy = new SpaceShellProxy();
                proxy.model = model;
                proxy.texture = texture;
                proxy.spawnTime = System.currentTimeMillis();

                Vec3 eyePos = mc.player.getEyePosition();
                Vec3 lookVec = mc.player.getLookAngle();
                Vec3 rightVec = lookVec.cross(new Vec3(0, 1, 0)).normalize();

                proxy.startPosition = eyePos.add(lookVec.scale(0.5)).add(rightVec.scale(0.3)).add(0, -0.2, 0);

                Quaternionf camQuat = new Quaternionf(GlobalVariables.currentQuat);
                proxy.startRotation = new Quaternionf(camQuat);

                TimelessAPI.getGunDisplay(mainHandItem).ifPresent(display -> {
                    if (display.getShellEjection() != null) {
                        Vector3f initialVel = display.getShellEjection().getInitialVelocity();
                        Vector3f localVel = new Vector3f(
                                -(initialVel.x() + randomVelocity.x()),
                                -(initialVel.y() + randomVelocity.y()),
                                initialVel.z() + randomVelocity.z()
                        ).mul(0.3f);

                        camQuat.transform(localVel);
                        proxy.velocity = localVel;
                        proxy.angularVelocity = display.getShellEjection().getAngularVelocity();
                    }
                });

                SpaceShellManager.SHELLS.add(proxy);
            });
        });
    }
}

//package cn.rbq108.nextboundarycornerstone.Mixin;
//
//import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
//import cn.rbq108.nextboundarycornerstone.client.SpaceShellManager;
//import cn.rbq108.nextboundarycornerstone.client.SpaceShellProxy;
//import com.tacz.guns.client.model.BedrockAmmoModel;
//import com.tacz.guns.client.model.functional.ShellRender;
//import com.tacz.guns.client.resource.GunDisplayInstance;
//import net.minecraft.client.Minecraft;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.world.phys.Vec3;
//import org.joml.Quaternionf;
//import org.joml.Vector3f;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//@Mixin(value = ShellRender.class)
//public class MixinShellRender {
//
//    // 核心修改：请注意看这里的参数列表！
//    // 之前你发给我的旧版代码里，renderSingleShell 是有 BedrockAmmoModel 和 ResourceLocation 的
//    // 说明这两个参数必然是在添加时被传入或者在渲染时被调用的。
//    // 根据 TACZ 源码，addShell 方法的真实参数大概率是这样：
//    @Inject(method = "addShell", at = @At("HEAD"), cancellable = true, remap = false)
//    private void nextboundary$stealAndConvert(
//            GunDisplayInstance display,
//            BedrockAmmoModel model,
//            ResourceLocation location,
//            ShellRender.Data data,
//            CallbackInfo ci
//    ) {
//        if (!GlobalVariables.B_LowGravity) {
//            return;
//        }
//
//        ci.cancel();
//
//        if (data == null || data.normal == null || data.pose == null) return;
//
//        Minecraft mc = Minecraft.getInstance();
//        if (mc.player == null) return;
//
//        SpaceShellProxy proxy = new SpaceShellProxy();
//
//        // 终于拿到它们了！
//        proxy.model = model;
//        proxy.texture = location;
//
//        proxy.spawnTime = System.currentTimeMillis();
//        proxy.livingTime = 30.0f;
//
//        // 后面的坐标计算逻辑保持不变
//        Vector3f localOffset = data.pose.getTranslation(new Vector3f());
//        Quaternionf camQuat = new Quaternionf(GlobalVariables.currentQuat);
//        camQuat.transform(localOffset);
//
//        Vec3 eyePos = mc.player.getEyePosition();
//        proxy.worldPosition = eyePos.add(localOffset.x, localOffset.y, localOffset.z);
//
//        Quaternionf localRot = data.pose.getNormalizedRotation(new Quaternionf());
//        proxy.rotation = new Quaternionf(camQuat).mul(localRot);
//
//        Vector3f initialVel = display.getShellEjection().getInitialVelocity();
//        Vector3f randomOffset = data.randomOffset;
//        Vector3f localVel = new Vector3f(
//                -(initialVel.x() + randomOffset.x()),
//                -(initialVel.y() + randomOffset.y()),
//                initialVel.z() + randomOffset.z()
//        ).mul(0.3f);
//
//        camQuat.transform(localVel);
//        proxy.velocity = localVel;
//
//        proxy.angularVelocity = display.getShellEjection().getAngularVelocity();
//
//        SpaceShellManager.SHELLS.add(proxy);
//    }
//}