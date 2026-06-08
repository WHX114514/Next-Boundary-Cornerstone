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
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ShellRender.class)
public class MixinShellRender {

    @Inject(method = "addShell", at = @At("HEAD"), cancellable = true, remap = false)
    private void nextboundary$interceptShell(Vector3f randomVelocity, CallbackInfo ci) {
        if (!GlobalVariables.B_LowGravity) return;

        ci.cancel();

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

                // 终极防御：少一个都不行
                if (model == null || texture == null) return;

                SpaceShellProxy proxy = new SpaceShellProxy();

                // 【绝不能省的两行】这就是为什么你上一版会爆空指针！
                proxy.model = model;
                proxy.texture = texture;
                proxy.spawnTime = System.currentTimeMillis();

                // === 核心修复：在这里一次性算死绝对起始坐标 ===
                if (mc.options.getCameraType().isFirstPerson()) {
                    Vec3 eyePos = mc.player.getEyePosition();
                    Vec3 lookVec = mc.player.getLookAngle();
                    Vec3 rightVec = lookVec.cross(new Vec3(0, 1, 0)).normalize();
                    proxy.startPosition = eyePos.add(lookVec.scale(0.5)).add(rightVec.scale(0.3)).add(0, -0.2, 0);
                } else {
                    // 第三人称：用玩家逻辑朝向算世界绝对偏移，彻底解决“不转”问题
                    float yaw = mc.player.getYRot() * (float) (Math.PI / 180.0);

                    //原本这里是填写固定死的位置，但是为了修复位置不随玩家旋转改变，固定数值改成了CartridgeCaseStartingPoint事先计算好的
                    Vector3f offsetInBodySpace = cn.rbq108.nextboundarycornerstone.client.CartridgeCaseStartingPoint.getThirdPersonOffset(mc.player);

//                    Vector3f offsetInBodySpace = new Vector3f(0.3f, 1f, 0.5f); // 微调旋钮：右, 下, 前

                    Quaternionf bodyRot = new Quaternionf().rotateY(-yaw);
                    bodyRot.transform(offsetInBodySpace);

                    Vec3 playerPos = mc.player.position();
                    proxy.startPosition = new Vec3(
                            playerPos.x + offsetInBodySpace.x,
                            playerPos.y + 1.4 + offsetInBodySpace.y, // 1.4 是胸口高度
                            playerPos.z + offsetInBodySpace.z
                    );
                }

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

                // 【唯一的一次添加】
                SpaceShellManager.SHELLS.add(proxy);
            });
        });
    }
}