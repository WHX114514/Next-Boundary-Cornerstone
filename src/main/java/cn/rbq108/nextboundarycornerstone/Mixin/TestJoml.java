import org.joml.Quaternionf;
import org.joml.Vector3f;

public class TestJoml {
    public static void main(String[] args) {
        Quaternionf q = new Quaternionf();
        // Pitch down by 90 degrees
        q.rotateX((float)Math.toRadians(90));
        System.out.println("After rotateX(90): " + q.getEulerAnglesYXZ(new Vector3f()));
        
        // Yaw by 90 degrees
        q.rotateY((float)Math.toRadians(90));
        System.out.println("After rotateY(90): " + q.getEulerAnglesYXZ(new Vector3f()));
        
        Quaternionf q2 = new Quaternionf();
        q2.rotateLocalX((float)Math.toRadians(90));
        q2.rotateLocalY((float)Math.toRadians(90));
        System.out.println("After rotateLocal: " + q2.getEulerAnglesYXZ(new Vector3f()));
    }
}
