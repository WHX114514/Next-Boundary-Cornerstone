package cn.rbq108.nextboundarycornerstone.capability;

import org.joml.Quaternionf;

public class PlayerRotationCapability {
    private final Quaternionf quaternion = new Quaternionf();

    public Quaternionf getQuaternion() {
        return quaternion;
    }

    public void setQuaternion(Quaternionf quat) {
        this.quaternion.set(quat);
    }
}