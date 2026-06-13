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
                proxy.spawnTime = System.nanoTime();//换成纳秒时钟喵
                //proxy.spawnTime = System.currentTimeMillis();


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

                // 新增：初始化碰撞检测点
                proxy.lastPos = proxy.startPosition;

                proxy.startRotation = new Quaternionf(bodyQuat);

                TimelessAPI.getGunDisplay(mainHandItem).ifPresent(display -> {
                    if (display.getShellEjection() != null) {

                        float B_rand1 = (float) (Math.random() - 0.5);
                        float B_rand2 = (float) (Math.random() - 0.5);
                        float B_rand3 = (float) (Math.random() - 0.5);
                        float PositiveNegative = (float) Math.random()-0.5f;
                        float PN =0f;
                        if(PositiveNegative >= 0){
                            PN =1f;
                        }else{
                            PN =-1f;

                        }


                        // ================恢复被误删的直线速度 ==================
                        //一长串等号分隔代码太好用了你知道吗
                        Vector3f initialVel = display.getShellEjection().getInitialVelocity();
                        Vector3f localVel = new Vector3f(
                                -(initialVel.x() + randomVelocity.x()/*+ B_rand * 0.2f*/),
                                -(initialVel.y() + randomVelocity.y()+ B_rand1 * 1f),
                                (initialVel.z() + randomVelocity.z() + B_rand2 * 1f)
                        ).mul(0.35f);
                        // 直线速度转入世界坐标系
                        new Quaternionf(bodyQuat).transform(localVel);
                        proxy.velocity = localVel;

                        // ==============角速度深拷贝防污染 ==================
                        Vector3f originalAngularVel = new Vector3f(display.getShellEjection().getAngularVelocity());
                        bodyQuat.transform(originalAngularVel);

                        float slowDownFactor = ((0.005f)*(1+B_rand3*1f)*PN);//转速喵,前面的0.005是基准转速，后面这一坨计算机负责随机用的
                        proxy.angularVelocity = new Vector3f(
                                originalAngularVel.x() * slowDownFactor,
                                originalAngularVel.y() * slowDownFactor,
                                originalAngularVel.z() * slowDownFactor
                        );
                    }
                });

                // ================== 防双重生成补丁 ============================
                long currentTime = System.nanoTime();
                boolean isDuplicate = false;
                for (int i = SpaceShellManager.SHELLS.size() - 1; i >= 0; i--) {
                    SpaceShellProxy existing = SpaceShellManager.SHELLS.get(i);
                    // 如果 20 毫秒内已经生成过弹壳，则判定为双黄蛋
                    if (currentTime - existing.spawnTime < 20_000_000L) {
                        isDuplicate = true;
                        break;
                    }
                }

                // ？
                if (!isDuplicate) {
                    SpaceShellManager.SHELLS.add(proxy);
                    cn.rbq108.nextboundarycornerstone.TACZ.recoil.applyWeaponBlowback();//这个负责触发后坐力啥啥啥的
                }
            }); // 结束 ammoIndex 寻找
        }); // 结束 gunIndex 寻找
    }
}