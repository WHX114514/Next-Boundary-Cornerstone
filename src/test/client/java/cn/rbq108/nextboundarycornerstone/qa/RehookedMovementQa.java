package cn.rbq108.nextboundarycornerstone.qa;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;
import java.util.Collection;
import java.util.function.BiConsumer;

/** Rehooked's own pull and jump APIs; no substitute velocity or production adapter. */
final class RehookedMovementQa {
    private final BiConsumer<Boolean,String> check;
    private int frame, scenario;
    private Vec3 before, requested;
    private Object clientHandler;
    private Class<?> common, client;

    RehookedMovementQa(BiConsumer<Boolean,String> check) {
        this.check=check;
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,this::beforeMovement);
    }

    private Object handler() throws Exception {
        if (client==null) {
            client=Class.forName("com.oe.rehooked.handlers.hook.def.IClientPlayerHookHandler");
            common=Class.forName("com.oe.rehooked.handlers.hook.def.ICommonPlayerHookHandler");
        }
        var lazy=(net.minecraftforge.common.util.LazyOptional<?>)client.getMethod("FromPlayer",net.minecraft.world.entity.player.Player.class)
                .invoke(null,Minecraft.getInstance().player);
        return lazy.resolve().orElseThrow();
    }

    private void beforeMovement(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.START || frame<22 || frame>27 || clientHandler==null) return;
        try {
            before=Minecraft.getInstance().player.position();
            requested=(Vec3)common.getMethod("getDeltaVThisTick").invoke(clientHandler);
        } catch(Exception e) {throw new RuntimeException(e);}
    }

    boolean tick() throws Exception {
        if (!ModList.get().isLoaded("rehooked") || scenario==6) return true;
        var mc=Minecraft.getInstance();
        if(frame==0) {
            clientHandler=handler();common.getMethod("removeAllHooks").invoke(clientHandler);
            common.getMethod("setMomentum",Vec3.class).invoke(clientHandler,new Object[]{null});
            cn.rbq108.nextboundarycornerstone.VariableLibrary.debug.FORCE_LOW_GRAVITY=scenario>=3;
            OptionalLaunchQa.server(player -> {
                player.teleportTo(0,200,0);
                player.setItemSlot(EquipmentSlot.CHEST,scenario>=3?ItemStack.EMPTY:new ItemStack(cn.rbq108.nextboundarycornerstone.main.BASIC_BACKPACK.get()));
                try {OptionalLaunchQa.equipRehooked(player);}catch(Exception e){throw new RuntimeException(e);}
            });
            cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_Vx1=0;
            cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_Vy1=0;
            cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_Vz1=0;
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
        if(frame==15) {
            var key=(KeyMapping)Class.forName("com.oe.rehooked.client.KeyBindings").getField("FIRE_HOOK_KEY").get(null);
            KeyMapping.click(key.getKey());
        }
        if(frame==21) {
            var hooks=(Collection<?>)common.getMethod("getHooks").invoke(clientHandler);
            if(hooks.isEmpty()) throw new AssertionError("Rehooked movement fixture did not receive a real hook");
            var anchor=mc.player.position().add(switch(scenario%3){case 0 -> new Vec3(10,1,0);case 1 -> new Vec3(0,10,0);default -> new Vec3(0,-10,0);});
            var hook=(Entity)hooks.iterator().next();setPull(hook,anchor);
            int id=hook.getId();
            OptionalLaunchQa.server(player -> {try {setPull(player.level().getEntity(id),anchor);}catch(Exception e){throw new RuntimeException(e);}});
        }
        if(frame>=22 && frame<=27) {
            var delta=mc.player.position().subtract(before);
            // LivingEntity.aiStep retains vanilla component snap-to-zero below 0.003.
            var snapped=new Vec3(Math.abs(requested.x)<.003?0:requested.x,
                    Math.abs(requested.y)<.003?0:requested.y,Math.abs(requested.z)<.003?0:requested.z);
            check.accept(delta.distanceTo(snapped)<1e-6,"Rehooked pull scenario="+scenario+" frame="+frame+" delta="+delta+" requested="+requested);
        }
        if(frame==28) {
            client.getMethod("jump").invoke(clientHandler);
            check.accept(common.getMethod("getMomentum").invoke(clientHandler)!=null,"Rehooked jump creates momentum scenario="+scenario);
        }
        if(frame==30) {
            check.accept(common.getMethod("getMomentum").invoke(clientHandler)!=null,"Rehooked jump momentum survives scenario="+scenario);
            check.accept(mc.player.getDeltaMovement().y>0,"Rehooked jump moves upward scenario="+scenario+" velocity="+mc.player.getDeltaMovement());
        }
        if(++frame>33) {
            common.getMethod("removeAllHooks").invoke(clientHandler);
            common.getMethod("setMomentum",Vec3.class).invoke(clientHandler,new Object[]{null});
            frame=0;scenario++;
            if(scenario==6) {
                cn.rbq108.nextboundarycornerstone.VariableLibrary.debug.FORCE_LOW_GRAVITY=false;
                OptionalLaunchQa.server(player -> player.setItemSlot(EquipmentSlot.CHEST,new ItemStack(cn.rbq108.nextboundarycornerstone.main.BASIC_BACKPACK.get())));
            }
        }
        return false;
    }

    private static void setPull(Entity hook,Vec3 anchor) throws Exception {
        var state=Class.forName("com.oe.rehooked.entities.hook.HookEntity$State");
        var hit=net.minecraft.core.BlockPos.containing(anchor);
        hook.level().setBlock(hit,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        var setter=hook.getClass().getDeclaredMethod("setHitPos",net.minecraft.core.BlockPos.class);
        setter.setAccessible(true);setter.invoke(hook,hit);
        hook.setPos(anchor);hook.getClass().getMethod("setState",state).invoke(hook,state.getField("PULLING").get(null));
    }
}
