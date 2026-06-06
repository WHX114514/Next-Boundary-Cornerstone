package cn.rbq108.nextboundarycornerstone.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerRotationProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<PlayerRotationCapability> PLAYER_ROTATION = CapabilityManager.get(new CapabilityToken<>() {});

    private final PlayerRotationCapability backend = new PlayerRotationCapability();
    private final LazyOptional<PlayerRotationCapability> optional = LazyOptional.of(() -> backend);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == PLAYER_ROTATION ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putFloat("x", backend.getQuaternion().x);
        nbt.putFloat("y", backend.getQuaternion().y);
        nbt.putFloat("z", backend.getQuaternion().z);
        nbt.putFloat("w", backend.getQuaternion().w);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        backend.getQuaternion().set(
                nbt.getFloat("x"),
                nbt.getFloat("y"),
                nbt.getFloat("z"),
                nbt.getFloat("w")
        );
    }
}