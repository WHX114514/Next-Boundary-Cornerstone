package cn.rbq108.nextboundarycornerstone.body;

/** Scope only the model animation call used to draw a first-person hand. */
public final class HandRenderContext {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private HandRenderContext() {}

    public static boolean active() { return DEPTH.get() > 0; }

    public static void during(Runnable draw) {
        int previous = DEPTH.get();
        DEPTH.set(previous + 1);
        try {
            draw.run();
        } finally {
            if (previous == 0) DEPTH.remove();
            else DEPTH.set(previous);
        }
    }
}
