package cn.rbq108.nextboundarycornerstone.rendering;

import cn.rbq108.nextboundarycornerstone.main;
import cn.rbq108.nextboundarycornerstone.ServeMiao.communication.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Quaternionf;


import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class OtherPlayer {

    private static final WeakHashMap<Player, Float[]> SAVED_ROTS = new WeakHashMap<>();

    private static final WeakHashMap<UUID, Quaternionf> SMOOTH_BODY_QUATS = new WeakHashMap<>();
    private static final WeakHashMap<UUID, Quaternionf> SMOOTH_BODY_QUATS_O = new WeakHashMap<>();

    private static final WeakHashMap<UUID, Quaternionf> NET_HEAD_QUATS = new WeakHashMap<>();
    private static final WeakHashMap<UUID, Quaternionf> NET_HEAD_QUATS_O = new WeakHashMap<>();

    public static class RotationData {
        public Quaternionf headQuatRender = new Quaternionf();
        public Quaternionf bodyQuatRender = new Quaternionf();
    }
    public static final ConcurrentHashMap<UUID, RotationData> ROTATION_SYNC_MAP = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() && player != Minecraft.getInstance().player) {
            UUID id = player.getUUID();
            if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(id, false)) {
                Quaternionf targetQuat = NetworkHandler.REMOTE_ROTATIONS.get(id);
                if (targetQuat == null) return;

                Quaternionf currentHead = NET_HEAD_QUATS.computeIfAbsent(id, k -> new Quaternionf(targetQuat));
                NET_HEAD_QUATS_O.put(id, new Quaternionf(currentHead));
                currentHead.set(targetQuat);

                Quaternionf currentBody = SMOOTH_BODY_QUATS.computeIfAbsent(id, k -> new Quaternionf(targetQuat));
                SMOOTH_BODY_QUATS_O.put(id, new Quaternionf(currentBody));
                currentBody.slerp(targetQuat, 0.15f);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player == Minecraft.getInstance().player) return;

        UUID id = player.getUUID();
        if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(id, false)) {
            Quaternionf headQuatNet = NetworkHandler.REMOTE_ROTATIONS.get(id);

            if (headQuatNet != null) {

                //float centerY = player.getBbHeight() * 0.5f;//这行是定义玩家模型偏离碰撞箱高度用的，用不着了


                float frameTime = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);

                Quaternionf prevHead = NET_HEAD_QUATS_O.getOrDefault(id, headQuatNet);
                Quaternionf currHead = NET_HEAD_QUATS.getOrDefault(id, headQuatNet);
                Quaternionf smoothHeadQuatRender = new Quaternionf(prevHead).slerp(currHead, frameTime);

                Quaternionf prevBody = SMOOTH_BODY_QUATS_O.getOrDefault(id, headQuatNet);
                Quaternionf currBody = SMOOTH_BODY_QUATS.getOrDefault(id, headQuatNet);
                Quaternionf smoothBodyQuatRender = new Quaternionf(prevBody).slerp(currBody, frameTime);

                float currentHitboxHeight = player.getBbHeight();
                float centerY = currentHitboxHeight * 0.5f;

                // 先把直立的存起来防止干扰到后面的计算
                //
                event.getPoseStack().pushPose();

                // 更改旋转轴心
                // 把旋转轴心从脚底 (0) 拔高到身体重心
                event.getPoseStack().translate(0.0D, centerY, 0.0D);
                //执行旋转
                event.getPoseStack().mulPose(smoothBodyQuatRender);
                // 旋转完后再把轴心降回脚底，居中
                event.getPoseStack().translate(0.0D, -centerY, 0.0D);




                RotationData data = ROTATION_SYNC_MAP.computeIfAbsent(id, k -> new RotationData());
                data.headQuatRender = smoothHeadQuatRender;
                data.bodyQuatRender = smoothBodyQuatRender;

                SAVED_ROTS.put(player, new Float[]{
                        player.yBodyRot, player.yBodyRotO,
                        player.yHeadRot, player.yHeadRotO,
                        player.getXRot(), player.xRotO,
                        player.getYRot(), player.yRotO
                });
                player.yBodyRot = 0; player.yBodyRotO = 0;
                player.yHeadRot = 0; player.yHeadRotO = 0;
                player.setXRot(0); player.xRotO = 0;
                player.setYRot(0); player.yRotO = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();

        // 把 popPose 移到冷冻柜的判断内部，防止按F3切第三人称的时候崩溃
        // 只有在 Pre 阶段真正执行了 pushPose（即成功拿到网络数据）的玩家，
        // 他的数据才会被存进 SAVED_ROTS。
        // 所以只要进得来这个 if，就说明之前一定 pushPose 了，这时候 popPose应该没问题
        if (SAVED_ROTS.containsKey(player)) {


            event.getPoseStack().popPose();


            Float[] rots = SAVED_ROTS.get(player);
            player.yBodyRot = rots[0]; player.yBodyRotO = rots[1];
            player.yHeadRot = rots[2]; player.yHeadRotO = rots[3];
            player.setXRot(rots[4]); player.xRotO = rots[5];
            player.setYRot(rots[6]); player.yRotO = rots[7];

            SAVED_ROTS.remove(player);
        }
    }
    /*public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();


        if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(player.getUUID(), false)) {
            event.getPoseStack().popPose();
        }

        if (SAVED_ROTS.containsKey(player)) {
            Float[] rots = SAVED_ROTS.get(player);
            player.yBodyRot = rots[0]; player.yBodyRotO = rots[1];
            player.yHeadRot = rots[2]; player.yHeadRotO = rots[3];
            player.setXRot(rots[4]); player.xRotO = rots[5];
            player.setYRot(rots[6]); player.yRotO = rots[7];
            SAVED_ROTS.remove(player);
        }
    }*/
}


/*这个不错，但是碰撞箱错了，顺便修一下叭

package cn.rbq108.test.rendering;

import cn.rbq108.test.main;
import cn.rbq108.test.ServeMiao.communication.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Quaternionf;

import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class OtherPlayer {

    private static final WeakHashMap<Player, Float[]> SAVED_ROTS = new WeakHashMap<>();

    private static final WeakHashMap<UUID, Quaternionf> SMOOTH_BODY_QUATS = new WeakHashMap<>();
    private static final WeakHashMap<UUID, Quaternionf> SMOOTH_BODY_QUATS_O = new WeakHashMap<>();


    private static final WeakHashMap<UUID, Quaternionf> NET_HEAD_QUATS = new WeakHashMap<>();
    private static final WeakHashMap<UUID, Quaternionf> NET_HEAD_QUATS_O = new WeakHashMap<>();

    public static class RotationData {
        public Quaternionf headQuatRender = new Quaternionf();
        public Quaternionf bodyQuatRender = new Quaternionf();
    }
    public static final ConcurrentHashMap<UUID, RotationData> ROTATION_SYNC_MAP = new ConcurrentHashMap<>();


    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() && player != Minecraft.getInstance().player) {
            UUID id = player.getUUID();
            if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(id, false)) {
                Quaternionf targetQuat = NetworkHandler.REMOTE_ROTATIONS.get(id);
                if (targetQuat == null) return;


                Quaternionf currentHead = NET_HEAD_QUATS.computeIfAbsent(id, k -> new Quaternionf(targetQuat));
                NET_HEAD_QUATS_O.put(id, new Quaternionf(currentHead)); // 把当前当做“上一刻”
                currentHead.set(targetQuat); // 更新为网络最新的瞬间目标


                Quaternionf currentBody = SMOOTH_BODY_QUATS.computeIfAbsent(id, k -> new Quaternionf(targetQuat));
                SMOOTH_BODY_QUATS_O.put(id, new Quaternionf(currentBody));
                currentBody.slerp(targetQuat, 0.15f); // 0.15f 决定了身体追赶的缓慢程度
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player == Minecraft.getInstance().player) return;

        UUID id = player.getUUID();
        if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(id, false)) {
            Quaternionf headQuatNet = NetworkHandler.REMOTE_ROTATIONS.get(id);

            if (headQuatNet != null) {
                float frameTime = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);


                Quaternionf prevHead = NET_HEAD_QUATS_O.getOrDefault(id, headQuatNet);
                Quaternionf currHead = NET_HEAD_QUATS.getOrDefault(id, headQuatNet);
                Quaternionf smoothHeadQuatRender = new Quaternionf(prevHead).slerp(currHead, frameTime);


                Quaternionf prevBody = SMOOTH_BODY_QUATS_O.getOrDefault(id, headQuatNet);
                Quaternionf currBody = SMOOTH_BODY_QUATS.getOrDefault(id, headQuatNet);
                Quaternionf smoothBodyQuatRender = new Quaternionf(prevBody).slerp(currBody, frameTime);


                event.getPoseStack().mulPose(smoothBodyQuatRender);


                RotationData data = ROTATION_SYNC_MAP.computeIfAbsent(id, k -> new RotationData());
                data.headQuatRender = smoothHeadQuatRender;
                data.bodyQuatRender = smoothBodyQuatRender;

                SAVED_ROTS.put(player, new Float[]{
                        player.yBodyRot, player.yBodyRotO,
                        player.yHeadRot, player.yHeadRotO,
                        player.getXRot(), player.xRotO,
                        player.getYRot(), player.yRotO
                });
                player.yBodyRot = 0; player.yBodyRotO = 0;
                player.yHeadRot = 0; player.yHeadRotO = 0;
                player.setXRot(0); player.xRotO = 0;
                player.setYRot(0); player.yRotO = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        if (SAVED_ROTS.containsKey(player)) {
            Float[] rots = SAVED_ROTS.get(player);
            player.yBodyRot = rots[0]; player.yBodyRotO = rots[1];
            player.yHeadRot = rots[2]; player.yHeadRotO = rots[3];
            player.setXRot(rots[4]); player.xRotO = rots[5];
            player.setYRot(rots[6]); player.yRotO = rots[7];
            SAVED_ROTS.remove(player);
        }
    }
}*/


/*这个测试完成了，但是由于没有平滑效果还是换一板叭
package cn.rbq108.test.rendering;

import cn.rbq108.test.main;
import cn.rbq108.test.ServeMiao.communication.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Quaternionf;

import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class OtherPlayer {

    private static final WeakHashMap<Player, Float[]> SAVED_ROTS = new WeakHashMap<>();

    private static final WeakHashMap<UUID, Quaternionf> SMOOTH_BODY_QUATS = new WeakHashMap<>();
    private static final WeakHashMap<UUID, Quaternionf> SMOOTH_BODY_QUATS_O = new WeakHashMap<>();

    public static class RotationData {
        public Quaternionf headQuatRender = new Quaternionf(); // 瞬时脑袋指向
        public Quaternionf bodyQuatRender = new Quaternionf(); // 插值身体指向
    }
    public static final ConcurrentHashMap<UUID, RotationData> ROTATION_SYNC_MAP = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() && player != Minecraft.getInstance().player) {
            UUID id = player.getUUID();
            if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(id, false)) {
                // 这是网络发来的 target 姿态（脑袋指向）
                Quaternionf targetQuat = NetworkHandler.REMOTE_ROTATIONS.get(id);
                if (targetQuat == null) return;

                Quaternionf currentBodyQuat = SMOOTH_BODY_QUATS.computeIfAbsent(id, k -> new Quaternionf(targetQuat));

                SMOOTH_BODY_QUATS_O.put(id, new Quaternionf(currentBodyQuat));

                currentBodyQuat.slerp(targetQuat, 0.15f);//0.15是追踪速度
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player == Minecraft.getInstance().player) return;

        UUID id = player.getUUID();
        if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(id, false)) {
            // 这是网络发来的目标姿态（脑袋指向）
            Quaternionf headQuatNet = NetworkHandler.REMOTE_ROTATIONS.get(id);

            if (headQuatNet != null) {

                //float frameTime = Minecraft.getInstance().getFrameTime();
                float frameTime = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
                Quaternionf prevBody = SMOOTH_BODY_QUATS_O.getOrDefault(id, headQuatNet);
                Quaternionf currBody = SMOOTH_BODY_QUATS.getOrDefault(id, headQuatNet);


                Quaternionf smoothBodyQuatRender = new Quaternionf(prevBody).slerp(currBody, frameTime);


                event.getPoseStack().mulPose(smoothBodyQuatRender);


                RotationData data = ROTATION_SYNC_MAP.computeIfAbsent(id, k -> new RotationData());
                data.headQuatRender = headQuatNet;
                data.bodyQuatRender = smoothBodyQuatRender;


                SAVED_ROTS.put(player, new Float[]{
                        player.yBodyRot, player.yBodyRotO,
                        player.yHeadRot, player.yHeadRotO,
                        player.getXRot(), player.xRotO,
                        player.getYRot(), player.yRotO
                });
                player.yBodyRot = 0; player.yBodyRotO = 0;
                player.yHeadRot = 0; player.yHeadRotO = 0;
                player.setXRot(0); player.xRotO = 0;
                player.setYRot(0); player.yRotO = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {

        Player player = event.getEntity();
        if (SAVED_ROTS.containsKey(player)) {
            Float[] rots = SAVED_ROTS.get(player);
            player.yBodyRot = rots[0]; player.yBodyRotO = rots[1];
            player.yHeadRot = rots[2]; player.yHeadRotO = rots[3];
            player.setXRot(rots[4]); player.xRotO = rots[5];
            player.setYRot(rots[6]); player.yRotO = rots[7];
            SAVED_ROTS.remove(player);
        }
    }
}*/



/*角度正确，但是四肢脱臼
package cn.rbq108.test.rendering;

import cn.rbq108.test.main;
import cn.rbq108.test.ServeMiao.communication.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Quaternionf;

import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class OtherPlayer {

    private static final WeakHashMap<Player, Float[]> SAVED_ROTS = new WeakHashMap<>();
    private static final WeakHashMap<UUID, Quaternionf> SMOOTH_HEAD_QUATS = new WeakHashMap<>();

    private static final WeakHashMap<UUID, Float> CUSTOM_BODY_YAWS = new WeakHashMap<>();
    private static final WeakHashMap<UUID, Float> CUSTOM_BODY_YAWS_O = new WeakHashMap<>();

    public static final ConcurrentHashMap<UUID, Float> BODY_TWIST_MAP = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        if (player.level().isClientSide() && player != Minecraft.getInstance().player) {
            UUID id = player.getUUID();
            if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(id, false)) {
                float targetHeadYaw = player.yHeadRot;
                float currentYaw = CUSTOM_BODY_YAWS.getOrDefault(id, targetHeadYaw);


                CUSTOM_BODY_YAWS_O.put(id, currentYaw);

                CUSTOM_BODY_YAWS.put(id, Mth.rotLerp(0.15f, currentYaw, targetHeadYaw));
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player == Minecraft.getInstance().player) return;

        UUID id = player.getUUID();
        if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(id, false)) {
            Quaternionf targetQuat = NetworkHandler.REMOTE_ROTATIONS.get(id);

            if (targetQuat != null) {

                Quaternionf headQuat = SMOOTH_HEAD_QUATS.computeIfAbsent(id, k -> new Quaternionf(targetQuat));
                headQuat.slerp(targetQuat, 0.4f);
                event.getPoseStack().mulPose(headQuat);


                float pt = event.getPartialTick(); // 如果 1.21.1 报错，请改成 Minecraft.getInstance().getFrameTime()
                float prevBody = CUSTOM_BODY_YAWS_O.getOrDefault(id, player.yHeadRotO);
                float currBody = CUSTOM_BODY_YAWS.getOrDefault(id, player.yHeadRot);

                float smoothBody = Mth.rotLerp(pt, prevBody, currBody);
                float smoothHead = Mth.rotLerp(pt, player.yHeadRotO, player.yHeadRot);


                float twist = smoothBody - smoothHead;
                BODY_TWIST_MAP.put(id, twist);


                SAVED_ROTS.put(player, new Float[]{
                        player.yBodyRot, player.yBodyRotO, player.yHeadRot, player.yHeadRotO,
                        player.getXRot(), player.xRotO, player.getYRot(), player.yRotO
                });

                player.yBodyRot = 0; player.yBodyRotO = 0;
                player.yHeadRot = 0; player.yHeadRotO = 0;
                player.setXRot(0); player.xRotO = 0;
                player.setYRot(0); player.yRotO = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {

        Player player = event.getEntity();
        if (SAVED_ROTS.containsKey(player)) {
            Float[] rots = SAVED_ROTS.get(player);
            player.yBodyRot = rots[0]; player.yBodyRotO = rots[1];
            player.yHeadRot = rots[2]; player.yHeadRotO = rots[3];
            player.setXRot(rots[4]); player.xRotO = rots[5];
            player.setYRot(rots[6]); player.yRotO = rots[7];
            SAVED_ROTS.remove(player);
        }
    }
}*/


/*全是bug吐了
import cn.rbq108.test.main;
import cn.rbq108.test.ServeMiao.communication.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.joml.Quaternionf;

import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class OtherPlayer {


    private static final WeakHashMap<Player, Float[]> SAVED_ROTS = new WeakHashMap<>();


    private static final WeakHashMap<UUID, Quaternionf> SMOOTH_HEAD_QUATS = new WeakHashMap<>();


    private static final WeakHashMap<UUID, Float> CUSTOM_BODY_YAWS = new WeakHashMap<>();


    public static final ConcurrentHashMap<UUID, Float> BODY_TWIST_MAP = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player == Minecraft.getInstance().player) return;

        UUID id = player.getUUID();
        if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(id, false)) {
            Quaternionf targetQuat = NetworkHandler.REMOTE_ROTATIONS.get(id);

            if (targetQuat != null) {

                Quaternionf headQuat = SMOOTH_HEAD_QUATS.computeIfAbsent(id, k -> new Quaternionf(targetQuat));
                headQuat.slerp(targetQuat, 0.4f);
                event.getPoseStack().mulPose(headQuat);


                float realHeadYaw = player.yHeadRot;
                float currentBodyYaw = CUSTOM_BODY_YAWS.computeIfAbsent(id, k -> realHeadYaw);


                currentBodyYaw = Mth.rotLerp(0.1f, currentBodyYaw, realHeadYaw);
                CUSTOM_BODY_YAWS.put(id, currentBodyYaw);


                float twist = currentBodyYaw - realHeadYaw;
                BODY_TWIST_MAP.put(id, twist);


                SAVED_ROTS.put(player, new Float[]{
                        player.yBodyRot, player.yBodyRotO,
                        player.yHeadRot, player.yHeadRotO,
                        player.getXRot(), player.xRotO,
                        player.getYRot(), player.yRotO
                });

                player.yBodyRot = 0; player.yBodyRotO = 0;
                player.yHeadRot = 0; player.yHeadRotO = 0;
                player.setXRot(0); player.xRotO = 0;
                player.setYRot(0); player.yRotO = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        if (SAVED_ROTS.containsKey(player)) {
            Float[] rots = SAVED_ROTS.get(player);
            player.yBodyRot = rots[0]; player.yBodyRotO = rots[1];
            player.yHeadRot = rots[2]; player.yHeadRotO = rots[3];
            player.setXRot(rots[4]); player.xRotO = rots[5];
            player.setYRot(rots[6]); player.yRotO = rots[7];
            SAVED_ROTS.remove(player);
        }
    }
}*/

/*身体没有做独立旋转，铁板一块看着难受

package cn.rbq108.test.rendering;

import cn.rbq108.test.main;
import cn.rbq108.test.ServeMiao.communication.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.joml.Quaternionf;

import java.util.UUID;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class OtherPlayer {


    private static final WeakHashMap<Player, Float[]> SAVED_ROTS = new WeakHashMap<>();


    private static final WeakHashMap<UUID, Quaternionf> SMOOTH_QUATS = new WeakHashMap<>();

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player == Minecraft.getInstance().player) return;

        UUID id = player.getUUID();
        if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(id, false)) {


            Quaternionf targetQuat = NetworkHandler.REMOTE_ROTATIONS.get(id);

            if (targetQuat != null) {

                Quaternionf currentSmoothQuat = SMOOTH_QUATS.computeIfAbsent(id, k -> new Quaternionf(targetQuat));


                currentSmoothQuat.slerp(targetQuat, 0.2f);


                event.getPoseStack().mulPose(currentSmoothQuat);


                SAVED_ROTS.put(player, new Float[]{
                        player.yBodyRot, player.yBodyRotO,
                        player.yHeadRot, player.yHeadRotO,
                        player.getXRot(), player.xRotO,
                        player.getYRot(), player.yRotO
                });
                player.yBodyRot = 0; player.yBodyRotO = 0;
                player.yHeadRot = 0; player.yHeadRotO = 0;
                player.setXRot(0); player.xRotO = 0;
                player.setYRot(0); player.yRotO = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        if (SAVED_ROTS.containsKey(player)) {
            Float[] rots = SAVED_ROTS.get(player);
            player.yBodyRot = rots[0]; player.yBodyRotO = rots[1];
            player.yHeadRot = rots[2]; player.yHeadRotO = rots[3];
            player.setXRot(rots[4]); player.xRotO = rots[5];
            player.setYRot(rots[6]); player.yRotO = rots[7];
            SAVED_ROTS.remove(player);
        }
    }
}*/

/*成功了，但是还没有平滑效果

package cn.rbq108.test.rendering;

import cn.rbq108.test.main;
import cn.rbq108.test.ServeMiao.communication.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.joml.Quaternionf;

import java.util.UUID;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class OtherPlayer {

    private static final WeakHashMap<Player, Float[]> SAVED_ROTS = new WeakHashMap<>();

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();

        if (player == Minecraft.getInstance().player) return;

        UUID id = player.getUUID();
        if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(id, false)) {
            Quaternionf remoteQuat = NetworkHandler.REMOTE_ROTATIONS.get(id);

            if (remoteQuat != null) {

                event.getPoseStack().mulPose(remoteQuat);


                SAVED_ROTS.put(player, new Float[]{
                        player.yBodyRot, player.yBodyRotO,
                        player.yHeadRot, player.yHeadRotO,
                        player.getXRot(), player.xRotO,
                        player.getYRot(), player.yRotO // 包含总偏航角
                });


                player.yBodyRot = 0;
                player.yBodyRotO = 0;
                player.yHeadRot = 0;
                player.yHeadRotO = 0;
                player.setXRot(0);
                player.xRotO = 0;
                player.setYRot(0);
                player.yRotO = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();

        if (SAVED_ROTS.containsKey(player)) {
            Float[] rots = SAVED_ROTS.get(player);
            player.yBodyRot = rots[0];
            player.yBodyRotO = rots[1];
            player.yHeadRot = rots[2];
            player.yHeadRotO = rots[3];
            player.setXRot(rots[4]);
            player.xRotO = rots[5];
            player.setYRot(rots[6]);
            player.yRotO = rots[7];

            SAVED_ROTS.remove(player);
        }
    }
}*/






/*下面这一坨是用坏的mixin
package cn.rbq108.test.rendering;

import cn.rbq108.test.main;
import cn.rbq108.test.ServeMiao.communication.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.joml.Quaternionf;
import java.util.UUID;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class OtherPlayer {

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();

        if (player == Minecraft.getInstance().player) return;

        UUID id = player.getUUID();
        if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(id, false)) {
            Quaternionf remoteQuat = NetworkHandler.REMOTE_ROTATIONS.get(id);

            if (remoteQuat != null) {

                event.getPoseStack().mulPose(remoteQuat);
            }
        }
    }
}*/


//以下是废弃掉的原版其他玩家渲染机制，现已改为mixin强行接管

/*package cn.rbq108.test.rendering;

import cn.rbq108.test.main;
import cn.rbq108.test.ServeMiao.communication.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.joml.Quaternionf;

import java.util.UUID;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public class OtherPlayer {


    private static final WeakHashMap<Player, Float[]> SAVED_ROTS = new WeakHashMap<>();


    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
            if (player == Minecraft.getInstance().player) {
                return;
        }

        UUID id = player.getUUID();
        if (NetworkHandler.REMOTE_GRAVITY_STATES.getOrDefault(id, false)) {
            Quaternionf remoteQuat = NetworkHandler.REMOTE_ROTATIONS.get(id);

            if (remoteQuat != null) {

                event.getPoseStack().mulPose(remoteQuat);


                float bodyYaw = player.yBodyRot;
                float bodyYawO = player.yBodyRotO;
                float headYaw = player.yHeadRot;
                float headYawO = player.yHeadRotO;
                float pitch = player.getXRot();
                float pitchO = player.xRotO;


                SAVED_ROTS.put(player, new Float[]{bodyYaw, bodyYawO, headYaw, headYawO, pitch, pitchO});


                player.yBodyRot = bodyYaw - headYaw;
                player.yBodyRotO = bodyYawO - headYawO;


                player.yHeadRot = 0;
                player.yHeadRotO = 0;


                player.setXRot(0);
                player.xRotO = 0;

                /*float bodyYaw = player.yBodyRot;
                float bodyYawO = player.yBodyRotO;
                float headYaw = player.yHeadRot;
                float headYawO = player.yHeadRotO;


                SAVED_ROTS.put(player, new Float[]{bodyYaw, bodyYawO, headYaw, headYawO});


                player.yBodyRot = 0;
                player.yBodyRotO = 0;


                player.yHeadRot = headYaw - bodyYaw;
                player.yHeadRotO = headYawO - bodyYawO;
            }
        }
    }

  //把数据还给原版引擎
    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();


        if (SAVED_ROTS.containsKey(player)) {
            Float[] rots = SAVED_ROTS.get(player);
            player.yBodyRot = rots[0];
            player.yBodyRotO = rots[1];
            player.yHeadRot = rots[2];
            player.yHeadRotO = rots[3];
            player.setXRot(rots[4]);
            player.xRotO = rots[5];

            Float[] rots = SAVED_ROTS.get(player);
            player.yBodyRot = rots[0];
            player.yBodyRotO = rots[1];
            player.yHeadRot = rots[2];
            player.yHeadRotO = rots[3];


            SAVED_ROTS.remove(player);
        }
    }
}*/