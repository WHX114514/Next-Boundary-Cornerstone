package cn.rbq108.nextboundarycornerstone.api;

import cn.rbq108.nextboundarycornerstone.effect.ExplosionParticles;
import net.minecraft.core.particles.ParticleOptions;

public final class ExplosionEffects {
    private ExplosionEffects() {}

    /**
     * Returns the high-explosive visual preset after particle registration.
     * Pass it as both particle arguments of a server-side explosion, or send
     * one particle with zero spread for a visual-only burst. The caller owns
     * damage, terrain interaction and sound.
     */
    public static ParticleOptions highExplosive() {
        return ExplosionParticles.HIGH_EXPLOSIVE.get();
    }
}
