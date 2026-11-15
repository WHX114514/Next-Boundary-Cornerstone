package cn.rbq108.nextboundarycornerstone.client.effect;

import cn.rbq108.nextboundarycornerstone.effect.ExplosionParticles;
import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge-native preset using the shared blast textures, with no Photon 2 runtime dependency. */
@Mod.EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientExplosionEffects {
    private static long lastTick = Long.MIN_VALUE;
    private static ClientLevel lastLevel;
    private static int bursts;
    private ClientExplosionEffects() {}

    @Mod.EventBusSubscriber(modid = main.MODID, value = Dist.CLIENT)
    public static final class Cleanup {
        @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
            lastLevel = null;
            lastTick = Long.MIN_VALUE;
            bursts = 0;
        }
    }

    @SubscribeEvent public static void providers(RegisterParticleProvidersEvent event) {
        event.registerSpecial(ExplosionParticles.HIGH_EXPLOSIVE.get(), (type, level, x, y, z, dx, dy, dz) -> {
            play(level, x, y, z);
            return null;
        });
        event.registerSpriteSet(ExplosionParticles.GLOW.get(), sprites -> provider(sprites, 0));
        event.registerSpriteSet(ExplosionParticles.SPARK.get(), sprites -> provider(sprites, 1));
        event.registerSpriteSet(ExplosionParticles.SHARD.get(), sprites -> provider(sprites, 2));
        event.registerSpriteSet(ExplosionParticles.SMOKE.get(), sprites -> provider(sprites, 3));
    }
    private static ParticleProvider<SimpleParticleType> provider(SpriteSet sprites, int kind) {
        return (type, level, x, y, z, dx, dy, dz) -> new BlastParticle(level, x, y, z, dx, dy, dz, sprites, kind);
    }
    private static void play(ClientLevel level, double x, double y, double z) {
        var client = Minecraft.getInstance();
        double distance = client.gameRenderer.getMainCamera().getPosition().distanceToSqr(x, y, z);
        if (distance > 4096) return;
        if (lastLevel != level || lastTick != level.getGameTime()) { lastLevel = level; lastTick = level.getGameTime(); bursts = 0; }
        if (bursts++ >= 2) return;
        var quality = client.options.particles().get();
        if (quality == ParticleStatus.MINIMAL) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.EXPLOSION, x, y, z, 0, 0, 0);
            return;
        }
        int count = distance < 576 && quality == ParticleStatus.ALL ? 24 : 10;
        level.addParticle(ExplosionParticles.GLOW.get(), x, y, z, 0, 0, 0);
        for (int i = 0; i < count; i++) {
            double dx = level.random.nextGaussian(), dy = level.random.nextGaussian(), dz = level.random.nextGaussian();
            double length = Math.max(.001, Math.sqrt(dx*dx + dy*dy + dz*dz));
            level.addParticle(ExplosionParticles.SPARK.get(), x, y, z, dx/length*.8, dy/length*.8, dz/length*.8);
            if (i % 3 == 0) level.addParticle(ExplosionParticles.SHARD.get(), x, y, z, dx*.2, dy*.2, dz*.2);
            if (i % 2 == 0) level.addParticle(ExplosionParticles.SMOKE.get(), x+dx*.2, y+dy*.2, z+dz*.2, dx*.04, .04+Math.abs(dy)*.04, dz*.04);
        }
    }
    private static final class BlastParticle extends TextureSheetParticle {
        private final int kind;
        private final float initialSize;
        private BlastParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites, int kind) {
            super(level, x, y, z);
            this.kind = kind;
            xd=dx; yd=dy; zd=dz;
            lifetime = kind == 0 ? 8 : kind == 3 ? 35 + random.nextInt(15) : 12 + random.nextInt(10);
            quadSize = initialSize = kind == 0 ? 3f : kind == 3 ? .8f : kind == 1 ? .12f : .09f;
            gravity = kind == 2 ? .5f : 0;
            friction = kind == 3 ? .96f : .9f;
            hasPhysics = kind != 0;
            pickSprite(sprites);
            if (kind == 3) setColor(.3f, .29f, .27f);
            else setColor(1f, .8f, .45f);
        }
        @Override public void tick() {
            super.tick();
            float remaining = Math.max(0, 1 - (float)age/lifetime);
            alpha = remaining * (kind == 3 ? .7f : 1f);
            quadSize = initialSize * (kind == 3 ? 1 + (1-remaining)*2 : remaining);
        }
        @Override protected int getLightColor(float partialTick) { return kind == 3 ? super.getLightColor(partialTick) : 0xF000F0; }
        @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
    }
}
