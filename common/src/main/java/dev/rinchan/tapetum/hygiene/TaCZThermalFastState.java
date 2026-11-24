package dev.rinchan.tapetum.hygiene;

/**
 * Per-frame cached boolean indicating whether TaCZ's thermal scope capture is active this frame.
 */
public final class TaCZThermalFastState {
    public static boolean activeThisFrame;

    private TaCZThermalFastState() {}

    public static void refresh(boolean enabled) {
        activeThisFrame = enabled;
    }
}
