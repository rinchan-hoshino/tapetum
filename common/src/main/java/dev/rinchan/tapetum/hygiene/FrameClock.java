package dev.rinchan.tapetum.hygiene;

/**
 * Monotonically increasing frame counter incremented once per render frame on the client.
 */
public final class FrameClock {
    private static long currentFrameId;

    private FrameClock() {}

    public static void advanceFrame() {
        currentFrameId++;
    }

    public static long currentFrameId() {
        return currentFrameId;
    }
}
