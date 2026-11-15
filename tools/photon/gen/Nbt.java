import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Minimal NBT reader/writer. Tree: Map(LinkedHashMap)=compound, List=list (with tag type in ListTag), primitives boxed. */
public class Nbt {
    public record Tag(byte type, Object v) {}
    public record ListTag(byte elemType, List<Object> items) {}
    public record ByteArr(byte[] a) {}
    public record IntArr(int[] a) {}
    public record LongArr(long[] a) {}

    public static Object[] read(Path p) throws IOException {
        byte[] raw = Files.readAllBytes(p);
        boolean gz = raw.length > 2 && (raw[0] & 0xff) == 0x1f && (raw[1] & 0xff) == 0x8b;
        InputStream in = new ByteArrayInputStream(raw);
        if (gz) in = new GZIPInputStream(in);
        try (DataInputStream d = new DataInputStream(new BufferedInputStream(in))) {
            byte t = d.readByte();
            if (t != 10) throw new IOException("Expected a compound root, got " + t);
            String name = d.readUTF();
            Object root = readPayload(d, t);
            if (d.read() != -1) throw new IOException("Trailing bytes after root compound");
            return new Object[]{name, root, gz};
        }
    }

    static Object readPayload(DataInputStream d, byte t) throws IOException {
        switch (t) {
            case 1: return d.readByte();
            case 2: return d.readShort();
            case 3: return d.readInt();
            case 4: return d.readLong();
            case 5: return d.readFloat();
            case 6: return d.readDouble();
            case 7: { byte[] a = new byte[d.readInt()]; d.readFully(a); return new ByteArr(a); }
            case 8: return d.readUTF();
            case 9: { byte et = d.readByte(); int n = d.readInt(); List<Object> l = new ArrayList<>();
                for (int i = 0; i < n; i++) l.add(readPayload(d, et)); return new ListTag(et, l); }
            case 10: { LinkedHashMap<String, Object> m = new LinkedHashMap<>();
                while (true) { byte ct = d.readByte(); if (ct == 0) break; String k = d.readUTF(); m.put(k, readPayload(d, ct)); } return m; }
            case 11: { int[] a = new int[d.readInt()]; for (int i = 0; i < a.length; i++) a[i] = d.readInt(); return new IntArr(a); }
            case 12: { long[] a = new long[d.readInt()]; for (int i = 0; i < a.length; i++) a[i] = d.readLong(); return new LongArr(a); }
        }
        throw new IOException("bad tag " + t);
    }

    public static byte typeOf(Object o) {
        if (o instanceof Byte) return 1; if (o instanceof Short) return 2; if (o instanceof Integer) return 3;
        if (o instanceof Long) return 4; if (o instanceof Float) return 5; if (o instanceof Double) return 6;
        if (o instanceof ByteArr) return 7; if (o instanceof String) return 8; if (o instanceof ListTag) return 9;
        if (o instanceof Map) return 10; if (o instanceof IntArr) return 11; if (o instanceof LongArr) return 12;
        throw new IllegalArgumentException(String.valueOf(o));
    }

    public static void write(Path p, String name, Object root, boolean gz) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        OutputStream os = gz ? new GZIPOutputStream(bo) : bo;
        DataOutputStream d = new DataOutputStream(os);
        d.writeByte(10); d.writeUTF(name); writePayload(d, root);
        d.flush(); if (gz) ((GZIPOutputStream) os).finish(); d.close();
        Files.write(p, bo.toByteArray());
    }

    @SuppressWarnings("unchecked")
    static void writePayload(DataOutputStream d, Object o) throws IOException {
        switch (typeOf(o)) {
            case 1 -> d.writeByte((Byte) o); case 2 -> d.writeShort((Short) o); case 3 -> d.writeInt((Integer) o);
            case 4 -> d.writeLong((Long) o); case 5 -> d.writeFloat((Float) o); case 6 -> d.writeDouble((Double) o);
            case 7 -> { byte[] a = ((ByteArr) o).a(); d.writeInt(a.length); d.write(a); }
            case 8 -> d.writeUTF((String) o);
            case 9 -> { ListTag l = (ListTag) o; byte et = l.elemType();
                d.writeByte(et); d.writeInt(l.items().size());
                for (Object i : l.items()) {
                    if (typeOf(i) != et) throw new IOException("Mixed or incorrect NBT list element type");
                    writePayload(d, i);
                } }
            case 10 -> { for (var e : ((Map<String, Object>) o).entrySet()) { d.writeByte(typeOf(e.getValue())); d.writeUTF(e.getKey()); writePayload(d, e.getValue()); } d.writeByte(0); }
            case 11 -> { int[] a = ((IntArr) o).a(); d.writeInt(a.length); for (int i : a) d.writeInt(i); }
            case 12 -> { long[] a = ((LongArr) o).a(); d.writeInt(a.length); for (long i : a) d.writeLong(i); }
        }
    }

    @SuppressWarnings("unchecked")
    public static void dump(StringBuilder sb, Object o, String ind) {
        if (o instanceof Map<?, ?> m) {
            if (m.isEmpty()) { sb.append("{}"); return; }
            sb.append("{\n");
            for (var e : ((Map<String, Object>) m).entrySet()) {
                sb.append(ind).append("  ").append(e.getKey()).append(": ");
                dump(sb, e.getValue(), ind + "  "); sb.append("\n");
            }
            sb.append(ind).append("}");
        } else if (o instanceof ListTag l) {
            boolean simple = l.items().stream().allMatch(x -> !(x instanceof Map) && !(x instanceof ListTag));
            if (simple) { sb.append("["); for (int i = 0; i < l.items().size(); i++) { if (i > 0) sb.append(", "); dump(sb, l.items().get(i), ind); } sb.append("]"); return; }
            sb.append("[\n");
            for (Object i : l.items()) { sb.append(ind).append("  "); dump(sb, i, ind + "  "); sb.append("\n"); }
            sb.append(ind).append("]");
        } else if (o instanceof String s) sb.append('"').append(s).append('"');
        else if (o instanceof Byte b) sb.append(b).append('b');
        else if (o instanceof Short s) sb.append(s).append('s');
        else if (o instanceof Long l) sb.append(l).append('L');
        else if (o instanceof Float f) sb.append(f).append('f');
        else if (o instanceof Double dd) sb.append(dd).append('d');
        else if (o instanceof ByteArr a) sb.append("[B;len=").append(a.a().length).append("]");
        else if (o instanceof IntArr a) sb.append(Arrays.toString(a.a()));
        else if (o instanceof LongArr a) sb.append("[L;").append(Arrays.toString(a.a())).append("]");
        else sb.append(o);
    }

    public static void main(String[] args) throws Exception {
        Object[] r = read(Path.of(args[0]));
        StringBuilder sb = new StringBuilder();
        sb.append("// name=").append(r[0]).append(" gz=").append(r[2]).append("\n");
        dump(sb, r[1], "");
        Files.writeString(Path.of(args[1]), sb.toString());
    }
}
