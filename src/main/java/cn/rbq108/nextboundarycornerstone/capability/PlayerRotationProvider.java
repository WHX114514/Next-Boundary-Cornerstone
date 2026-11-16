package cn.rbq108.nextboundarycornerstone.capability;

import cn.rbq108.nextboundarycornerstone.attachment.PlayerRotationAttachment;
import net.minecraft.core.Direction;
import net.minecraft.nbt.*;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PlayerRotationProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<PlayerRotationCapability> PLAYER_ROTATION = CapabilityManager.get(new CapabilityToken<>() {});
    private final PlayerRotationCapability backend = new PlayerRotationCapability();
    private LazyOptional<PlayerRotationCapability> optional = LazyOptional.of(() -> backend);
    private final net.minecraft.world.entity.player.Player owner;

    public PlayerRotationProvider() { this(null); }
    public PlayerRotationProvider(net.minecraft.world.entity.player.Player owner) { this.owner = owner; }

    @Override public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap != PLAYER_ROTATION) return LazyOptional.empty();
        // Entity.getCapability gates invalid entities. After reviveCaps, expose the same backend
        // through a new optional; stale references must remain invalid.
        if (owner != null && !optional.isPresent()) optional = LazyOptional.of(() -> backend);
        return optional.cast();
    }
    public void invalidate() { optional.invalidate(); }
    @Override public CompoundTag serializeNBT() {
        return (CompoundTag) PlayerRotationAttachment.CODEC.encodeStart(NbtOps.INSTANCE, backend).result().orElseGet(CompoundTag::new);
    }
    @Override public void deserializeNBT(CompoundTag tag) {
        PlayerRotationAttachment.CODEC.parse(NbtOps.INSTANCE, tag).result().ifPresent(value -> {
            backend.setQuaternion(value.getQuaternion());
            backend.setLowGravity(value.isLowGravity());
        });
    }
}
