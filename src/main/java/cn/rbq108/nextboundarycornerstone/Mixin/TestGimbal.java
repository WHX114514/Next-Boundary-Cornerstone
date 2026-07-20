import org.joml.Quaternionf;
import org.joml.Vector3f;

public class TestGimbal {
    public static void main(String[] args) {
        Quaternionf q = new Quaternionf();
        // simulate looking straight down
        q.rotationYXZ((float)Math.toRadians(45), (float)Math.toRadians(89.99), 0);
        
        Vector3f fwd = new Vector3f(0, 0, 1).rotate(q);
        double horiz = Math.sqrt(fwd.x * fwd.x + fwd.z * fwd.z);
        
        float yaw = 45;
        if (horiz > 0.001) {
            yaw = (float) Math.toDegrees(-Math.atan2(fwd.x, fwd.z));
        }
        float pitch = (float) Math.toDegrees(Math.asin(-fwd.y));
        
        System.out.println("Forward: " + fwd);
        System.out.println("Yaw: " + yaw + ", Pitch: " + pitch);
    }
}
