package cn.rbq108.nextboundarycornerstone.Mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {
    @Accessor("xd")
    void setXd(double xd);

    @Accessor("yd")
    void setYd(double yd);

    @Accessor("zd")
    void setZd(double zd);

    @Accessor("xd")
    double getXd();

    @Accessor("yd")
    double getYd();

    @Accessor("zd")
    double getZd();
}
