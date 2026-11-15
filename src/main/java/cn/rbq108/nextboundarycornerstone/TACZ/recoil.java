package cn.rbq108.nextboundarycornerstone.TACZ;

import cn.rbq108.nextboundarycornerstone.VariableLibrary.GlobalVariables;
import cn.rbq108.nextboundarycornerstone.body.SegmentedHitboxes;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.event.common.GunFirePostEvent;
import com.tacz.guns.api.item.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Local flight impulse per successful shot, independent of shell ejection and bolt animations. */
public final class recoil {
    private recoil() {}

    // Called only during client setup when the optional TaCZ mod is present.
    public static void register() {
        NeoForge.EVENT_BUS.addListener(recoil::onGunFire);
    }

    private static void onGunFire(GunFirePostEvent event) {
        var mc = Minecraft.getInstance();
        if (event.getLogicalSide() != LogicalSide.CLIENT || event.getShooter() != mc.player) return;
        ItemStack firedGun = event.getGunItemStack().copy();
        mc.execute(() -> {
            if (mc.player != null && event.getShooter() == mc.player && SegmentedHitboxes.enabled(mc.player)) {
                applyWeaponBlowback(firedGun);
            }
        });
    }

    private static void applyWeaponBlowback(ItemStack firedGun) {
        IGun gun = IGun.getIGunOrNull(firedGun);
        if (gun == null) return;
        TimelessAPI.getClientGunIndex(gun.getGunId(firedGun)).ifPresent(index -> {
            var data = index.getGunData();
            if (data == null || data.getRecoil() == null) return;
            var frames = data.getRecoil().getPitch();
            if (frames == null || frames.length == 0) return;
            float[] values = frames[0].getValue();
            if (values == null || values.length < 2) return;
            // Preserve the existing strength; only the trigger and pending-impulse accumulation change.
            float force = (values[0] + values[1]) * .5f * .05f;
            if (!Float.isFinite(force)) return;
            Vector3f impulse = new Quaternionf(GlobalVariables.currentQuat).transform(new Vector3f(0, 0, -force));
            GlobalVariables.B_Vx5 += impulse.x;
            GlobalVariables.B_Vy5 += impulse.y;
            GlobalVariables.B_Vz5 += impulse.z;
        });
    }
}
