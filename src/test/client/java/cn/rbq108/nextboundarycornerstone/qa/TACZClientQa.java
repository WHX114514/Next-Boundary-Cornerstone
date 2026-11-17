package cn.rbq108.nextboundarycornerstone.qa;

import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import org.joml.Vector3f;

/** Run only in the opt-in tacz visual scenario with the official mod on the runtime classpath. */
final class TACZClientQa {
    static void prepare(Minecraft mc) {
        var id = mc.player.getUUID();
        mc.getSingleplayerServer().execute(() -> {
            var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
            var stack = com.tacz.guns.api.item.builder.GunItemBuilder.create()
                    .setId(net.minecraft.resources.ResourceLocation.parse("tacz:ak47")).setAmmoCount(30).setAmmoInBarrel(true).build();
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
            com.tacz.guns.api.entity.IGunOperator.fromLivingEntity(player).draw(player::getMainHandItem);
        });
    }

    static void verify(Minecraft mc) {
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        if (!mc.player.isNoGravity()) throw new AssertionError("TaCZ QA needs a rotating low-gravity camera");
        var bullet = new EntityKineticBullet(EntityKineticBullet.TYPE, mc.level);
        bullet.setOwner(mc.player);
        var muzzle = new Vector3f(.17f, -.13f, -.6f);
        bullet.setFirstPersonRenderOffset(new Vector3f(muzzle));
        bullet.setFirstPersonCameraRotation(mc.gameRenderer.getMainCamera().rotation());
        for (int i = 0; i < 5; i++) {
            if (!bullet.getFirstPersonRenderOffset().equals(muzzle))
                throw new AssertionError("TaCZ muzzle offset was rotated/scaled again by the old adaptation");
        }
        com.mojang.logging.LogUtils.getLogger().info("TACZ_FIRST_PERSON_OFFSET_QA_PASS");
        verifyRecoil(mc);
        try { TACZCameraQa.verify(); }
        catch (Exception e) { throw new RuntimeException("TaCZ camera QA", e); }
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }

    private static void verifyRecoil(Minecraft mc) {
        var previousGun = mc.player.getMainHandItem();
        var pending = impulse();
        var gun = com.tacz.guns.api.item.builder.GunItemBuilder.create()
                .setId(net.minecraft.resources.ResourceLocation.parse("tacz:ai_awp"))
                .setAmmoCount(5).setAmmoInBarrel(true).build();
        var shells = java.util.List.copyOf(cn.rbq108.nextboundarycornerstone.client.SpaceShellManager.SHELLS);
        try {
            require(!gun.isEmpty(), "bolt-action test gun exists");
            mc.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, gun);
            clearImpulse();
            // Trigger pulls and canceled/pre-fire notifications must not create recoil.
            var attempt = new com.tacz.guns.api.event.common.GunFireEvent(mc.player, gun, net.neoforged.fml.LogicalSide.CLIENT);
            attempt.setCanceled(true);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(attempt);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                    new com.tacz.guns.api.event.common.GunShootEvent(mc.player, gun, net.neoforged.fml.LogicalSide.CLIENT));
            require(impulse().lengthSquared() == 0, "attempts without a successful shot have no recoil");
            fire(mc.player, gun, net.neoforged.fml.LogicalSide.CLIENT);
            Vector3f first = impulse();
            require(first.lengthSquared() > 0, "bolt-action shot recoils immediately before shell ejection");
            var forward = new org.joml.Quaternionf(cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.currentQuat)
                    .transform(new Vector3f(0, 0, 1));
            require(new Vector3f(first).normalize().dot(forward) < -.9999f, "recoil opposes the rotated shot direction");
            new com.tacz.guns.client.model.functional.ShellRender(null).addShell(new Vector3f());
            require(cn.rbq108.nextboundarycornerstone.client.SpaceShellManager.SHELLS.size() > shells.size(),
                    "delayed bolt shell actually spawns");
            require(impulse().equals(first), "bolt shell does not add another recoil impulse");
            fire(mc.player, gun, net.neoforged.fml.LogicalSide.CLIENT);
            require(impulse().distance(new Vector3f(first).mul(2)) < 1e-6f, "same-tick shots accumulate rather than overwrite");
            clearImpulse();
            fire(mc.player, gun, net.neoforged.fml.LogicalSide.SERVER);
            var remote = net.minecraft.world.entity.EntityType.ZOMBIE.create(mc.level);
            fire(remote, gun, net.neoforged.fml.LogicalSide.CLIENT);
            require(impulse().lengthSquared() == 0, "server and remote shooters cannot recoil the local player");
            mc.player.setNoGravity(false);
            fire(mc.player, gun, net.neoforged.fml.LogicalSide.CLIENT);
            require(impulse().lengthSquared() == 0, "ordinary gravity does not gain a flight impulse");
            mc.player.setNoGravity(true);
            // The shot payload wins even if the currently held item has already changed.
            mc.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
            fire(mc.player, gun, net.neoforged.fml.LogicalSide.CLIENT);
            require(impulse().distance(first) < 1e-6f, "recoil uses the fired gun payload");
            com.mojang.logging.LogUtils.getLogger().info("TACZ_RECOIL_QA_PASS immediateShot=true delayedShell=false burstAccumulates=true");
        } finally {
            mc.player.setNoGravity(true);
            mc.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, previousGun);
            var globals = cn.rbq108.nextboundarycornerstone.client.SpaceShellManager.SHELLS;
            globals.removeIf(shell -> !shells.contains(shell));
            cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_Vx5 = pending.x;
            cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_Vy5 = pending.y;
            cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_Vz5 = pending.z;
        }
    }

    private static void fire(net.minecraft.world.entity.LivingEntity shooter, net.minecraft.world.item.ItemStack gun,
                             net.neoforged.fml.LogicalSide side) {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                new com.tacz.guns.api.event.common.GunFirePostEvent(shooter, gun, side));
    }

    private static Vector3f impulse() {
        return new Vector3f(cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_Vx5,
                cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_Vy5,
                cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_Vz5);
    }

    private static void clearImpulse() {
        cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_Vx5 = 0;
        cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_Vy5 = 0;
        cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables.B_Vz5 = 0;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
