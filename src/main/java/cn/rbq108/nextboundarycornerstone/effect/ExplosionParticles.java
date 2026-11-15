package cn.rbq108.nextboundarycornerstone.effect;

import cn.rbq108.nextboundarycornerstone.main;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public final class ExplosionParticles {
    private static final DeferredRegister<ParticleType<?>> TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, main.MODID);

    // The client provider applies the particle setting and the distance budget itself.
    public static final RegistryObject<SimpleParticleType> HIGH_EXPLOSIVE =
            TYPES.register("high_explosive", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> GLOW = TYPES.register("blast_glow", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> SPARK = TYPES.register("blast_spark", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> SHARD = TYPES.register("blast_shard", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> SMOKE = TYPES.register("blast_smoke", () -> new SimpleParticleType(false));

    private ExplosionParticles() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }
}
