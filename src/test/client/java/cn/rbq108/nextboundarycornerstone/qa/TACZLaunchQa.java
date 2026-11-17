package cn.rbq108.nextboundarycornerstone.qa;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import cn.rbq108.nextboundarycornerstone.main;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.function.BiConsumer;

/** Real NeoForge TaCZ reload/ADS/hip API and final joined bullet velocity. */
final class TACZLaunchQa {
    private final BiConsumer<Boolean,String> check;
    private int frame,scenario,reloads;
    private volatile boolean observing,shot;
    private volatile Vec3 launch;
    private volatile Throwable failure;

    TACZLaunchQa(BiConsumer<Boolean,String> check) {
        this.check=check;
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST,this::beforeBullet);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,this::afterBullet);
        NeoForge.EVENT_BUS.addListener((com.tacz.guns.api.event.common.GunReloadEvent event) -> {
            if(event.getLogicalSide().isClient() && event.getEntity()==Minecraft.getInstance().player)reloads++;
        });
    }

    private boolean ours(EntityJoinLevelEvent event) {
        return observing && !event.getLevel().isClientSide() && event.getEntity() instanceof EntityKineticBullet bullet
                && bullet.getOwner() instanceof Player player && player.getUUID().equals(Minecraft.getInstance().player.getUUID());
    }

    private void beforeBullet(EntityJoinLevelEvent event) {
        if(!ours(event))return;
        var bullet=(EntityKineticBullet)event.getEntity();var player=(Player)bullet.getOwner();
        var motion=player.getDeltaMovement();
        launch=bullet.getDeltaMovement().subtract(motion.x,player.onGround()?0:motion.y,motion.z);
    }

    private void afterBullet(EntityJoinLevelEvent event) {
        if(!ours(event))return;
        try {
            var bullet=(EntityKineticBullet)event.getEntity();var player=(Player)bullet.getOwner();
            var expected=new Vec3(player.getData(main.PLAYER_ROTATION).getQuaternion().transform(new Vector3f(0,0,1)));
            double dot=bullet.getDeltaMovement().normalize().dot(expected.normalize());
            check.accept(dot>((scenario&1)==0?.9999:.995),"TaCZ final joined direction scenario="+scenario+" dot="+dot);
            check.accept(bullet.getDeltaMovement().distanceTo(launch)<1e-5,"TaCZ preserves native speed/scatter and removes inertia");
            check.accept(IGunOperator.fromLivingEntity(player).getSynIsAiming()==((scenario&1)==0),"TaCZ real server ADS/hip state");
            shot=true;
        }catch(Throwable error){failure=error;}
    }

    boolean tick() throws Exception {
        if(scenario==4)return true;
        if(failure!=null)throw new AssertionError("TaCZ shot",failure);
        var mc=Minecraft.getInstance();mc.setWindowActive(true);
        int fireAt=scenario==0?160:50;
        if(frame==0) {
            observing=false;shot=false;
            OptionalLaunchQa.server(player -> {
                player.setGameMode(GameType.CREATIVE);player.teleportTo(0,200,0);
                player.setItemSlot(EquipmentSlot.CHEST,new ItemStack(main.BASIC_BACKPACK.get()));
                var gun=GunItemBuilder.create().setId(ResourceLocation.parse("tacz:ak47"))
                        .setAmmoCount(5).setAmmoInBarrel(true).setFireMode(FireMode.SEMI).build();
                player.setItemInHand(InteractionHand.MAIN_HAND,gun);
                IGunOperator.fromLivingEntity(player).draw(player::getMainHandItem);
            });
            GlobalVariables.B_Vx1=GlobalVariables.B_Vy1=GlobalVariables.B_Vz1=0;
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
        if(frame==25) {
            var view=new Quaternionf().rotationYXZ(.7f,.4f,scenario<2?1.5707964f:3.1415927f);
            GlobalVariables.currentQuat.set(view);GlobalVariables.prevQuat.set(view);mc.player.turn(13,-7);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        if(frame==35 && scenario==0) {
            TACZClientQa.verify(mc);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            IClientPlayerGunOperator.fromLocalPlayer(mc.player).reload();
        }
        if(frame==fireAt-20) {
            boolean aiming=(scenario&1)==0;
            com.tacz.guns.client.input.AimKey.AIM_KEY.setDown(aiming);
            IClientPlayerGunOperator.fromLocalPlayer(mc.player).aim(aiming);
        }
        if(frame==fireAt) {
            if(scenario==0) {
                var stack=mc.player.getMainHandItem();
                check.accept(reloads>0 && IGun.getIGunOrNull(stack).getCurrentAmmoCount(stack)>5,"TaCZ real reload completes");
            }
            observing=true;
            var result=IClientPlayerGunOperator.fromLocalPlayer(mc.player).shoot();
            if(result!=ShootResult.SUCCESS)throw new AssertionError("TaCZ real shoot="+result);
        }
        if(++frame>fireAt+30) {
            check.accept(shot,"TaCZ actual network shot scenario="+scenario);
            observing=false;
            com.tacz.guns.client.input.AimKey.AIM_KEY.setDown(false);
            IClientPlayerGunOperator.fromLocalPlayer(mc.player).aim(false);
            frame=0;scenario++;
            if(scenario==4)OptionalLaunchQa.server(player -> player.setGameMode(GameType.SURVIVAL));
        }
        return false;
    }
}
