package cn.rbq108.nextboundarycornerstone.client.effect;

import cn.rbq108.nextboundarycornerstone.main;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXHelper;
import com.lowdragmc.photon.client.fx.FXRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

final class PhotonExplosionEffects {
    private static final ResourceLocation NEAR = ResourceLocation.fromNamespaceAndPath(main.MODID, "high_explosive");
    private static final ResourceLocation LIGHT = ResourceLocation.fromNamespaceAndPath(main.MODID, "high_explosive_lite");
    private static final List<ResourceLocation> TEXTURES = List.of("glow", "spark", "shard", "smoke").stream()
            .map(name -> ResourceLocation.fromNamespaceAndPath(main.MODID, "textures/effect/blast_" + name + ".png"))
            .toList();
    private static FX nearFx;
    private static FX lightFx;
    private static boolean resolved;

    private PhotonExplosionEffects() {}

    static void prepare() {
        if (!resolved) {
            var resources = Minecraft.getInstance().getResourceManager();
            if (TEXTURES.stream().allMatch(texture -> resources.getResource(texture).isPresent())) {
                nearFx = FXHelper.getFX(NEAR);
                lightFx = FXHelper.getFX(LIGHT);
            }
            resolved = true;
        }
    }

    static ClientExplosionEffects.Playback play(ClientLevel level, Vec3 position, boolean near) {
        prepare();
        FX fx = near ? nearFx : lightFx;
        if (fx == null || fx.getFxData().objects().isEmpty()) {
            return null;
        }
        FXRuntime runtime = fx.createRuntime();
        runtime.root.updatePos(new Vector3f((float) position.x, (float) position.y, (float) position.z));
        runtime.emit(() -> level);
        return new ClientExplosionEffects.Playback() {
            @Override
            public boolean finished() {
                return !runtime.isValid() || runtime.isFinished();
            }

            @Override
            public void stop() {
                runtime.destroy(true);
            }
        };
    }

    static void invalidateResources() {
        resolved = false;
        nearFx = null;
        lightFx = null;
    }
}
