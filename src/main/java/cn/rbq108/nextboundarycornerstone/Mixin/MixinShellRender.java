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
        System.out.println("========== 抛壳拦截测试 ==========");
        System.out.println("1. 成功阻断 TACZ 原版抛壳！");

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            System.out.println("失败：玩家实体为空！");
            return;
        }

        ItemStack mainHandItem = mc.player.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(mainHandItem);
        if (iGun == null) {
            System.out.println("失败：玩家手里拿的不是 TACZ 枪械！");
            return;
        }

        ResourceLocation gunId = iGun.getGunId(mainHandItem);
        System.out.println("2. 成功获取枪械 ID: " + gunId);

        TimelessAPI.getClientGunIndex(gunId).ifPresentOrElse(gunIndex -> {
            GunData gunData = gunIndex.getGunData();
            System.out.println("3. 成功获取子弹 ID: " + gunData.getAmmoId());

            TimelessAPI.getClientAmmoIndex(gunData.getAmmoId()).ifPresentOrElse(ammoIndex -> {
                BedrockAmmoModel model = ammoIndex.getShellModel();
                ResourceLocation texture = ammoIndex.getShellTextureLocation();

                if (model == null) {
                    System.out.println("失败：这颗子弹没有抛壳模型！");
                    return;
                }
                if (texture == null) {
                    System.out.println("失败：这颗子弹没有抛壳贴图！");
                    return;
                }

                System.out.println("4. 成功获取模型与贴图，准备生成！");

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

                TimelessAPI.getGunDisplay(mainHandItem).ifPresentOrElse(display -> {
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
                        System.out.println("5. 物理参数计算完毕！");
                    } else {
                        System.out.println("警告：枪械没有配置抛壳速度，将使用默认零速度！");
                    }
                }, () -> System.out.println("警告：获取不到 Display！"));

                SpaceShellManager.SHELLS.add(proxy);
                System.out.println("6. 大功告成！已成功将弹壳推入渲染池！当前池内弹壳数量: " + SpaceShellManager.SHELLS.size());

            }, () -> System.out.println("失败：获取不到 Ammo Index (客户端子弹数据未加载)！"));
        }, () -> System.out.println("失败：获取不到 Gun Index (客户端枪械数据未加载)！"));

        System.out.println("==================================");
    }
}