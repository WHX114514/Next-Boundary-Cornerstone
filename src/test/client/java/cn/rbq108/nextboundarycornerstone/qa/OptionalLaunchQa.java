package cn.rbq108.nextboundarycornerstone.qa;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModList;
import net.minecraft.core.registries.BuiltInRegistries;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;

/** Optional-mod fixtures are reflection-only and live entirely in the QA source set. */
final class OptionalLaunchQa {
    private record Fixture(String mod, String itemClass, String suffix) {}
    private record Shot(Entity entity, Vec3 velocity, Vec3 expected, double angle, long tick) {}
    private final List<Fixture> fixtures = new ArrayList<>();
    private final ConcurrentLinkedQueue<Entity> pending = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Shot> shots = new ConcurrentLinkedQueue<>();
    private final BiConsumer<Boolean,String> check;
    private final boolean transitions=Boolean.getBoolean("cornerstone.qa.transitions");
    private int index, frame, pose=Boolean.getBoolean("cornerstone.qa.transitions")?6:0;
    private volatile Fixture active;
    private volatile Vec3 expected;
    private KeyMapping launchKey;

    OptionalLaunchQa(BiConsumer<Boolean,String> check) {
        this.check = check;
        add("rehooked", "HookItem", "iron_hook");
        add("adhooks", "LauncherItem", "");
        add("spartan_weaponry_unofficial", "ThrowingKnifeItem", "throwing_knife_iron");
        add("spartan_weaponry_unofficial", "JavelinItem", "javelin_iron");
        add("spartan_weaponry_unofficial", "BoomerangItem", "boomerang_iron");
        add("immersiveengineering", "RailgunItem", "railgun");
        add("immersiveengineering", "RevolverItem", "revolver");
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::joined);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::serverTick);
    }

    private void add(String mod, String type, String suffix) {
        if (ModList.get().isLoaded(mod)) fixtures.add(new Fixture(mod,type,suffix));
    }

    private void joined(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || active == null || expected == null) return;
        var entity = event.getEntity();
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id == null || !id.getNamespace().equals(active.mod)) return;
        var velocity = entity.getDeltaMovement();
        if (velocity.lengthSqr() < 1e-10) {pending.add(entity);return;}
        capture(entity);
    }

    private void serverTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        Entity entity;
        while ((entity=pending.poll()) != null) if (active != null && expected != null && !entity.isRemoved()) capture(entity);
        for(var shot:shots) {
            if(shot.entity.level().getGameTime()-shot.tick!=10 || active==null) continue;
            if(active.mod.equals("spartan_weaponry_unofficial") && !active.itemClass.equals("BoomerangItem"))
                check.accept(!shot.entity.isRemoved() && shot.entity.getDeltaMovement().y<shot.velocity.y,
                        active+" native downward acceleration after ten ticks");
        }
    }

    private void capture(Entity entity) {
        var velocity=entity.getDeltaMovement();
        var shooter = Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID());
        Vec3 launch = velocity;
        if (active.mod.equals("spartan_weaponry_unofficial") || active.itemClass.equals("RailgunItem")) {
            var motion = shooter.getKnownMovement();
            launch = launch.subtract(motion.x,shooter.onGround()?0:motion.y,motion.z);
        }
        double angle = Math.toDegrees(Math.acos(Math.max(-1,Math.min(1,launch.normalize().dot(expected.normalize())))));
        shots.add(new Shot(entity,velocity,expected,angle,entity.level().getGameTime()));
    }

    boolean tick() throws Exception {
        if (index == fixtures.size()) { active = null; return true; }
        var mc = Minecraft.getInstance(); var fixture = fixtures.get(index);
        mc.setWindowActive(true);
        if (frame == 0) {
            active = null; expected = null; shots.clear();
            server(player -> {
                // Clear only prior QA projectiles, never natural entities or the player.
                var old = new ArrayList<Entity>();
                player.serverLevel().getAllEntities().forEach(e -> {
                    var id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
                    if (id != null && fixtures.stream().anyMatch(f -> f.mod.equals(id.getNamespace()))) old.add(e);
                });
                old.forEach(Entity::discard);
                player.stopUsingItem(); player.getInventory().clearContent();
                player.teleportTo(0,200,0); player.setDeltaMovement(Vec3.ZERO);
                GlobalVariables.B_Vx1=GlobalVariables.B_Vy1=GlobalVariables.B_Vz1=0;
                player.setItemSlot(EquipmentSlot.CHEST,pose==7?ItemStack.EMPTY:new ItemStack(cn.rbq108.nextboundarycornerstone.main.BASIC_BACKPACK.get()));
                try { equip(player,fixture); } catch (Exception e) { throw new RuntimeException(e); }
            });
        }
        if(frame==10 && pose==8) server(player -> player.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY));
        if(frame==15 && pose>=7) {mc.player.setXRot(17);mc.player.setYRot(-67);mc.player.turn(13,-7);}
        if (frame == 20) {
            var rotations = new Quaternionf[] {new Quaternionf().rotationYXZ(.3f,.2f,0),
                    new Quaternionf().rotationYXZ(.7f,.4f,1.5707964f), new Quaternionf().rotationYXZ(-.8f,.5f,3.1415927f),
                    new Quaternionf().rotationYXZ(.7f,2.1f,-.8f), new Quaternionf().rotationYXZ(.3f,1.5707f,.8f),
                    new Quaternionf().rotationYXZ(-.8f,-1.5707f,-.5f)};
            GlobalVariables.currentQuat.set(rotations[Math.min(pose,3)]); GlobalVariables.prevQuat.set(rotations[Math.min(pose,3)]);
            if(pose==6) mc.player.setDeltaMovement(.03,.02,.01);
            if(pose<7) mc.player.turn(13,-7);
            expected = pose>=7?mc.player.getLookAngle():new Vec3(GlobalVariables.currentQuat.transform(new Vector3f(0,0,1))).normalize();
            active = fixture;
            if (fixture.mod.equals("rehooked")) {
                var key=(KeyMapping)Class.forName("com.oe.rehooked.client.KeyBindings").getField("FIRE_HOOK_KEY").get(null);
                KeyMapping.click(key.getKey());
            } else if (fixture.mod.equals("adhooks")) {
                var stateClass=Class.forName("com.endertech.minecraft.mods.adhooks.parts.HookShot$SynchedBools");
                var state=stateClass.getField("LAUNCHING").get(null);
                var binding=(Optional<?>)Class.forName("com.endertech.minecraft.mods.adhooks.client.ControlBindings")
                        .getMethod("getKeyBindingFor",InteractionHand.class,stateClass).invoke(null,InteractionHand.MAIN_HAND,state);
                launchKey=(KeyMapping)binding.orElseThrow();
                launchKey.setDown(true);
            } else {
                mc.options.keyUse.setDown(true);
                mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);
            }
        }
        if (frame == 65) {
            if (launchKey != null) {launchKey.setDown(false);launchKey=null;}
            mc.options.keyUse.setDown(false);
            if (mc.player.isUsingItem()) mc.gameMode.releaseUsingItem(mc.player);
        }
        if (++frame >= (fixture.itemClass.equals("BoomerangItem")?110:80)) {
            server(player -> {
                check.accept(!shots.isEmpty(),fixture+" pose="+pose+" actual use/release spawns projectile");
                for (var shot : shots) {
                    // IE 1.21.1 calls vanilla shoot(...,3,1): triangle spread is bounded below 1.72 degrees.
                    double tolerance=fixture.itemClass.equals("RevolverItem")?1.75:
                            fixture.mod.equals("spartan_weaponry_unofficial")?.9:fixture.mod.equals("adhooks")?.8:.01;
                    check.accept(shot.angle < tolerance,fixture+" pose="+pose+" angle="+shot.angle);
                    check.accept(Double.isFinite(shot.entity.position().lengthSqr()),fixture+" finite trajectory");
                    if(fixture.itemClass.equals("BoomerangItem")) {
                        try {
                            var returning=shot.entity.getClass().getDeclaredMethod("isBoomerangReturning");returning.setAccessible(true);
                            boolean recovered=player.getInventory().items.stream().anyMatch(s -> !s.isEmpty() && s.getItem().getClass().getSimpleName().equals("BoomerangItem"));
                            check.accept((boolean)returning.invoke(shot.entity) || shot.entity.isRemoved() && recovered,fixture+" native return/catch retained");
                        }catch(ReflectiveOperationException e){throw new RuntimeException(e);}
                    }
                    if (fixture.mod.equals("spartan_weaponry_unofficial") && !fixture.itemClass.equals("BoomerangItem")
                            && !shot.entity.isRemoved() && !shot.entity.onGround())
                        check.accept(shot.entity.getDeltaMovement().y < shot.velocity.y,fixture+" native downward acceleration retained");
                }
            });
            com.mojang.logging.LogUtils.getLogger().info("OPTIONAL_LAUNCH_QA_CASE fixture={} pose={} shots={}",fixture,pose,shots.size());
            active=null; expected=null; frame=0;
            if (++pose == (transitions?9:6)) {pose=transitions?6:0;index++;}
        }
        return false;
    }

    static void equipRehooked(Player player) throws Exception {equip(player,new Fixture("rehooked","HookItem","iron_hook"));}

    private static void equip(Player player, Fixture fixture) throws Exception {
        var item = BuiltInRegistries.ITEM.entrySet().stream().filter(e -> e.getKey().location().getNamespace().equals(fixture.mod))
                .map(Map.Entry::getValue).filter(i -> i.getClass().getSimpleName().equals(fixture.itemClass))
                .filter(i -> !fixture.mod.equals("adhooks") || !BuiltInRegistries.ITEM.getKey(i).getPath().contains("prototype"))
                .filter(i -> fixture.suffix.isEmpty() || BuiltInRegistries.ITEM.getKey(i).getPath().equals(fixture.suffix))
                .findFirst().orElseThrow(() -> new AssertionError("fixture item missing: "+fixture));
        com.mojang.logging.LogUtils.getLogger().info("OPTIONAL_QA_EQUIP {}",BuiltInRegistries.ITEM.getKey(item));
        var stack = new ItemStack(item);
        if (fixture.mod.equals("spartan_weaponry_unofficial") && !fixture.itemClass.equals("BoomerangItem"))
            stack = (ItemStack)item.getClass().getMethod("makeTabStack").invoke(item);
        if (fixture.mod.equals("rehooked")) {
            var inventory=(Optional<?>)Class.forName("top.theillusivec4.curios.api.CuriosApi")
                    .getMethod("getCuriosInventory",net.minecraft.world.entity.LivingEntity.class).invoke(null,player);
            var handler=inventory.orElseThrow();
            Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler")
                    .getMethod("setEquippedCurio",String.class,int.class,ItemStack.class).invoke(handler,"hook",0,stack);
        } else player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        if (fixture.mod.equals("adhooks")) {
            var field=item.getClass().getField("hookType");var type=field.get(item);
            Item hook=BuiltInRegistries.ITEM.stream().filter(i -> i.getClass().getName().equals("com.endertech.minecraft.mods.adhooks.items.HookItem"))
                    .filter(i -> {try{return field.getDeclaringClass()==i.getClass()?false:i.getClass().getField("hookType").get(i)==type;}catch(Exception e){return false;}}).findFirst().orElseThrow();
            player.getInventory().setItem(1,new ItemStack(hook,16));
        }
        if (fixture.itemClass.equals("RailgunItem")) {
            var energy=stack.getCapability(Capabilities.EnergyStorage.ITEM);
            if(energy==null)throw new AssertionError("railgun energy capability missing");
            energy.receiveEnergy(Integer.MAX_VALUE,false);
            player.getInventory().setItem(1,new ItemStack(BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("immersiveengineering:stick_iron")),64));
        }
        if (fixture.itemClass.equals("RevolverItem")) {
            var bullet=BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("immersiveengineering:bullet_casull"));
            var bullets=net.minecraft.core.NonNullList.withSize(8,new ItemStack(bullet));
            item.getClass().getMethod("setBullets",ItemStack.class,net.minecraft.core.NonNullList.class,boolean.class).invoke(item,stack,bullets,false);
        }
    }

    static void server(java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> action) {
        var mc=Minecraft.getInstance();var id=mc.player.getUUID();
        mc.getSingleplayerServer().submit(() -> action.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(id))).join();
    }
}
