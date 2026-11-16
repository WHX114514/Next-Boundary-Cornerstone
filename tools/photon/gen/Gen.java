import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/**
 * Generates the cornerstone high-explosive Photon FX (near + lite) and its procedural textures.
 * Emitters are cloned from the existing exported FX so every compound keeps a structure Photon 2.2.8 accepts.
 *
 * usage: java -cp . Gen <cornerstone-root> <original high_explosive.fx>
 */
public class Gen {
    static final String NS = "next_boundary_cornerstone";
    static final Random RNG = new Random(1080);

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: java -cp . Gen <cornerstone-root> <original high_explosive.fx>");
        }
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        Path res = root.resolve("src/main/resources/assets/" + NS);
        Path texDir = res.resolve("textures/effect");
        Path fxDir = res.resolve("fx");
        Path projDir = root.resolve("tools/photon");

        // templates come from the original export (backed up), so the generator can be re-run safely
        Path source = Path.of(args[1]).toAbsolutePath().normalize();
        if (source.equals(fxDir.resolve("high_explosive.fx"))
                || source.equals(fxDir.resolve("high_explosive_lite.fx"))) {
            throw new IllegalArgumentException("Use a backup of the original FX, never the generated output");
        }
        Object[] src = Nbt.read(source);
        Map<String, Object> srcRoot = cast(src[1]);
        List<Object> objs = ((Nbt.ListTag) path(srcRoot, "fxData.fxObjects")).items();
        Map<String, Map<String, Object>> tpl = new HashMap<>();
        for (Object o : objs) {
            Map<String, Object> m = cast(o);
            String name = (String) path(m, "data.name");
            if (tpl.put(name.substring(0, name.indexOf(' ')), m) != null) {
                throw new IllegalArgumentException("Duplicate template prefix: " + name);
            }
        }
        String[] originals = {"01  White-hot flash", "02  Copper-red bloom",
                "03  Outward gold streaks", "04  Dark tumbling fragments", "05  Expanding ash-white smoke"};
        for (String name : originals) {
            var template = tpl.get(name.substring(0, 2));
            if (template == null || !name.equals(path(template, "data.name"))) {
                throw new IllegalArgumentException("Expected original template: " + name);
            }
        }
        RNG.setSeed(1080);
        // Build both variants before writing anything, so invalid template fields fail without partial output.
        List<Object> nearObjects = build(tpl, true);
        List<Object> liteObjects = build(tpl, false);
        Files.createDirectories(texDir);
        Files.createDirectories(fxDir);
        Files.createDirectories(projDir);
        Textures.writeAll(texDir);

        for (boolean near : new boolean[]{true, false}) {
            List<Object> out = near ? nearObjects : liteObjects;
            Map<String, Object> fxData = map("fxObjects", new Nbt.ListTag((byte) 10, out));
            String base = near ? "high_explosive" : "high_explosive_lite";
            Map<String, Object> fx = map("fxData", fxData, "version", 5);
            Nbt.write(fxDir.resolve(base + ".fx"), "", fx, true);
            Map<String, Object> proj = map("data", map("fx", map("fxData", deepCopy(fxData))),
                    "meta", map("name", "fx_project", "version_num", 5, "suffix", ".fxproj", "version", "5.0"));
            Nbt.write(projDir.resolve(base + ".fxproj"), "", proj, false);
            System.out.println(base + ": " + out.size() + " emitters");
        }
    }

    // ------------------------------------------------------------------ emitters

    static List<Object> build(Map<String, Map<String, Object>> tpl, boolean near) {
        List<Object> list = new ArrayList<>();
        var smokeT = tpl.get("05");
        var glowT = tpl.get("01");
        var bloomT = tpl.get("02");
        var streakT = tpl.get("03");
        var rotT = path(tpl.get("04"), "data.config.rotationOverLifetime");

        // --- 06 smoke: fire-lit at birth, then rising warm-grey billows (alpha blended, drawn first)
        {
            var e = clone(smokeT, "06  Fire-lit rising smoke", 0, 0.1f, 0);
            texture(e, "blast_smoke", false, null);
            int n = near ? 12 : 4;
            burst(e, n, 2);
            set(e, "maxParticles", n);
            set(e, "startLifetime", near ? rc(45, 75) : rc(30, 50));
            set(e, "startSpeed", rc(0.8, 2.6));
            set(e, "startSize", size3(rc(1.4, 2.4)));
            set(e, "startRotation", list3(c(0), c(0), rc(0, 360)));
            set(e, "sizeOverLifetime.size", size3(curve(0, 3, 0, 0.25, 0.3, 0.5, 1, 0.85)));
            set(e, "colorOverLifetime.color", gradient(
                    new float[]{0, 0, 0.12f, 0.62f, 0.55f, 0.45f, 1, 0},
                    new float[]{0, 1, 0.55f, 0.25f, 0.18f, 0.42f, 0.36f, 0.32f, 0.5f, 0.36f, 0.34f, 0.33f, 1, 0.55f, 0.54f, 0.53f}));
            set(e, "physics.gravity", c(-0.18));
            set(e, "physics.friction", c(0.9));
            sphere(e, 0.8f);
            set(e, "rotationOverLifetime", rotation(rotT, -2, 2));
            set(e, "noise", near ? noise(0.5f, 0.05f) : map("_enable", (byte) 0));
            list.add(e);
        }
        // --- 02 fireball: billowing additive fire puffs, white-hot -> orange -> ember red
        {
            var e = clone(smokeT, "02  Billowing fireball", 0, 0, 0);
            texture(e, "blast_fire", true, new float[]{1.9f, 1.55f, 1.2f});
            set(e, "renderer.orderInLayer", 1);
            int n = near ? 10 : 5;
            burst(e, n, 0);
            set(e, "duration", 1);
            set(e, "maxParticles", n);
            set(e, "startLifetime", near ? rc(10, 20) : rc(8, 14));
            set(e, "startSpeed", rc(1.5, 4.0));
            set(e, "startSize", size3(near ? rc(1.3, 2.2) : rc(1.1, 1.8)));
            set(e, "startRotation", list3(c(0), c(0), rc(0, 360)));
            set(e, "sizeOverLifetime.size", size3(curve(0, 2, 0, 0.35, 0.3, 0.6, 1, 0.8)));
            set(e, "colorOverLifetime.color", gradient(
                    new float[]{0, 1, 0.6f, 0.85f, 1, 0},
                    new float[]{0, 1, 0.98f, 0.9f, 0.2f, 1, 0.8f, 0.35f, 0.45f, 1, 0.45f, 0.1f, 0.75f, 0.6f, 0.14f, 0.04f, 1, 0.25f, 0.05f, 0.03f}));
            set(e, "physics.gravity", c(-0.12));
            set(e, "physics.friction", c(0.85));
            sphere(e, 0.4f);
            set(e, "rotationOverLifetime", rotation(rotT, -6, 6));
            list.add(e);
        }
        // --- 02b lingering heat bloom (big soft copper glow under everything)
        {
            var e = clone(bloomT, "02b  Copper heat bloom", 0, 0, 0);
            texture(e, "blast_glow", true, new float[]{1.4f, 1.2f, 1.0f});
            set(e, "startSize", size3(c(near ? 3.0 : 2.4)));
            set(e, "startLifetime", c(near ? 16 : 12));
            list.add(e);
        }
        // --- 01 white-hot flash
        {
            var e = clone(glowT, "01  White-hot flash", 0, 0, 0);
            texture(e, "blast_glow", true, new float[]{3f, 3f, 3f});
            set(e, "renderer.orderInLayer", 2);
            set(e, "startSize", size3(c(near ? 3.5 : 2.8)));
            set(e, "startLifetime", c(5));
            set(e, "sizeOverLifetime.size", size3(curve(0, 1, 0, 0.5, 0.15, 1, 1, 0.2)));
            list.add(e);
        }
        // --- 01b anamorphic star flare
        {
            var e = clone(glowT, "01b  Star flare", 0, 0, 0);
            texture(e, "blast_flare", true, new float[]{2.5f, 2.3f, 2.0f});
            set(e, "renderer.orderInLayer", 2);
            set(e, "startSize", size3(c(near ? 7 : 5)));
            set(e, "startLifetime", c(8));
            set(e, "startRotation", list3(c(0), c(0), rc(0, 360)));
            set(e, "sizeOverLifetime.size", size3(curve(0, 1, 0, 0.3, 0.12, 1, 1, 0)));
            set(e, "colorOverLifetime.color", gradient(
                    new float[]{0, 1, 0.5f, 0.7f, 1, 0},
                    new float[]{0, 1, 1, 1, 1, 1, 0.85f, 0.55f}));
            list.add(e);
        }
        // --- 03 ground shock ring (horizontal, expands to ~10 blocks)
        {
            var e = clone(glowT, "03  Ground shock ring", 0, 0, 0);
            texture(e, "blast_ring", true, new float[]{1.6f, 1.5f, 1.4f});
            set(e, "renderer.renderMode", "Horizontal");
            set(e, "startSize", size3(c(1)));
            set(e, "startLifetime", c(near ? 12 : 10));
            set(e, "sizeOverLifetime.size", size3(curve(0, near ? 10 : 7, 0, 0.08, 0.35, 0.7, 1, 1)));
            set(e, "colorOverLifetime.color", gradient(
                    new float[]{0, 0.95f, 0.4f, 0.5f, 1, 0},
                    new float[]{0, 1, 0.9f, 0.75f, 1, 1, 0.75f, 0.5f}));
            list.add(e);
        }
        // --- 03b camera-facing shock shell
        if (near) {
            var e = clone(glowT, "03b  Shock shell", 0, 0, 0);
            texture(e, "blast_ring", true, new float[]{1.3f, 1.3f, 1.4f});
            set(e, "startSize", size3(c(1)));
            set(e, "startLifetime", c(8));
            set(e, "sizeOverLifetime.size", size3(curve(0, 7, 0, 0.1, 0.4, 0.75, 1, 1)));
            set(e, "colorOverLifetime.color", gradient(
                    new float[]{0, 0.7f, 1, 0},
                    new float[]{0, 0.85f, 0.92f, 1, 1, 0.85f, 0.92f, 1}));
            list.add(e);
        }
        // --- 04 gold streaks (stretched), now drooping under gravity
        {
            var e = clone(streakT, "04  Gold streaks", 0.0f, 0.12f, 0);
            texture(e, "blast_spark", true, new float[]{2.2f, 1.8f, 1.3f});
            int n = near ? 48 : 16;
            burst(e, n, 0);
            set(e, "maxParticles", n);
            set(e, "physics.gravity", c(0.35));
            list.add(e);
        }
        // --- 05 trailing ember arcs: hot embers thrown out with glowing tails, bouncing on terrain
        if (near) {
            var e = clone(streakT, "05  Trailing ember arcs", 0, 0.2f, 0);
            texture(e, "blast_ember", true, new float[]{2.4f, 1.8f, 1.2f});
            set(e, "renderer.renderMode", "Billboard");
            int n = 16;
            burst(e, n, 0);
            set(e, "maxParticles", n * 3);
            set(e, "startLifetime", rc(18, 34));
            set(e, "startSpeed", rc(7, 14));
            set(e, "startSize", size3(rc(0.22, 0.35)));
            set(e, "sizeOverLifetime.size", size3(curve(0, 1, 0, 1, 0.7, 0.85, 1, 0.3)));
            set(e, "colorOverLifetime.color", gradient(
                    new float[]{0, 1, 0.7f, 1, 1, 0},
                    new float[]{0, 1, 1, 0.85f, 0.3f, 1, 0.75f, 0.3f, 0.7f, 1, 0.4f, 0.1f, 1, 0.7f, 0.12f, 0.04f}));
            set(e, "physics.gravity", c(0.9));
            set(e, "physics.friction", c(0.955));
            set(e, "physics.hasCollision", (byte) 1);
            set(e, "physics.bounceRate", c(0.35));
            set(e, "physics.bounceChance", c(1));
            set(e, "physics.collidedFriction", c(0.6));
            sphere(e, 0.3f);
            trails(e);
            list.add(e);
        }
        // --- 07 rising cinders drifting on the heat
        if (near) {
            var e = clone(streakT, "07  Rising cinders", 0, 0.3f, 0);
            texture(e, "blast_ember", true, new float[]{2.2f, 1.6f, 1.1f});
            set(e, "renderer.renderMode", "Billboard");
            int n = 14;
            burst(e, n, 3);
            set(e, "duration", 4);
            set(e, "maxParticles", n);
            set(e, "startLifetime", rc(30, 60));
            set(e, "startSpeed", rc(0.4, 1.6));
            set(e, "startSize", size3(rc(0.07, 0.14)));
            set(e, "sizeOverLifetime.size", size3(curve(0, 1, 0, 1, 1, 0.5)));
            set(e, "colorOverLifetime.color", gradient(
                    new float[]{0, 0, 0.1f, 1, 0.7f, 0.9f, 1, 0},
                    new float[]{0, 1, 0.8f, 0.4f, 0.5f, 1, 0.45f, 0.12f, 1, 0.6f, 0.1f, 0.03f}));
            set(e, "physics.gravity", c(-0.15));
            set(e, "physics.friction", c(0.95));
            sphere(e, 1.4f);
            set(e, "noise", noise(0.8f, 0.12f));
            list.add(e);
        }
        return list;
    }

    // ------------------------------------------------------------------ emitter helpers

    static Map<String, Object> clone(Map<String, Object> tpl, String name, float x, float y, float z) {
        Map<String, Object> e = cast(deepCopy(tpl));
        Map<String, Object> data = cast(e.get("data"));
        put(data, "name", name);
        Map<String, Object> tr = cast(data.get("transform"));
        put(tr, "id", new UUID(RNG.nextLong(), RNG.nextLong()).toString());
        put(tr, "localPosition", floats(x, y, z));
        // non-looping one-shot defaults
        set(e, "duration", 1);
        set(e, "looping", (byte) 0);
        return e;
    }

    static void set(Map<String, Object> e, String p, Object v) {
        put(cast(path(e, "data.config")), p, v);
    }

    static void texture(Map<String, Object> e, String tex, boolean additive, float[] hdr) {
        Map<String, Object> mat = cast(((Nbt.ListTag) path(e, "data.config.renderer.materials.payload")).items().get(0));
        Map<String, Object> d = cast(path(mat, "material.data"));
        put(d, "texture", NS + ":textures/effect/" + tex + ".png");
        put(d, "discardThreshold", 0.01f);
        if (hdr != null) {
            put(d, "hdr", map("color", floats(hdr[0], hdr[1], hdr[2], 1f)));
            put(d, "hdrMode", "MULTIPLICATIVE");
        } else {
            put(d, "hdr", map("color", floats(0, 0, 0, 1)));
            put(d, "hdrMode", "ADDITIVE");
        }
        put(mat, "blendMode.dstColorFactor", additive ? "ONE" : "ONE_MINUS_SRC_ALPHA");
    }

    static void burst(Map<String, Object> e, int count, int time) {
        Map<String, Object> b = cast(((Nbt.ListTag) path(e, "data.config.emission.bursts.payload")).items().get(0));
        put(b, "count", c(count));
        put(b, "time", time);
        put(b, "cycles", 1);
        set(e, "duration", time + 1);
    }

    static void sphere(Map<String, Object> e, float radius) {
        Map<String, Object> s = cast(path(e, "data.config.shape.shape"));
        put(s, "type", "sphere");
        Map<String, Object> d = cast(s.get("data"));
        put(d, "radius", radius);
        put(d, "radiusThickness", 1.0f);
        put(d, "arc", 360.0f);
    }

    static Object rotation(Object tpl, double a, double b) {
        Map<String, Object> r = cast(deepCopy(tpl));
        put(r, "_enable", (byte) 1);
        put(r, "roll", rc(a, b));
        return r;
    }

    static Object noise(float frequency, float strength) {
        return map("_enable", (byte) 1, "frequency", frequency, "quality", "Noise2D",
                "position", list3(c(strength), c(strength), c(strength)),
                "rotation", c(0), "size", c(0));
    }

    static void trails(Map<String, Object> e) {
        Map<String, Object> t = cast(path(e, "data.config.trails"));
        t.put("_enable", (byte) 1);
        t.put("ratio", 1.0f);
        t.put("lifetime", c(0.3));
        t.put("dieWithParticles", (byte) 0);
        t.put("sizeAffectsWidth", (byte) 1);
        t.put("sizeAffectsLifetime", (byte) 0);
        t.put("inheritParticleColor", (byte) 1);
        t.put("colorOverLifetime", gradient(new float[]{0, 1, 1, 1}, new float[]{0, 1, 1, 1, 1, 1, 1, 1}));
        t.put("trailType", "TRAIL");
        Map<String, Object> cfg = cast(t.get("config"));
        // index 0 = oldest tail point, 1 = head
        put(cfg, "widthOverTrail", curve(0, 0.9f, 0, 0.05f, 1, 1));
        put(cfg, "colorOverTrail", gradient(new float[]{0, 0, 0.6f, 0.6f, 1, 1},
                new float[]{0, 1, 0.3f, 0.06f, 1, 1, 0.85f, 0.5f}));
        put(cfg, "minVertexDistance", 0.04f);
        Map<String, Object> mat = cast(((Nbt.ListTag) path(cfg, "renderer.materials.payload")).items().get(0));
        Map<String, Object> d = cast(path(mat, "material.data"));
        put(d, "texture", NS + ":textures/effect/blast_trail.png");
        put(d, "discardThreshold", 0.01f);
        put(d, "hdr", map("color", floats(2.2f, 1.6f, 1.1f, 1f)));
        put(d, "hdrMode", "MULTIPLICATIVE");
        put(mat, "blendMode.dstColorFactor", "ONE");
        put(mat, "cull", (byte) 0);
    }

    // ------------------------------------------------------------------ number functions

    static Object c(double v) { return map("data", map("number", (float) v), "type", "constant"); }

    static Object rc(double a, double b) { return map("data", map("a", a, "b", b), "type", "random_constant"); }

    static Nbt.ListTag list3(Object x, Object y, Object z) { return new Nbt.ListTag((byte) 10, new ArrayList<>(List.of(x, y, z))); }

    /** Particle size: x/y follow the function, z stays 1 like the editor writes it. */
    static Nbt.ListTag size3(Object f) { return list3(f, deepCopy(f), c(1)); }

    /** Curve through normalized key points (x0,y0,x1,y1,...), mapped to [lower, upper], flat tangents. */
    static Object curve(float lower, float upper, double... kp) {
        List<Object> segs = new ArrayList<>();
        for (int i = 0; i + 3 < kp.length; i += 2) {
            float x0 = (float) kp[i], y0 = (float) kp[i + 1], x1 = (float) kp[i + 2], y1 = (float) kp[i + 3];
            float dx = (x1 - x0) / 3f;
            segs.add(floats(x0, y0, x0 + dx, y0, x1 - dx, y1, x1, y1));
        }
        return map("data", map("lockControlPoint", (byte) 1, "curves", new Nbt.ListTag((byte) 9, segs),
                "yAxis", "size", "min", lower, "xAxis", "lifetime", "max", upper, "lower", lower, "upper", upper), "type", "curve");
    }

    static Object gradient(float[] a, float[] rgb) {
        return map("data", map("gradientColor", map("a", floats(a), "rgb", floats(rgb))), "type", "gradient");
    }

    // ------------------------------------------------------------------ nbt utils

    static Nbt.ListTag floats(float... f) {
        List<Object> l = new ArrayList<>();
        for (float v : f) l.add(v);
        return new Nbt.ListTag((byte) 5, l);
    }

    static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            Object v = kv[i + 1];
            if (v instanceof Double d && !"a".equals(kv[i]) && !"b".equals(kv[i])) v = d.floatValue();
            m.put((String) kv[i], v);
        }
        return m;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> cast(Object o) { return (Map<String, Object>) o; }

    static Object path(Map<String, Object> m, String p) {
        Object cur = m;
        for (String k : p.split("\\.")) {
            cur = cast(cur).get(k);
            if (cur == null) throw new IllegalStateException("missing " + p + " at " + k);
        }
        return cur;
    }

    static void put(Map<String, Object> m, String p, Object v) {
        String[] ks = p.split("\\.");
        Map<String, Object> cur = m;
        for (int i = 0; i < ks.length - 1; i++) {
            Object n = cur.get(ks[i]);
            if (n == null) throw new IllegalStateException("missing " + p + " at " + ks[i]);
            cur = cast(n);
        }
        if (!cur.containsKey(ks[ks.length - 1]))
            throw new IllegalStateException("unknown key " + p);
        cur.put(ks[ks.length - 1], v);
    }

    static Object deepCopy(Object o) {
        if (o instanceof Map<?, ?> m) {
            Map<String, Object> r = new LinkedHashMap<>();
            for (var e : m.entrySet()) r.put((String) e.getKey(), deepCopy(e.getValue()));
            return r;
        }
        if (o instanceof Nbt.ListTag l) {
            List<Object> r = new ArrayList<>();
            for (Object i : l.items()) r.add(deepCopy(i));
            return new Nbt.ListTag(l.elemType(), r);
        }
        if (o instanceof Nbt.ByteArr a) return new Nbt.ByteArr(a.a().clone());
        if (o instanceof Nbt.IntArr a) return new Nbt.IntArr(a.a().clone());
        if (o instanceof Nbt.LongArr a) return new Nbt.LongArr(a.a().clone());
        return o;
    }

    // ------------------------------------------------------------------ textures

    static final class Textures {
        static void writeAll(Path dir) throws IOException {
            write(dir, "blast_fire", 128, Textures::fire);
            write(dir, "blast_smoke", 128, Textures::smoke);
            write(dir, "blast_flare", 128, Textures::flare);
            write(dir, "blast_ring", 128, Textures::ring);
            write(dir, "blast_ember", 32, Textures::ember);
            write(dir, "blast_trail", 32, Textures::trail);
        }

        interface Px { float[] at(float x, float y); } // x,y in [-1,1] -> {r,g,b,a}

        static void write(Path dir, String name, int size, Px px) throws IOException {
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            for (int j = 0; j < size; j++) for (int i = 0; i < size; i++) {
                float x = (i + 0.5f) / size * 2 - 1, y = (j + 0.5f) / size * 2 - 1;
                float[] c = px.at(x, y);
                int a = cl(c[3]), r = cl(c[0]), g = cl(c[1]), b = cl(c[2]);
                img.setRGB(i, j, (a << 24) | (r << 16) | (g << 8) | b);
            }
            ImageIO.write(img, "png", dir.resolve(name + ".png").toFile());
        }

        static int cl(float v) { return Math.max(0, Math.min(255, Math.round(v * 255))); }

        static float sat(float v) { return Math.max(0, Math.min(1, v)); }

        static float smooth(float e0, float e1, float x) { float t = sat((x - e0) / (e1 - e0)); return t * t * (3 - 2 * t); }

        // value noise + fbm
        static float hash(int x, int y, int s) {
            int h = x * 374761393 + y * 668265263 + s * 982451653;
            h = (h ^ (h >>> 13)) * 1274126177;
            return ((h ^ (h >>> 16)) & 0xffffff) / (float) 0xffffff;
        }

        static float vnoise(float x, float y, int s) {
            int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
            float fx = x - xi, fy = y - yi;
            fx = fx * fx * (3 - 2 * fx); fy = fy * fy * (3 - 2 * fy);
            float a = hash(xi, yi, s), b = hash(xi + 1, yi, s), c = hash(xi, yi + 1, s), d = hash(xi + 1, yi + 1, s);
            return a + (b - a) * fx + (c - a) * fy + (a - b - c + d) * fx * fy;
        }

        static float fbm(float x, float y, int s) {
            float v = 0, amp = 0.5f, f = 1;
            for (int o = 0; o < 5; o++) { v += amp * vnoise(x * f, y * f, s + o * 17); f *= 2; amp *= 0.5f; }
            return v / 0.96875f;
        }

        static float[] fire(float x, float y) {
            float r = (float) Math.sqrt(x * x + y * y);
            float n = fbm(x * 2.6f + 7, y * 2.6f + 3, 11);
            float n2 = fbm(x * 5.5f - 2, y * 5.5f + 9, 23);
            float edge = r + (n - 0.5f) * 0.55f;
            float mask = 1 - smooth(0.45f, 0.95f, edge);
            float a = mask * (0.55f + 0.45f * n2);
            float lum = 0.7f + 0.3f * n2 + 0.25f * (1 - smooth(0f, 0.6f, r));
            return new float[]{lum, lum, lum, a};
        }

        static float[] smoke(float x, float y) {
            float r = (float) Math.sqrt(x * x + y * y);
            float n = fbm(x * 2.2f + 1, y * 2.2f + 5, 41);
            float n2 = fbm(x * 4.5f + 3, y * 4.5f - 6, 57);
            float edge = r + (n - 0.5f) * 0.6f;
            float mask = 1 - smooth(0.35f, 0.98f, edge);
            float a = mask * (0.6f + 0.4f * n2);
            // fake top-left key light gives the puff some volume
            float shade = 0.62f + 0.3f * n2 + 0.18f * (-x - y) * 0.5f;
            return new float[]{shade, shade, shade, a};
        }

        static float[] flare(float x, float y) {
            float r2 = x * x + y * y;
            float v = (float) (Math.exp(-r2 / 0.004) + 0.55 * Math.exp(-r2 / 0.05) + 0.18 * Math.exp(-r2 / 0.3));
            float ax = Math.abs(x), ay = Math.abs(y);
            v += (float) (Math.exp(-ay / 0.012) * Math.exp(-ax * 3.2) * 0.95);         // long horizontal streak
            v += (float) (Math.exp(-ax / 0.014) * Math.exp(-ay * 4.5) * 0.75);         // vertical
            float u = (x + y) * 0.7071f, w = (x - y) * 0.7071f;
            v += (float) (Math.exp(-Math.abs(w) / 0.012) * Math.exp(-Math.abs(u) * 7) * 0.45); // diagonals
            v += (float) (Math.exp(-Math.abs(u) / 0.012) * Math.exp(-Math.abs(w) * 7) * 0.45);
            v *= 1 - smooth(0.85f, 1f, (float) Math.sqrt(r2));
            return new float[]{1, 1, 1, sat(v)};
        }

        static float[] ring(float x, float y) {
            float r = (float) Math.sqrt(x * x + y * y);
            double ang = Math.atan2(y, x);
            float R = 0.8f + 0.025f * (vnoise((float) (ang * 3 + 10), 0.5f, 77) - 0.5f) * 2;
            float v;
            if (r > R) v = (float) Math.exp(-Math.pow((r - R) / 0.04, 2));
            else v = (float) (Math.exp(-Math.pow((R - r) / 0.18, 2)) * 0.85 + 0.15 * Math.exp(-Math.pow((R - r) / 0.03, 2)));
            v += 0.08f * smooth(0, R, r) * (r < R ? 1 : 0);
            v *= 0.8f + 0.2f * fbm((float) (ang * 4), r * 3, 91);
            v *= 1 - smooth(0.94f, 1f, r);
            return new float[]{1, 1, 1, sat(v)};
        }

        static float[] ember(float x, float y) {
            float r2 = x * x + y * y;
            float v = (float) (Math.exp(-r2 / 0.03) + 0.4 * Math.exp(-r2 / 0.18));
            v *= 1 - smooth(0.8f, 1f, (float) Math.sqrt(r2));
            return new float[]{1, 1, 1, sat(v)};
        }

        static float[] trail(float x, float y) {
            float v = (float) (Math.exp(-y * y / 0.08) * 0.8 + Math.exp(-y * y / 0.015) * 0.5);
            return new float[]{1, 1, 1, sat(v)};
        }
    }
}
