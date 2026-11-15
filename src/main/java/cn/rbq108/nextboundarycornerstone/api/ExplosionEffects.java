package cn.rbq108.nextboundarycornerstone.api;

import cn.rbq108.nextboundarycornerstone.effect.ExplosionParticles;
import net.minecraft.core.particles.ParticleOptions;

public final class ExplosionEffects {
    private ExplosionEffects() {}

    /**
     * * 在粒子注册完成后返回高爆视觉预设。
     *  * 可在服务端爆炸中将其同时作为两个粒子参数传入，或以零扩散（spread 为 0）发送单个粒子，
     *  * 以触发纯视觉的爆发效果。伤害、地形交互与音效均由调用方自行负责。
     * Returns the high-explosive visual preset after particle registration.
     * Pass it as both particle arguments of a server-side explosion, or send
     * one particle with zero spread for a visual-only burst. The caller owns
     * damage, terrain interaction and sound.
     */
    public static ParticleOptions highExplosive() {
        return ExplosionParticles.HIGH_EXPLOSIVE.get();
    }
}
