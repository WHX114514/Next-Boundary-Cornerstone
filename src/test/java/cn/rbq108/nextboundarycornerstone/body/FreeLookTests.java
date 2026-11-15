package cn.rbq108.nextboundarycornerstone.body;

import org.joml.Quaternionf;

public final class FreeLookTests {
    private static int checks;
    public static void main(String[] args) {
        var angles = new FreeLookMath.Angles(0, 0, 0);
        for (int i = 0; i < 1000; i++) angles = angles.turn(9, 7, 6, true);
        check(angles.yaw() == 110 && angles.pitch() == 90 && angles.roll() == 60, "ordinary first-person view saturates at limits");
        angles = angles.turn(-1, -1, -1, true);
        check(angles.yaw() == 109 && angles.pitch() == 89 && angles.roll() == 59, "reverse input immediately leaves limit without hidden accumulated turns");
        for (int i = 0; i < 1000; i++) angles = angles.turn(-9, -7, -6, true);
        check(angles.yaw() == -110 && angles.pitch() == -90 && angles.roll() == -60, "negative view limits");
        var third = new FreeLookMath.Angles(0, 0, 0).turn(810, 900, 900, false);
        check(third.yaw() == 90 && third.pitch() == 90 && third.roll() == 60, "third-person ordinary orbit wraps yaw but limits pitch and roll");
        var head = FreeLookMath.relative(new Quaternionf(), new FreeLookMath.Angles(110, 40, 20).headRotation());
        check(Math.abs(head.yaw() - 90) < .001 && Math.abs(head.pitch() - 40) < .001, "head yaw has ordinary free-look's ninety degree limit");
        for (float pitch : new float[] {-90, -89, -40, 0, 40, 89, 90}) {
            for (float yaw : new float[] {-110, -90, -20, 0, 20, 90, 110}) {
                for (float roll : new float[] {-60, 0, 60}) {
                    Quaternionf body = new Quaternionf().rotateYXZ(.7f, -.4f, 1.2f);
                    var expected = new FreeLookMath.Angles(yaw, pitch, roll);
                    var camera = new Quaternionf(body).mul(expected.rotation());
                    var extracted = FreeLookMath.relative(body, camera);
                    check(Math.abs(extracted.rotation().dot(expected.rotation())) > .99999,
                            "body-local view survives quaternion extraction including vertical pitch");
                    check(Float.isFinite(extracted.yaw()) && Float.isFinite(extracted.pitch()) && Float.isFinite(extracted.roll()), "no gimbal NaN");
                }
            }
        }
        System.out.println("FREE_LOOK_TEST_PASS assertions=" + checks);
    }
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
}
