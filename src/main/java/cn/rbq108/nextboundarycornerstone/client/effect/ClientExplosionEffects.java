package cn.rbq108.nextboundarycornerstone.client.effect;

import cn.rbq108.nextboundarycornerstone.effect.ExplosionParticles;
import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
public final class ClientExplosionEffects {
    private static final int MAX_PLAYING = 4;
    private static final int MAX_NEAR = 2;
    private static final int MAX_FALLBACK_PER_TICK = 2;
    private static final double NEAR_SEPARATION_SQUARED = 3 * 3;
    private static final boolean HAS_PHOTON = ModList.get().getModContainerById("photon")
            .map(container -> container.getModInfo().getVersion().toString().equals("2.2.8"))
            .orElse(false);
    private static final List<Active> ACTIVE = new ArrayList<>(MAX_PLAYING);
    private static ClientLevel activeLevel;
    private static int fallbackCount;

    private ClientExplosionEffects() {}

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpecial(ExplosionParticles.HIGH_EXPLOSIVE.get(), (type, level, x, y, z, dx, dy, dz) -> {
            play(level, new Vec3(x, y, z));
            return null;
        });
    }

    @SubscribeEvent
    public static void registerReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resources -> {
            clear();
            if (HAS_PHOTON) {
                PhotonExplosionEffects.invalidateResources();
            }
        });
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        changeLevel(client.level);
        if (HAS_PHOTON && client.player != null) {
            PhotonExplosionEffects.prepare();
        }
        ACTIVE.removeIf(active -> active.playback().finished());
        fallbackCount = 0;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        changeLevel(null);
    }

    private static void play(ClientLevel level, Vec3 position) {
        changeLevel(level);
        Minecraft client = Minecraft.getInstance();
        double distance = client.gameRenderer.getMainCamera().getPosition().distanceToSqr(position);
        if (distance > 64 * 64) {
            return;
        }
        ParticleStatus particles = client.options.particles().get();
        if (HAS_PHOTON && distance <= 48 * 48 && particles != ParticleStatus.MINIMAL && ACTIVE.size() < MAX_PLAYING) {
            boolean near = distance <= 24 * 24 && particles == ParticleStatus.ALL
                    && ACTIVE.stream().filter(Active::near).count() < MAX_NEAR
                    && ACTIVE.stream().noneMatch(active -> active.near()
                            && active.position().distanceToSqr(position) < NEAR_SEPARATION_SQUARED);
            Playback playback = PhotonExplosionEffects.play(level, position, near);
            if (playback != null) {
                ACTIVE.add(new Active(near, position, playback));
                return;
            }
        }
        if (fallbackCount < MAX_FALLBACK_PER_TICK) {
            fallbackCount++;
            level.addParticle(ParticleTypes.EXPLOSION, true, position.x, position.y, position.z, 0, 0, 0);
        }
    }

    private static void changeLevel(ClientLevel level) {
        if (activeLevel != level) {
            clear();
            activeLevel = level;
        }
    }

    private static void clear() {
        ACTIVE.forEach(active -> active.playback().stop());
        ACTIVE.clear();
        fallbackCount = 0;
    }

    interface Playback {
        boolean finished();

        void stop();
    }

    private record Active(boolean near, Vec3 position, Playback playback) {}
}
