

/*

这坨屎山不要啦！

package cn.rbq108.test.body; // 确保包名正确

// --- 必需的 Import 语句 ---
import cn.rbq108.test.api.RollEntity;
import cn.rbq108.test.camera.CameraManager;
import cn.rbq108.test.main;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.client.event.RenderPlayerEvent;

/**
 * 这个类专门负责处理玩家模型的渲染事件。
 * 它的职责是根据 CameraManager 中的状态来旋转玩家的身体。
 /
@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class PlayerRenderEvents {

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        var minecraft = Minecraft.getInstance();
        var player = event.getEntity();

        // 只影响自己的玩家模型，并且只在第三人称视角下生效
        if (minecraft.player != null && player.equals(minecraft.player) && !minecraft.options.getCameraType().isFirstPerson()) {
            var poseStack = event.getPoseStack();
            poseStack.pushPose(); // 保存当前状态

            // 获取平滑的角度
            float partialTicks = event.getPartialTick();
            float pitch = CameraManager.prevPitch + (CameraManager.currentPitch - CameraManager.prevPitch) * partialTicks;
            float yaw = CameraManager.prevYaw + (CameraManager.currentYaw - CameraManager.prevYaw) * partialTicks;

            float roll = 0.0f;
            if (player instanceof RollEntity) {
                roll = ((RollEntity) player).doABarrelRoll$getRoll(partialTicks); // 从Mixin获取平滑的roll
            }

            // 让旋转围绕身体中心，而不是脚底
            poseStack.translate(0, player.getBbHeight() / 2.0, 0);

            // 应用旋转
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw - player.yBodyRot));
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
            poseStack.mulPose(Axis.ZP.rotationDegrees(roll));

            // 将坐标系移回原位
            poseStack.translate(0, -player.getBbHeight() / 2.0, 0);
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var player = event.getEntity();

        // 渲染结束后，恢复之前保存的状态，防止影响其他渲染
        if (minecraft.player != null && player.equals(minecraft.player) && !minecraft.options.getCameraType().isFirstPerson()) {
            event.getPoseStack().popPose();
        }
    }
}
*/