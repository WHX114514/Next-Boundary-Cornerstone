package cn.rbq108.nextboundarycornerstone.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.joml.Quaternionf;

public class PlayerRotationAttachment {
    private final Quaternionf quaternion = new Quaternionf();
    private boolean lowGravity = false;

    public static final Codec<PlayerRotationAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("x").forGetter(a -> a.quaternion.x),
            Codec.FLOAT.fieldOf("y").forGetter(a -> a.quaternion.y),
            Codec.FLOAT.fieldOf("z").forGetter(a -> a.quaternion.z),
            Codec.FLOAT.fieldOf("w").forGetter(a -> a.quaternion.w),
            Codec.BOOL.fieldOf("lowGravity").forGetter(a -> a.lowGravity)
    ).apply(instance, (x, y, z, w, lg) -> {
        PlayerRotationAttachment a = new PlayerRotationAttachment();
        a.quaternion.set(x, y, z, w);
        a.lowGravity = lg;
        return a;
    }));

    public Quaternionf getQuaternion() {
        return quaternion;
    }

    public void setQuaternion(Quaternionf quat) {
        this.quaternion.set(quat);
    }

    public boolean isLowGravity() {
        return lowGravity;
    }

    public void setLowGravity(boolean v) {
        this.lowGravity = v;
    }
}
