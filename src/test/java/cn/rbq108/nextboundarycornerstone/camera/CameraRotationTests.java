package cn.rbq108.nextboundarycornerstone.camera;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class CameraRotationTests {
    private static int assertions;
    private static void roundTrip(Quaternionf q) {
        var angles = CameraRotation.angles(q);
        var actual = CameraRotation.rotation(angles.yaw(),angles.pitch(),angles.roll());
        var expected = new Quaternionf(q).normalize();
        for (var axis : new Vector3f[]{new Vector3f(1,0,0),new Vector3f(0,1,0),new Vector3f(0,0,1)}) {
            float distance = actual.transform(new Vector3f(axis)).distance(expected.transform(new Vector3f(axis)));
            if (!(distance < 2e-6)) throw new AssertionError("camera round trip " + q + " angles=" + angles + " distance=" + distance);
            assertions++;
        }
    }

    public static void main(String[] args) {
        var random = new java.util.Random(20261117L);
        for (int i = 0; i < 5000; i++) {
            var q = new Quaternionf().rotationYXZ((random.nextFloat()-.5f)*8,
                    (random.nextFloat()-.5f)*8,(random.nextFloat()-.5f)*8);
            roundTrip(q);
            float scale = i % 2 == 0 ? 1.0001f : -2;
            roundTrip(new Quaternionf(q.x*scale,q.y*scale,q.z*scale,q.w*scale));
        }
        for (float sign : new float[]{-1,1}) for (int i = -1000; i <= 1000; i++)
            roundTrip(new Quaternionf().rotationYXZ(.7f,(float)Math.toRadians(sign*(90+i*.0002)),1.1f));
        System.out.println("CAMERA_ROTATION_TEST_PASS assertions=" + assertions);
    }
}
