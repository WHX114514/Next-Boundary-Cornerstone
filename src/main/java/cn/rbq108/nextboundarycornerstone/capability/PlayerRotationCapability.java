package cn.rbq108.nextboundarycornerstone.capability;

import org.joml.Quaternionf;

public class PlayerRotationCapability {
    private final Quaternionf quaternion = new Quaternionf();
    private boolean lowGravity = false;

    public Quaternionf getQuaternion() {
        return quaternion;
    }

    public void setQuaternion(Quaternionf quat) {
        this.quaternion.set(quat);
    }

    public boolean isLowGravity() { return lowGravity; }
    public void setLowGravity(boolean v) { this.lowGravity = v; }
}