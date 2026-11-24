package dev.rinchan.tapetum.geometry;

import org.joml.Matrix4f;

/**
 * Prevents TLM's {@code SodiumGeoRenderer.renderCubesOfBone} from running view-space backface
 * culling against the local origin during one-time bone baking.
 */
public final class TlmGeoBakeGuard {
    private TlmGeoBakeGuard() {}

    public static void runUnculled(Matrix4f modelViewMatrix, Runnable bakeAction) {
        if (modelViewMatrix == null) {
            bakeAction.run();
            return;
        }
        float prevM32 = modelViewMatrix.m32();
        boolean overridden = prevM32 == 0.0f;
        if (overridden) {
            modelViewMatrix.m32(1.0f);
        }
        try {
            bakeAction.run();
        } finally {
            if (overridden) {
                modelViewMatrix.m32(prevM32);
            }
        }
    }
}
