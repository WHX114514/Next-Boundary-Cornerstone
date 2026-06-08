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

                // 当前四元数
                Quaternionf bodyQuat = new Quaternionf(GlobalVariables.currentQuat);

                if (mc.options.getCameraType().isFirstPerson()) {
                    // 这是第一人称喵（原点应该是眼睛）
                    Vector3f localOffset = new Vector3f(-0.3f, -0.2f, 0.5f);
                    bodyQuat.transform(localOffset);

                    Vec3 eyePos = mc.player.getEyePosition();
                    proxy.startPosition = eyePos.add(localOffset.x, localOffset.y, localOffset.z);

                } else {
                    // 第三人称喵（原点玩家脚底，然后本地偏移经过 currentQuat 旋转到世界空间）
                    // LOCAL_RIGHT, LOCAL_UP, LOCAL_FORWARD 是相对于玩家身体坐标系的偏移
                    final float PIVOT_HEIGHT = 0.7f; // 弹壳高度微调

                    Vector3f localOffset = new Vector3f(
                            CartridgeCaseStartingPoint.LOCAL_RIGHT,
                            CartridgeCaseStartingPoint.LOCAL_UP,
                            CartridgeCaseStartingPoint.LOCAL_FORWARD
                    );
                    bodyQuat.transform(localOffset);

                    Vec3 playerPos = mc.player.position();
                    proxy.startPosition = new Vec3(
                            playerPos.x + localOffset.x,
                            playerPos.y + PIVOT_HEIGHT + localOffset.y, // PIVOT_HEIGHT 是旋转中心的世界高度
                            playerPos.z + localOffset.z
                    );
                }

                proxy.startRotation = new Quaternionf(bodyQuat);

                // 计算速度
                TimelessAPI.getGunDisplay(mainHandItem).ifPresent(display -> {
                    if (display.getShellEjection() != null) {
                        Vector3f initialVel = display.getShellEjection().getInitialVelocity();

                        // 本地速度向量（枪械定义的抛出方向，在枪的本地空间）
                        Vector3f localVel = new Vector3f(
                                -(initialVel.x() + randomVelocity.x()),
                                -(initialVel.y() + randomVelocity.y()),
                                (initialVel.z() + randomVelocity.z())
                        ).mul(0.3f);

                        // 把速度向量旋转到世界空间
                        new Quaternionf(bodyQuat).transform(localVel);//
                        proxy.velocity = localVel;




                        // 1. 获取枪械原始的角速度
                        Vector3f originalAngularVel = display.getShellEjection().getAngularVelocity();

                        // 2. 关键点：将角速度也旋转到世界空间，这样弹壳旋转轴才会跟随飞船姿态
                        // 不然在飞船翻滚时，它会按错误的局部轴旋转，导致“死锁”视觉效果
                        bodyQuat.transform(originalAngularVel);

                        // 3. 应用减速系数 (原来的 0.0019f 如果觉得慢了，可以微调)
                        float slowDownFactor = 0.0019f;
                        proxy.angularVelocity = new Vector3f(
                                originalAngularVel.x() * slowDownFactor,
                                originalAngularVel.y() * slowDownFactor,
                                originalAngularVel.z() * slowDownFactor
                        );


//                        proxy.angularVelocity = display.getShellEjection().getAngularVelocity();
//
//                        Vector3f originalAngularVel = display.getShellEjection().getAngularVelocity();
//                        // 这里乘一个系数，比如 0.2f 就是原速度的 20%，你可以根据喜好调整！
//                        float slowDownFactor = 0.0019f;
//                        proxy.angularVelocity = new Vector3f(
//                                originalAngularVel.x() * slowDownFactor,
//                                originalAngularVel.y() * slowDownFactor,
//                                originalAngularVel.z() * slowDownFactor
//                        );

                    }
                });

                SpaceShellManager.SHELLS.add(proxy);
            });
        });
    }
}