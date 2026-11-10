package cn.rbq108.nextboundarycornerstone.effect;

import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ExplosionParticles {
    private static final DeferredRegister<ParticleType<?>> TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, main.MODID);

    // The client provider applies the particle setting and the distance budget itself.
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HIGH_EXPLOSIVE =
            TYPES.register("high_explosive", () -> new SimpleParticleType(true));

    private ExplosionParticles() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }
}
