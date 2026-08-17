import java.util.zip.ZipFile;
import java.util.Enumeration;
import java.util.zip.ZipEntry;

public class ListJar {
    public static void main(String[] args) {
        try {
            ZipFile zf = new ZipFile("run/mods/create-aeronautics-bundled-1.21.1-1.3.1.jar");
            Enumeration<? extends ZipEntry> entries = zf.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                String lower = name.toLowerCase();
                if (lower.contains("handle") || lower.contains("grab") || lower.contains("mount")) {
                    System.out.println(name);
                }
            }
            zf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
