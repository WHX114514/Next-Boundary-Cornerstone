package cn.rbq108.nextboundarycornerstone.qa;

/** Render probes have no TaCZ linkage, so ordinary client QA still runs without it. */
public final class TACZQaState {
    public static boolean active, slim, checkingHands;
    public static int leftHands, rightHands;
    public static String failure;

    private TACZQaState() {}

    public static void hand(net.minecraft.client.model.geom.ModelPart part, boolean left) {
        if (!active || !checkingHands) return;
        if (left) leftHands++; else rightHands++;
        if (Math.abs(part.yRot) > .0001f || Math.abs(part.zRot) > .0001f) {
            failure = "Gun hand rotation overwritten: left=" + left + ", y=" + part.yRot + ", z=" + part.zRot;
        }
    }
}
