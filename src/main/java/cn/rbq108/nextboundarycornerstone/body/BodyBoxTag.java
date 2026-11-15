package cn.rbq108.nextboundarycornerstone.body;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Stable anatomy order shared by AABB, OBB, swept motion and external part queries. */
public enum BodyBoxTag {
    HEAD(Part.HEAD, Segment.WHOLE, 0),
    TORSO_UPPER(Part.TORSO, Segment.UPPER, 1),
    TORSO_MIDDLE(Part.TORSO, Segment.MIDDLE, 1),
    TORSO_LOWER(Part.TORSO, Segment.LOWER, 1),
    LEFT_ARM_UPPER(Part.LEFT_ARM, Segment.UPPER, 2),
    LEFT_ARM_MIDDLE(Part.LEFT_ARM, Segment.MIDDLE, 2),
    LEFT_ARM_LOWER(Part.LEFT_ARM, Segment.LOWER, 2),
    RIGHT_ARM_UPPER(Part.RIGHT_ARM, Segment.UPPER, 3),
    RIGHT_ARM_MIDDLE(Part.RIGHT_ARM, Segment.MIDDLE, 3),
    RIGHT_ARM_LOWER(Part.RIGHT_ARM, Segment.LOWER, 3),
    LEFT_LEG_UPPER(Part.LEFT_LEG, Segment.UPPER, 4),
    LEFT_LEG_MIDDLE(Part.LEFT_LEG, Segment.MIDDLE, 4),
    LEFT_LEG_LOWER(Part.LEFT_LEG, Segment.LOWER, 4),
    RIGHT_LEG_UPPER(Part.RIGHT_LEG, Segment.UPPER, 5),
    RIGHT_LEG_MIDDLE(Part.RIGHT_LEG, Segment.MIDDLE, 5),
    RIGHT_LEG_LOWER(Part.RIGHT_LEG, Segment.LOWER, 5);

    public enum Part { HEAD, TORSO, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG }
    public enum Segment { WHOLE, UPPER, MIDDLE, LOWER }

    private static final BodyBoxTag[] ORDER = values();
    public static final List<BodyBoxTag> ALL = List.of(ORDER);
    public static final int ALL_MASK = (1 << ORDER.length) - 1;
    public static final int ARMS_MASK = mask(Part.LEFT_ARM) | mask(Part.RIGHT_ARM);
    public static final int MOVEMENT_MASK = ALL_MASK & ~ARMS_MASK;
    public static final List<BodyBoxTag> MOVEMENT = matching(MOVEMENT_MASK);

    private final Part part;
    private final Segment segment;
    private final int jointIndex;
    private final String id;

    BodyBoxTag(Part part, Segment segment, int jointIndex) {
        this.part = part;
        this.segment = segment;
        this.jointIndex = jointIndex;
        this.id = name().toLowerCase(Locale.ROOT);
    }

    public Part part() { return part; }
    public Segment segment() { return segment; }
    public int jointIndex() { return jointIndex; }
    public String id() { return id; }
    public int mask() { return 1 << ordinal(); }
    public boolean matches(int mask) { return (mask & mask()) != 0; }
    public boolean participatesInMovement() { return matches(MOVEMENT_MASK); }

    public static BodyBoxTag at(int geometryIndex) { return ORDER[geometryIndex]; }
    public static int mask(Part part) {
        int result = 0;
        for (BodyBoxTag tag : ORDER) if (tag.part == part) result |= tag.mask();
        return result;
    }
    public static List<BodyBoxTag> matching(int mask) {
        return Arrays.stream(ORDER).filter(tag -> tag.matches(mask)).toList();
    }
    public static boolean movementJoint(int joint) { return joint != 2 && joint != 3; }
}
