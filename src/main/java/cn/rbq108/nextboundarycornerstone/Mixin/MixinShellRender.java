package cn.rbq108.nextboundarycornerstone.Mixin;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import cn.rbq108.nextboundarycornerstone.client.CartridgeCaseStartingPoint;
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
                if (model == null || texture == null) return;

                SpaceShellProxy proxy = new SpaceShellProxy();
                proxy.model = model;
                proxy.texture = texture;
                proxy.spawnTime = System.currentTimeMillis();

                // 当前 6DoF 绝对四元数
                Quaternionf bodyQuat = new Quaternionf(GlobalVariables.currentQuat);

                if (mc.options.getCameraType().isFirstPerson()) {
                    // 第一人称（原点是眼睛）
                    Vector3f localOffset = new Vector3f(-0.3f, -0.2f, 0.5f);
                    bodyQuat.transform(localOffset);
                    Vec3 eyePos = mc.player.getEyePosition();
                    proxy.startPosition = eyePos.add(localOffset.x, localOffset.y, localOffset.z);

                } else {
                    // 第三人称：调用外部类的完美方法，括号里填你想要的身体中心高度 (比如 0.7)
                    // 它会自动把这个高度加进四元数旋转，解决你平躺时弹壳乱飞的 Bug！
                    Vector3f worldOffset = CartridgeCaseStartingPoint.getWorldSpaceOffset(0.7f);

                    Vec3 playerPos = mc.player.position();
                    proxy.startPosition = new Vec3(
                            playerPos.x + worldOffset.x,
                            playerPos.y + worldOffset.y, // 注意：不需要再硬加 PIVOT_HEIGHT 了
                            playerPos.z + worldOffset.z
                    );
                }

                proxy.startRotation = new Quaternionf(bodyQuat);

                TimelessAPI.getGunDisplay(mainHandItem).ifPresent(display -> {
                    if (display.getShellEjection() != null) {
                        // ================== 1. 恢复被误删的直线速度 ==================
                        Vector3f initialVel = display.getShellEjection().getInitialVelocity();
                        Vector3f localVel = new Vector3f(
                                -(initialVel.x() + randomVelocity.x()),
                                -(initialVel.y() + randomVelocity.y()),
                                (initialVel.z() + randomVelocity.z())
                        ).mul(0.3f);
                        // 直线速度转入世界坐标系
                        new Quaternionf(bodyQuat).transform(localVel);
                        proxy.velocity = localVel;

                        // ================== 2. 角速度深拷贝防污染 ==================
                        Vector3f originalAngularVel = new Vector3f(display.getShellEjection().getAngularVelocity());
                        bodyQuat.transform(originalAngularVel);

                        float slowDownFactor = 0.0019f;
                        proxy.angularVelocity = new Vector3f(
                                originalAngularVel.x() * slowDownFactor,
                                originalAngularVel.y() * slowDownFactor,
                                originalAngularVel.z() * slowDownFactor
                        );
                    }
                });

                // ================== 防双重生成补丁 ==================
                long currentTime = System.currentTimeMillis();
                boolean isDuplicate = false;
                for (int i = SpaceShellManager.SHELLS.size() - 1; i >= 0; i--) {
                    SpaceShellProxy existing = SpaceShellManager.SHELLS.get(i);
                    // 如果 20 毫秒内已经生成过弹壳，则判定为双黄蛋
                    if (currentTime - existing.spawnTime < 20) {
                        isDuplicate = true;
                        break;
                    }
                }

                // 【修正括号】：原来你这底下的多余括号和缺失括号都在这里修复了
                if (!isDuplicate) {
                    SpaceShellManager.SHELLS.add(proxy);
                }
            }); // 结束 ammoIndex 寻找
        }); // 结束 gunIndex 寻找
    }
}