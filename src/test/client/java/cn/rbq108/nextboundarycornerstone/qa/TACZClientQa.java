package cn.rbq108.nextboundarycornerstone.qa;

import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import org.joml.Vector3f;

/** Run only in the opt-in tacz visual scenario with the official mod on the runtime classpath. */
final class TACZClientQa {
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
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }
}
